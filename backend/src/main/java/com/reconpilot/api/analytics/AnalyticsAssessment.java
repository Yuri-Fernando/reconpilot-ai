package com.reconpilot.api.analytics;

import com.reconpilot.api.reconciliation.ReconciliationItem;
import com.reconpilot.api.reconciliation.ReconciliationRun;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "analytics_assessments", schema = "app")
public class AnalyticsAssessment {
    @Id
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "run_id", nullable = false)
    private ReconciliationRun run;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private ReconciliationItem item;
    @Column(nullable = false, length = 100)
    private String method;
    @Column(name = "model_version", nullable = false, length = 100)
    private String modelVersion;
    @Column(nullable = false, precision = 5, scale = 4)
    private BigDecimal score;
    @Column(nullable = false, length = 20)
    private String severity;
    @Column(name = "reasons_json", nullable = false, columnDefinition = "text")
    private String reasonsJson;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected AnalyticsAssessment() {}

    public AnalyticsAssessment(ReconciliationRun run, ReconciliationItem item, String method,
            String modelVersion, BigDecimal score, String severity, String reasonsJson) {
        this.id = UUID.randomUUID();
        this.run = run;
        this.item = item;
        this.method = method;
        this.modelVersion = modelVersion;
        this.score = score;
        this.severity = severity;
        this.reasonsJson = reasonsJson;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public ReconciliationRun getRun() { return run; }
    public ReconciliationItem getItem() { return item; }
    public String getMethod() { return method; }
    public String getModelVersion() { return modelVersion; }
    public BigDecimal getScore() { return score; }
    public String getSeverity() { return severity; }
    public String getReasonsJson() { return reasonsJson; }
    public Instant getCreatedAt() { return createdAt; }
}
