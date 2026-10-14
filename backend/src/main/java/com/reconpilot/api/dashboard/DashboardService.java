package com.reconpilot.api.dashboard;

import com.reconpilot.api.reconciliation.ReconciliationDtos;
import com.reconpilot.api.reconciliation.ReconciliationRun;
import com.reconpilot.api.reconciliation.ReconciliationRunRepository;
import com.reconpilot.api.reconciliation.ReconciliationService;
import com.reconpilot.api.reconciliation.RunStatus;
import com.reconpilot.api.transaction.FinancialTransaction;
import com.reconpilot.api.transaction.TransactionRepository;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {
    private final ReconciliationRunRepository runRepository;
    private final ReconciliationService reconciliationService;
    private final TransactionRepository transactionRepository;

    public DashboardService(ReconciliationRunRepository runRepository, ReconciliationService reconciliationService,
            TransactionRepository transactionRepository) {
        this.runRepository = runRepository;
        this.reconciliationService = reconciliationService;
        this.transactionRepository = transactionRepository;
    }

    @Transactional(readOnly = true)
    public DashboardDtos.Summary summary() {
        ReconciliationRun run = runRepository.findFirstByStatusOrderByStartedAtDesc(RunStatus.COMPLETED).orElse(null);
        if (run == null) return DashboardDtos.Summary.empty();
        ReconciliationDtos.Summary summary = reconciliationService.summary(run.getId());
        BigDecimal volume = transactionRepository
                .findByImportBatchIdOrderByTransactionDateAscExternalIdAsc(run.getBankBatch().getId()).stream()
                .map(FinancialTransaction::getAmount).map(BigDecimal::abs)
                .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2);
        DashboardDtos.LastRun lastRun = new DashboardDtos.LastRun(
                run.getId(), run.getStatus(), run.getStartedAt(), run.getAlgorithmVersion());
        long pending = summary.exceptions() - summary.reviewed();
        return new DashboardDtos.Summary(lastRun, summary.bankCount(), summary.ledgerCount(), summary.matched(),
                summary.possible(), Math.max(0, pending), summary.reviewed(), summary.reconciliationRate(), volume.toPlainString());
    }
}
