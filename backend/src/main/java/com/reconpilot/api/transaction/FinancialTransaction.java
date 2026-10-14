package com.reconpilot.api.transaction;

import com.reconpilot.api.importation.ImportBatch;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Locale;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "financial_transactions", schema = "app")
public class FinancialTransaction {
    @Id
    private UUID id;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionSource source;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "import_batch_id", nullable = false)
    private ImportBatch importBatch;
    @Column(name = "external_id", nullable = false, length = 100)
    private String externalId;
    @Column(length = 100)
    private String reference;
    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate;
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;
    @Column(nullable = false, length = 3, columnDefinition = "CHAR(3)")
    @JdbcTypeCode(SqlTypes.CHAR)
    private String currency;
    @Column(nullable = false, length = 500)
    private String description;
    @Column(name = "normalized_description", nullable = false, length = 500)
    private String normalizedDescription;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected FinancialTransaction() {}

    public FinancialTransaction(ImportBatch importBatch, String externalId, String reference,
            LocalDate transactionDate, BigDecimal amount, String currency, String description) {
        this.id = UUID.randomUUID();
        this.source = importBatch.getSource();
        this.importBatch = importBatch;
        this.externalId = externalId;
        this.reference = normalizeReference(reference);
        this.transactionDate = transactionDate;
        this.amount = amount.setScale(2);
        this.currency = currency.toUpperCase(Locale.ROOT);
        this.description = description;
        this.normalizedDescription = normalizeDescription(description);
        this.createdAt = Instant.now();
    }

    public static String normalizeReference(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private static String normalizeDescription(String value) {
        String ascii = Normalizer.normalize(value.trim(), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return ascii.toUpperCase(Locale.ROOT).replaceAll("\\s+", " ");
    }

    public UUID getId() { return id; }
    public TransactionSource getSource() { return source; }
    public ImportBatch getImportBatch() { return importBatch; }
    public String getExternalId() { return externalId; }
    public String getReference() { return reference; }
    public LocalDate getTransactionDate() { return transactionDate; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public String getDescription() { return description; }
    public Instant getCreatedAt() { return createdAt; }
}
