package com.reconpilot.api.analytics;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class AnalyticsServiceValidationTest {
    private final AnalyticsService service = new AnalyticsService(null, null);
    private final AnalyticsDtos.Request request = new AnalyticsDtos.Request("run-1", List.of(
            new AnalyticsDtos.Candidate("expected", 1000, 0, 0, false, true)), false);

    @Test
    void acceptsCompleteAdvisoryOnlyResponse() {
        AnalyticsDtos.Response response = response("expected", BigDecimal.valueOf(0.38), false, "none_advisory_only");
        assertDoesNotThrow(() -> service.validate(response, request));
    }

    @Test
    void rejectsUnknownTransactionId() {
        assertThrows(IllegalStateException.class,
                () -> service.validate(response("intruder", BigDecimal.ZERO, false, "none_advisory_only"), request));
    }

    @Test
    void rejectsProbabilityClaimOrDecisionAuthority() {
        assertThrows(IllegalStateException.class,
                () -> service.validate(response("expected", BigDecimal.ONE, true, "match_decision"), request));
    }

    @Test
    void rejectsOutOfBoundsScore() {
        assertThrows(IllegalStateException.class,
                () -> service.validate(response("expected", BigDecimal.valueOf(1.01), false, "none_advisory_only"), request));
    }

    private AnalyticsDtos.Response response(String id, BigDecimal score, boolean probability, String authority) {
        return new AnalyticsDtos.Response("run-1", "analytics-0.2.0", "heuristic_v1", authority, probability,
                List.of(new AnalyticsDtos.Score(id, score, "medium", List.of("reference_conflict"), true)));
    }
}
