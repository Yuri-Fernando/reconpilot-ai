package com.reconpilot.api.reconciliation;

import java.math.BigDecimal;

public record EngineResult(
        EngineTransaction bank,
        EngineTransaction ledger,
        ReconciliationStatus status,
        BigDecimal confidence,
        String reasonCode,
        String explanation,
        ReviewStatus reviewStatus) {
}
