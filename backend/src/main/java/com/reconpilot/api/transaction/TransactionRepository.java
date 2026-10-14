package com.reconpilot.api.transaction;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<FinancialTransaction, UUID> {
    List<FinancialTransaction> findByImportBatchIdOrderByTransactionDateAscExternalIdAsc(UUID importBatchId);
    long countByImportBatchId(UUID importBatchId);
}
