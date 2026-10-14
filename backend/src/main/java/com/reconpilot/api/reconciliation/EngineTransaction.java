package com.reconpilot.api.reconciliation;

import com.reconpilot.api.transaction.FinancialTransaction;
import com.reconpilot.api.transaction.TransactionSource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Locale;
import java.util.UUID;

public record EngineTransaction(
        UUID id,
        TransactionSource source,
        String externalId,
        String reference,
        LocalDate transactionDate,
        BigDecimal amount,
        String currency,
        String description) {

    public EngineTransaction {
        amount = amount.setScale(2);
        reference = reference == null || reference.isBlank() ? null : reference.trim().toUpperCase(Locale.ROOT);
        currency = currency.toUpperCase(Locale.ROOT);
    }

    public static EngineTransaction from(FinancialTransaction value) {
        return new EngineTransaction(value.getId(), value.getSource(), value.getExternalId(), value.getReference(),
                value.getTransactionDate(), value.getAmount(), value.getCurrency(), value.getDescription());
    }
}
