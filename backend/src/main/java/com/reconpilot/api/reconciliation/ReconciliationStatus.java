package com.reconpilot.api.reconciliation;

public enum ReconciliationStatus {
    MATCHED,
    POSSIBLE_MATCH,
    AMOUNT_MISMATCH,
    DATE_MISMATCH,
    CURRENCY_MISMATCH,
    AMBIGUOUS,
    DUPLICATE,
    UNMATCHED_BANK,
    UNMATCHED_LEDGER
}
