package com.reconpilot.api.analytics;

import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class HttpAnalyticsClient implements AnalyticsClient {
    private final boolean enabled;
    private final String token;
    private final RestClient restClient;

    public HttpAnalyticsClient(
            @Value("${app.analytics.enabled:false}") boolean enabled,
            @Value("${app.analytics.base-url:http://localhost:8000}") String baseUrl,
            @Value("${app.analytics.token:}") String token,
            @Value("${app.analytics.timeout:2s}") Duration timeout) {
        this.enabled = enabled;
        this.token = token;
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(timeout)
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(timeout);
        this.restClient = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
    }

    @Override
    public boolean enabled() { return enabled; }

    @Override
    public AnalyticsDtos.Response analyze(AnalyticsDtos.Request request) {
        if (!enabled) throw new IllegalStateException("Analytics is disabled");
        if (token.isBlank()) throw new IllegalStateException("AI_INTERNAL_TOKEN is not configured");
        return restClient.post().uri("/v1/anomalies/analyze")
                .header("X-Internal-Token", token)
                .body(request)
                .retrieve()
                .body(AnalyticsDtos.Response.class);
    }
}
