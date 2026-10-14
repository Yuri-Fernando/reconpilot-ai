package com.reconpilot.api.review;

import com.reconpilot.api.reconciliation.ReconciliationItem;
import com.reconpilot.api.reconciliation.ReviewStatus;
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
@Table(name = "review_events", schema = "app")
public class ReviewEvent {
    @Id
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reconciliation_item_id", nullable = false)
    private ReconciliationItem item;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ReviewAction action;
    @Column(nullable = false, length = 100)
    private String actor;
    @Column(nullable = false, length = 1000)
    private String justification;
    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status", nullable = false, length = 20)
    private ReviewStatus previousStatus;
    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", nullable = false, length = 20)
    private ReviewStatus newStatus;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ReviewEvent() {}

    public ReviewEvent(ReconciliationItem item, ReviewAction action, String actor, String justification,
            ReviewStatus previousStatus, ReviewStatus newStatus) {
        this.id = UUID.randomUUID();
        this.item = item;
        this.action = action;
        this.actor = actor;
        this.justification = justification;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public ReconciliationItem getItem() { return item; }
    public ReviewAction getAction() { return action; }
    public String getActor() { return actor; }
    public String getJustification() { return justification; }
    public ReviewStatus getPreviousStatus() { return previousStatus; }
    public ReviewStatus getNewStatus() { return newStatus; }
    public Instant getCreatedAt() { return createdAt; }
}
