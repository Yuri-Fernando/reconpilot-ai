package com.reconpilot.api.reconciliation;

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
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "reconciliation_runs", schema = "app")
public class ReconciliationRun {
    @Id
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bank_batch_id", nullable = false)
    private ImportBatch bankBatch;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ledger_batch_id", nullable = false)
    private ImportBatch ledgerBatch;
    @Column(name = "algorithm_version", nullable = false, length = 20)
    private String algorithmVersion;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RunStatus status;
    @Column(name = "started_at", nullable = false)
    private Instant startedAt;
    @Column(name = "finished_at")
    private Instant finishedAt;

    protected ReconciliationRun() {}

    public ReconciliationRun(ImportBatch bankBatch, ImportBatch ledgerBatch, String algorithmVersion) {
        this.id = UUID.randomUUID();
        this.bankBatch = bankBatch;
        this.ledgerBatch = ledgerBatch;
        this.algorithmVersion = algorithmVersion;
        this.status = RunStatus.RUNNING;
        this.startedAt = Instant.now();
    }

    public void complete() {
        this.status = RunStatus.COMPLETED;
        this.finishedAt = Instant.now();
    }

    public void fail() {
        this.status = RunStatus.FAILED;
        this.finishedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public ImportBatch getBankBatch() { return bankBatch; }
    public ImportBatch getLedgerBatch() { return ledgerBatch; }
    public String getAlgorithmVersion() { return algorithmVersion; }
    public RunStatus getStatus() { return status; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getFinishedAt() { return finishedAt; }
}
