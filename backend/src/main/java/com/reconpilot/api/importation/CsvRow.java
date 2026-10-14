package com.reconpilot.api.importation;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CsvRow(
        String externalId,
        String reference,
        LocalDate transactionDate,
        BigDecimal amount,
        String currency,
        String description) {
}
