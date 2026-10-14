package com.reconpilot.api.analytics;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnalyticsAssessmentRepository extends JpaRepository<AnalyticsAssessment, UUID> {
    List<AnalyticsAssessment> findByRunIdOrderByScoreDesc(UUID runId);
    boolean existsByRunId(UUID runId);
}
