package com.reconpilot.api.reconciliation;

import com.reconpilot.api.transaction.FinancialTransaction;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class ReconciliationDtos {
    private ReconciliationDtos() {}

    public record CreateRequest(@NotNull UUID bankBatchId, @NotNull UUID ledgerBatchId) {}

    public record TransactionView(UUID id, String externalId, String reference, LocalDate transactionDate,
            String amount, String currency, String description) {
        static TransactionView from(FinancialTransaction value) {
            if (value == null) return null;
            return new TransactionView(value.getId(), value.getExternalId(), value.getReference(),
                    value.getTransactionDate(), value.getAmount().toPlainString(), value.getCurrency(), value.getDescription());
        }
    }

    public record ItemView(UUID id, TransactionView bank, TransactionView ledger, ReconciliationStatus status,
            String reasonCode, String explanation, ReviewStatus reviewStatus, BigDecimal confidence, Instant createdAt) {
        static ItemView from(ReconciliationItem item) {
            return new ItemView(item.getId(), TransactionView.from(item.getBankTransaction()),
                    TransactionView.from(item.getLedgerTransaction()), item.getReconciliationStatus(),
                    item.getReasonCode(), item.getExplanation(), item.getReviewStatus(), item.getConfidence(), item.getCreatedAt());
        }
    }

    public record StatusCount(ReconciliationStatus status, long count) {}

    public record Summary(long bankCount, long ledgerCount, long matched, long possible, long exceptions,
            long reviewed, List<StatusCount> byStatus, String unmatchedTotalAmount, String reconciliationRate) {}

    public record CreateResponse(UUID runId, RunStatus status, String algorithmVersion, boolean created,
            String analyticsStatus, Summary summary) {}

    public record RunListItem(UUID runId, Instant createdAt, Instant finishedAt, UUID bankBatchId,
            UUID ledgerBatchId, RunStatus status, String algorithmVersion) {
        static RunListItem from(ReconciliationRun run) {
            return new RunListItem(run.getId(), run.getStartedAt(), run.getFinishedAt(),
                    run.getBankBatch().getId(), run.getLedgerBatch().getId(), run.getStatus(), run.getAlgorithmVersion());
        }
    }

    public record PageResponse<T>(List<T> content, long totalElements, int totalPages, int page, int size) {}
}
