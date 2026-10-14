package com.reconpilot.api.reconciliation;

import com.reconpilot.api.transaction.FinancialTransaction;
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
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "reconciliation_items", schema = "app")
public class ReconciliationItem {
    @Id
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "run_id", nullable = false)
    private ReconciliationRun run;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bank_transaction_id")
    private FinancialTransaction bankTransaction;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ledger_transaction_id")
    private FinancialTransaction ledgerTransaction;
    @Enumerated(EnumType.STRING)
    @Column(name = "reconciliation_status", nullable = false, length = 30)
    private ReconciliationStatus reconciliationStatus;
    @Column(precision = 4, scale = 3)
    private BigDecimal confidence;
    @Column(name = "reason_code", nullable = false, length = 50)
    private String reasonCode;
    @Column(nullable = false, columnDefinition = "text")
    private String explanation;
    @Enumerated(EnumType.STRING)
    @Column(name = "review_status", nullable = false, length = 20)
    private ReviewStatus reviewStatus;
    @Column(name = "reviewed_at")
    private Instant reviewedAt;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ReconciliationItem() {}

    public ReconciliationItem(ReconciliationRun run, FinancialTransaction bankTransaction,
            FinancialTransaction ledgerTransaction, EngineResult result) {
        this.id = UUID.randomUUID();
        this.run = run;
        this.bankTransaction = bankTransaction;
        this.ledgerTransaction = ledgerTransaction;
        this.reconciliationStatus = result.status();
        this.confidence = result.confidence();
        this.reasonCode = result.reasonCode();
        this.explanation = result.explanation();
        this.reviewStatus = result.reviewStatus();
        this.createdAt = Instant.now();
    }

    public ReviewStatus applyReview(ReviewStatus target) {
        ReviewStatus previous = this.reviewStatus;
        this.reviewStatus = target;
        this.reviewedAt = Instant.now();
        return previous;
    }

    public UUID getId() { return id; }
    public ReconciliationRun getRun() { return run; }
    public FinancialTransaction getBankTransaction() { return bankTransaction; }
    public FinancialTransaction getLedgerTransaction() { return ledgerTransaction; }
    public ReconciliationStatus getReconciliationStatus() { return reconciliationStatus; }
    public BigDecimal getConfidence() { return confidence; }
    public String getReasonCode() { return reasonCode; }
    public String getExplanation() { return explanation; }
    public ReviewStatus getReviewStatus() { return reviewStatus; }
    public Instant getReviewedAt() { return reviewedAt; }
    public Instant getCreatedAt() { return createdAt; }
}
