package com.reconpilot.api.analytics;

public interface AnalyticsClient {
    boolean enabled();
    AnalyticsDtos.Response analyze(AnalyticsDtos.Request request);
}
