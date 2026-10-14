package com.reconpilot.api.analytics;

import com.reconpilot.api.reconciliation.ReconciliationItem;
import com.reconpilot.api.reconciliation.ReconciliationRun;
import com.reconpilot.api.reconciliation.ReconciliationStatus;
import com.reconpilot.api.transaction.FinancialTransaction;
import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientResponseException;

@Service
public class AnalyticsService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AnalyticsService.class);
    private final AnalyticsClient client;
    private final AnalyticsAssessmentRepository repository;

    public AnalyticsService(AnalyticsClient client, AnalyticsAssessmentRepository repository) {
        this.client = client;
        this.repository = repository;
    }

    @Transactional
    public String analyze(ReconciliationRun run, List<ReconciliationItem> allItems) {
        if (!client.enabled()) return "DISABLED";
        List<ReconciliationItem> candidates = allItems.stream()
                .filter(item -> item.getReconciliationStatus() != ReconciliationStatus.MATCHED)
                .toList();
        if (candidates.isEmpty()) return "NOT_REQUIRED";
        AnalyticsDtos.Request request = new AnalyticsDtos.Request(run.getId().toString(),
                candidates.stream().map(this::candidate).toList(), false);
        try {
            AnalyticsDtos.Response response = client.analyze(request);
            validate(response, request);
            Map<UUID, ReconciliationItem> byId = candidates.stream()
                    .collect(Collectors.toMap(ReconciliationItem::getId, Function.identity()));
            List<AnalyticsAssessment> assessments = response.items().stream().map(score -> {
                UUID itemId = UUID.fromString(score.transactionId());
                return new AnalyticsAssessment(run, byId.get(itemId), response.method(), response.modelVersion(),
                        score.riskScore(), score.severity(), reasonsJson(score.reasons()));
            }).toList();
            repository.saveAll(assessments);
            LOGGER.info("Analytics advisory persisted runId={} method={} modelVersion={}",
                    run.getId(), response.method(), response.modelVersion());
            return "AVAILABLE";
        } catch (RuntimeException exception) {
            if (exception instanceof RestClientResponseException responseException) {
                LOGGER.warn("Analytics HTTP failure runId={} status={} detail={}", run.getId(),
                        responseException.getStatusCode().value(), responseException.getResponseBodyAsString());
            }
            LOGGER.warn("Analytics advisory unavailable for runId={} type={}", run.getId(), exception.getClass().getSimpleName());
            return "UNAVAILABLE";
        }
    }

    @Transactional(readOnly = true)
    public String status(UUID runId) {
        if (repository.existsByRunId(runId)) return "AVAILABLE";
        return client.enabled() ? "UNAVAILABLE_OR_NOT_REQUESTED" : "DISABLED";
    }

    @Transactional(readOnly = true)
    public AnalyticsDtos.RunView list(UUID runId) {
        List<AnalyticsDtos.AssessmentView> values = repository.findByRunIdOrderByScoreDesc(runId).stream()
                .map(AnalyticsDtos.AssessmentView::from).toList();
        return new AnalyticsDtos.RunView(values.isEmpty() ? status(runId) : "AVAILABLE", true, values);
    }

    private AnalyticsDtos.Candidate candidate(ReconciliationItem item) {
        FinancialTransaction primary = item.getBankTransaction() != null
                ? item.getBankTransaction() : item.getLedgerTransaction();
        long amountCents = primary.getAmount().movePointRight(2).longValueExact();
        long deltaCents = 0;
        int dateGapDays = 0;
        if (item.getBankTransaction() != null && item.getLedgerTransaction() != null) {
            deltaCents = item.getBankTransaction().getAmount().subtract(item.getLedgerTransaction().getAmount())
                    .movePointRight(2).longValueExact();
            dateGapDays = Math.toIntExact(Math.abs(ChronoUnit.DAYS.between(
                    item.getBankTransaction().getTransactionDate(), item.getLedgerTransaction().getTransactionDate())));
        }
        boolean duplicate = item.getReconciliationStatus() == ReconciliationStatus.DUPLICATE;
        boolean referenceConflict = item.getReconciliationStatus() == ReconciliationStatus.AMOUNT_MISMATCH
                || item.getReconciliationStatus() == ReconciliationStatus.DATE_MISMATCH
                || item.getReconciliationStatus() == ReconciliationStatus.CURRENCY_MISMATCH;
        return new AnalyticsDtos.Candidate(item.getId().toString(), amountCents, deltaCents,
                dateGapDays, duplicate, referenceConflict);
    }

    void validate(AnalyticsDtos.Response response, AnalyticsDtos.Request request) {
        if (response == null || !request.batchId().equals(response.batchId())) {
            throw new IllegalStateException("Analytics response batch_id mismatch");
        }
        if (!"none_advisory_only".equals(response.decisionAuthority()) || response.scoresAreProbabilities()) {
            throw new IllegalStateException("Analytics response violates advisory-only contract");
        }
        if (response.modelVersion() == null || response.modelVersion().isBlank()
                || response.method() == null || response.method().isBlank() || response.items() == null) {
            throw new IllegalStateException("Analytics response metadata is incomplete");
        }
        Set<String> expected = request.candidates().stream().map(AnalyticsDtos.Candidate::transactionId).collect(Collectors.toSet());
        Set<String> actual = new HashSet<>();
        for (AnalyticsDtos.Score score : response.items()) {
            if (!expected.contains(score.transactionId()) || !actual.add(score.transactionId())) {
                throw new IllegalStateException("Analytics returned unknown or duplicate transaction_id");
            }
            if (score.riskScore() == null || score.riskScore().compareTo(BigDecimal.ZERO) < 0
                    || score.riskScore().compareTo(BigDecimal.ONE) > 0) {
                throw new IllegalStateException("Analytics score outside [0,1]");
            }
            if (!Set.of("low", "medium", "high").contains(score.severity()) || score.reasons() == null) {
                throw new IllegalStateException("Analytics severity or reasons invalid");
            }
            if (score.reasons().stream().anyMatch(reason -> !reason.matches("[a-z0-9_]{1,80}"))) {
                throw new IllegalStateException("Analytics reason is invalid");
            }
        }
        if (!actual.equals(expected)) throw new IllegalStateException("Analytics response omitted candidates");
    }

    private String reasonsJson(List<String> reasons) {
        return reasons.stream().map(reason -> "\"" + reason + "\"").collect(Collectors.joining(",", "[", "]"));
    }
}
