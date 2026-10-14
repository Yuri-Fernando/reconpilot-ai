package com.reconpilot.api.reconciliation;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReconciliationItemRepository extends JpaRepository<ReconciliationItem, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from ReconciliationItem i where i.id = :id")
    Optional<ReconciliationItem> findByIdForUpdate(@Param("id") UUID id);

    Page<ReconciliationItem> findByRunId(UUID runId, Pageable pageable);
    Page<ReconciliationItem> findByRunIdAndReconciliationStatus(UUID runId, ReconciliationStatus status, Pageable pageable);
    List<ReconciliationItem> findAllByRunIdOrderByCreatedAtAsc(UUID runId);
}
