package com.reconpilot.api.dashboard;

import com.reconpilot.api.reconciliation.RunStatus;
import java.time.Instant;
import java.util.UUID;

public final class DashboardDtos {
    private DashboardDtos() {}

    public record LastRun(UUID runId, RunStatus status, Instant createdAt, String algorithmVersion) {}

    public record Summary(
            LastRun lastRun,
            long bankCount,
            long ledgerCount,
            long matched,
            long possible,
            long exceptionsPending,
            long reviewed,
            String reconciliationRate,
            String volume) {
        public static Summary empty() {
            return new Summary(null, 0, 0, 0, 0, 0, 0, "0.0", "0.00");
        }
    }
}
