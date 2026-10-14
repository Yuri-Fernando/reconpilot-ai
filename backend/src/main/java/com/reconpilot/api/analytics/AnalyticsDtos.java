package com.reconpilot.api.analytics;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

public final class AnalyticsDtos {
    private AnalyticsDtos() {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record Candidate(String transactionId, long amountCents, long deltaCents, int dateGapDays,
            boolean possibleDuplicate, boolean referenceConflict) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record Request(String batchId, List<Candidate> candidates, boolean enableIforest) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record Score(String transactionId, BigDecimal riskScore, String severity, List<String> reasons,
            boolean requiresHumanReview) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record Response(String batchId, String modelVersion, String method, String decisionAuthority,
            boolean scoresAreProbabilities, List<Score> items) {}

    public record AssessmentView(UUID id, UUID itemId, String method, String modelVersion,
            BigDecimal score, String severity, String reasonsJson, Instant createdAt) {
        static AssessmentView from(AnalyticsAssessment value) {
            return new AssessmentView(value.getId(), value.getItem().getId(), value.getMethod(), value.getModelVersion(),
                    value.getScore(), value.getSeverity(), value.getReasonsJson(), value.getCreatedAt());
        }
    }

    public record RunView(String status, boolean advisoryOnly, List<AssessmentView> items) {}
}
