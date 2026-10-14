package com.reconpilot.api.analytics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import java.util.List;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

class HttpAnalyticsClientContractTest {
    // Folga larga: a primeira chamada da JVM (aquecimento do HttpClient) pode passar de 2 s sob carga.
    private static final Duration GENEROUS_TIMEOUT = Duration.ofSeconds(10);

    private MockWebServer server;

    @BeforeEach
    void start() throws Exception {
        server = new MockWebServer();
        server.start();
    }

    @AfterEach
    void stop() throws Exception {
        server.shutdown();
    }

    @Test
    void sendsInternalTokenAndDeserializesAdvisoryContract() throws Exception {
        server.enqueue(json(200, """
                {"batch_id":"run-1","model_version":"analytics-0.2.0","method":"heuristic_v1",
                 "decision_authority":"none_advisory_only","scores_are_probabilities":false,
                 "items":[{"transaction_id":"item-1","risk_score":0.35,"severity":"medium",
                 "reasons":["possible_duplicate"],"requires_human_review":true}]}
                """));
        AnalyticsDtos.Response response = client(GENEROUS_TIMEOUT).analyze(request());
        RecordedRequest recorded = server.takeRequest();
        assertEquals("secret", recorded.getHeader("X-Internal-Token"));
        assertEquals("/v1/anomalies/analyze", recorded.getPath());
        String json = recorded.getBody().readUtf8();
        org.junit.jupiter.api.Assertions.assertTrue(json.contains("\"batch_id\":\"run-1\""));
        org.junit.jupiter.api.Assertions.assertTrue(json.contains("\"transaction_id\":\"item-1\""));
        org.junit.jupiter.api.Assertions.assertTrue(json.contains("\"enable_iforest\":false"));
        assertEquals("analytics-0.2.0", response.modelVersion());
        assertEquals("item-1", response.items().getFirst().transactionId());
    }

    @Test
    void propagates401AsAvailabilityFailureForCallerToDegrade() {
        server.enqueue(json(401, "{\"detail\":\"Unauthorized\"}"));
        assertThrows(HttpClientErrorException.Unauthorized.class, () -> client(GENEROUS_TIMEOUT).analyze(request()));
    }

    @Test
    void propagates422AsInvalidContractFailure() {
        server.enqueue(json(422, "{\"detail\":\"Invalid\"}"));
        assertThrows(HttpClientErrorException.UnprocessableContent.class, () -> client(GENEROUS_TIMEOUT).analyze(request()));
    }

    @Test
    void propagates503AsAvailabilityFailure() {
        server.enqueue(json(503, "{\"detail\":\"Unavailable\"}"));
        assertThrows(HttpServerErrorException.ServiceUnavailable.class, () -> client(GENEROUS_TIMEOUT).analyze(request()));
    }

    @Test
    void enforcesReadTimeout() {
        server.enqueue(new MockResponse().setHeadersDelay(500, java.util.concurrent.TimeUnit.MILLISECONDS)
                .setResponseCode(200).setBody("{}"));
        assertThrows(ResourceAccessException.class, () -> client(Duration.ofMillis(100)).analyze(request()));
    }

    private HttpAnalyticsClient client(Duration timeout) {
        return new HttpAnalyticsClient(true, server.url("/").toString(), "secret", timeout);
    }

    private AnalyticsDtos.Request request() {
        return new AnalyticsDtos.Request("run-1", List.of(
                new AnalyticsDtos.Candidate("item-1", 1000, 0, 0, true, false)), false);
    }

    private MockResponse json(int status, String body) {
        return new MockResponse().setResponseCode(status).addHeader("Content-Type", "application/json").setBody(body);
    }
}
