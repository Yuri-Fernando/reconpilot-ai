package com.reconpilot.api.reconciliation;

import com.reconpilot.api.analytics.AnalyticsService;
import com.reconpilot.api.common.ApiException;
import com.reconpilot.api.importation.ImportBatch;
import com.reconpilot.api.importation.ImportBatchRepository;
import com.reconpilot.api.transaction.FinancialTransaction;
import com.reconpilot.api.transaction.TransactionRepository;
import com.reconpilot.api.transaction.TransactionSource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReconciliationService {
    private final ImportBatchRepository batchRepository;
    private final TransactionRepository transactionRepository;
    private final ReconciliationRunRepository runRepository;
    private final ReconciliationItemRepository itemRepository;
    private final ReconciliationEngine engine;
    private final AnalyticsService analyticsService;

    public ReconciliationService(ImportBatchRepository batchRepository, TransactionRepository transactionRepository,
            ReconciliationRunRepository runRepository, ReconciliationItemRepository itemRepository,
            ReconciliationEngine engine, AnalyticsService analyticsService) {
        this.batchRepository = batchRepository;
        this.transactionRepository = transactionRepository;
        this.runRepository = runRepository;
        this.itemRepository = itemRepository;
        this.engine = engine;
        this.analyticsService = analyticsService;
    }

    @Transactional
    public ReconciliationDtos.CreateResponse create(ReconciliationDtos.CreateRequest request) {
        var existing = runRepository.findByBankBatchIdAndLedgerBatchIdAndAlgorithmVersion(
                request.bankBatchId(), request.ledgerBatchId(), ReconciliationEngine.ALGORITHM_VERSION);
        if (existing.isPresent()) return createResponse(existing.get(), false);

        ImportBatch bankBatch = requireBatch(request.bankBatchId(), TransactionSource.BANK);
        ImportBatch ledgerBatch = requireBatch(request.ledgerBatchId(), TransactionSource.LEDGER);
        List<FinancialTransaction> bankTransactions = transactionRepository
                .findByImportBatchIdOrderByTransactionDateAscExternalIdAsc(bankBatch.getId());
        List<FinancialTransaction> ledgerTransactions = transactionRepository
                .findByImportBatchIdOrderByTransactionDateAscExternalIdAsc(ledgerBatch.getId());
        if (bankTransactions.isEmpty() || ledgerTransactions.isEmpty()) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "EMPTY_IMPORT_BATCH", "Os dois lotes precisam conter transacoes");
        }

        ReconciliationRun run = runRepository.save(new ReconciliationRun(bankBatch, ledgerBatch, ReconciliationEngine.ALGORITHM_VERSION));
        List<FinancialTransaction> allTransactions = java.util.stream.Stream
                .concat(bankTransactions.stream(), ledgerTransactions.stream()).toList();
        Map<UUID, FinancialTransaction> byId = allTransactions.stream()
                .collect(Collectors.toMap(FinancialTransaction::getId, Function.identity()));
        List<EngineResult> results = engine.reconcile(allTransactions.stream().map(EngineTransaction::from).toList());
        List<ReconciliationItem> items = results.stream()
                .map(result -> new ReconciliationItem(run,
                        result.bank() == null ? null : byId.get(result.bank().id()),
                        result.ledger() == null ? null : byId.get(result.ledger().id()), result))
                .toList();
        itemRepository.saveAll(items);
        run.complete();
        String analyticsStatus = analyticsService.analyze(run, items);
        return createResponse(run, true, analyticsStatus);
    }

    @Transactional(readOnly = true)
    public List<ReconciliationDtos.RunListItem> listRuns() {
        return runRepository.findAllByOrderByStartedAtDesc().stream().map(ReconciliationDtos.RunListItem::from).toList();
    }

    @Transactional(readOnly = true)
    public ReconciliationDtos.PageResponse<ReconciliationDtos.ItemView> items(
            UUID runId, ReconciliationStatus status, int page, int size) {
        requireRun(runId);
        if (page < 0 || size < 1 || size > 100) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_PAGE", "page deve ser >= 0 e size entre 1 e 100");
        }
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").ascending());
        Page<ReconciliationItem> result = status == null
                ? itemRepository.findByRunId(runId, pageable)
                : itemRepository.findByRunIdAndReconciliationStatus(runId, status, pageable);
        return new ReconciliationDtos.PageResponse<>(result.getContent().stream().map(ReconciliationDtos.ItemView::from).toList(),
                result.getTotalElements(), result.getTotalPages(), result.getNumber(), result.getSize());
    }

    @Transactional(readOnly = true)
    public ReconciliationDtos.Summary summary(UUID runId) {
        ReconciliationRun run = requireRun(runId);
        List<ReconciliationItem> items = itemRepository.findAllByRunIdOrderByCreatedAtAsc(runId);
        long bankCount = transactionRepository.countByImportBatchId(run.getBankBatch().getId());
        long ledgerCount = transactionRepository.countByImportBatchId(run.getLedgerBatch().getId());
        return summarize(items, bankCount, ledgerCount);
    }

    ReconciliationDtos.Summary summarize(List<ReconciliationItem> items, long bankCount, long ledgerCount) {
        Map<ReconciliationStatus, Long> counts = items.stream().collect(Collectors.groupingBy(
                ReconciliationItem::getReconciliationStatus, () -> new EnumMap<>(ReconciliationStatus.class), Collectors.counting()));
        List<ReconciliationDtos.StatusCount> byStatus = java.util.Arrays.stream(ReconciliationStatus.values())
                .map(status -> new ReconciliationDtos.StatusCount(status, counts.getOrDefault(status, 0L))).toList();
        long matched = counts.getOrDefault(ReconciliationStatus.MATCHED, 0L);
        long possible = counts.getOrDefault(ReconciliationStatus.POSSIBLE_MATCH, 0L);
        long reviewed = items.stream().filter(item -> item.getReviewStatus() == ReviewStatus.APPROVED
                || item.getReviewStatus() == ReviewStatus.REJECTED).count();
        BigDecimal unmatched = items.stream()
                .filter(item -> item.getReconciliationStatus() == ReconciliationStatus.UNMATCHED_BANK
                        || item.getReconciliationStatus() == ReconciliationStatus.UNMATCHED_LEDGER)
                .map(item -> item.getBankTransaction() != null ? item.getBankTransaction().getAmount() : item.getLedgerTransaction().getAmount())
                .map(BigDecimal::abs).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2);
        long denominator = Math.min(bankCount, ledgerCount);
        BigDecimal rate = denominator == 0 ? BigDecimal.ZERO : BigDecimal.valueOf(matched)
                .multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(denominator), 1, RoundingMode.HALF_UP);
        return new ReconciliationDtos.Summary(bankCount, ledgerCount, matched, possible, items.size() - matched,
                reviewed, byStatus, unmatched.toPlainString(), rate.toPlainString());
    }

    private ReconciliationDtos.CreateResponse createResponse(ReconciliationRun run, boolean created) {
        return createResponse(run, created, analyticsService.status(run.getId()));
    }

    private ReconciliationDtos.CreateResponse createResponse(ReconciliationRun run, boolean created, String analyticsStatus) {
        return new ReconciliationDtos.CreateResponse(run.getId(), run.getStatus(), run.getAlgorithmVersion(),
                created, analyticsStatus, summary(run.getId()));
    }

    private ImportBatch requireBatch(UUID id, TransactionSource expectedSource) {
        ImportBatch batch = batchRepository.findById(id).orElseThrow(() ->
                new ApiException(HttpStatus.NOT_FOUND, "IMPORT_BATCH_NOT_FOUND", "Lote nao encontrado: " + id));
        if (batch.getSource() != expectedSource) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "IMPORT_SOURCE_MISMATCH",
                    "O lote " + id + " nao pertence a origem " + expectedSource);
        }
        return batch;
    }

    private ReconciliationRun requireRun(UUID id) {
        return runRepository.findById(id).orElseThrow(() ->
                new ApiException(HttpStatus.NOT_FOUND, "RECONCILIATION_NOT_FOUND", "Conciliacao nao encontrada: " + id));
    }
}
