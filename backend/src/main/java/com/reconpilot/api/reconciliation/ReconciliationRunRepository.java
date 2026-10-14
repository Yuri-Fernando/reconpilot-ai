package com.reconpilot.api.reconciliation;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReconciliationRunRepository extends JpaRepository<ReconciliationRun, UUID> {
    Optional<ReconciliationRun> findByBankBatchIdAndLedgerBatchIdAndAlgorithmVersion(
            UUID bankBatchId, UUID ledgerBatchId, String algorithmVersion);
    List<ReconciliationRun> findAllByOrderByStartedAtDesc();
    Optional<ReconciliationRun> findFirstByStatusOrderByStartedAtDesc(RunStatus status);
}
