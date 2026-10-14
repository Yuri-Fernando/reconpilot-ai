package com.reconpilot.api.importation;

import com.reconpilot.api.transaction.TransactionSource;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImportBatchRepository extends JpaRepository<ImportBatch, UUID> {
    boolean existsBySourceAndFileSha256(TransactionSource source, String fileSha256);
    List<ImportBatch> findAllByOrderByCreatedAtDesc();
}
