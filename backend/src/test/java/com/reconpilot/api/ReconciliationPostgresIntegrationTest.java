package com.reconpilot.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.reconpilot.api.common.ApiException;
import com.reconpilot.api.importation.ImportService;
import com.reconpilot.api.reconciliation.ReconciliationDtos;
import com.reconpilot.api.reconciliation.ReconciliationItemRepository;
import com.reconpilot.api.reconciliation.ReconciliationService;
import com.reconpilot.api.reconciliation.ReviewStatus;
import com.reconpilot.api.review.ReviewAction;
import com.reconpilot.api.review.ReviewDtos;
import com.reconpilot.api.review.ReviewService;
import com.reconpilot.api.transaction.TransactionSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.mock.web.MockMultipartFile;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest(properties = {
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.jpa.properties.hibernate.default_schema=app"
})
@Testcontainers(disabledWithoutDocker = true)
class ReconciliationPostgresIntegrationTest {

    @Container
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    ImportService importService;

    @Autowired
    ReconciliationService reconciliationService;

    @Autowired
    ReconciliationItemRepository itemRepository;

    @Autowired
    ReviewService reviewService;

    @Test
    void importsFixturesRunsFlywayAndPersistsDeterministicResult() throws Exception {
        byte[] bankCsv = Files.readAllBytes(Path.of("..", "datasets", "bank_transactions.csv"));
        byte[] ledgerCsv = Files.readAllBytes(Path.of("..", "datasets", "ledger_entries.csv"));

        var bank = importService.importCsv(TransactionSource.BANK,
                new MockMultipartFile("file", "bank_transactions.csv", "text/csv", bankCsv));
        var ledger = importService.importCsv(TransactionSource.LEDGER,
                new MockMultipartFile("file", "ledger_entries.csv", "text/csv", ledgerCsv));

        var created = reconciliationService.create(
                new ReconciliationDtos.CreateRequest(bank.batchId(), ledger.batchId()));
        var repeated = reconciliationService.create(
                new ReconciliationDtos.CreateRequest(bank.batchId(), ledger.batchId()));

        assertTrue(created.created());
        assertFalse(repeated.created());
        assertEquals(created.runId(), repeated.runId());
        assertEquals(5, created.summary().matched());
        assertEquals(1, created.summary().possible());
        assertEquals(11, created.summary().exceptions());
        assertEquals("41.7", created.summary().reconciliationRate());

        ApiException duplicate = assertThrows(ApiException.class, () -> importService.importCsv(
                TransactionSource.BANK,
                new MockMultipartFile("file", "bank_transactions.csv", "text/csv", bankCsv)));
        assertEquals("DUPLICATE_IMPORT", duplicate.code());

        assertConcurrentReviewsProduceSingleDecision(created.runId());
    }

    private void assertConcurrentReviewsProduceSingleDecision(java.util.UUID runId) throws Exception {
        var pending = itemRepository.findAllByRunIdOrderByCreatedAtAsc(runId).stream()
                .filter(item -> item.getReviewStatus() == ReviewStatus.PENDING)
                .findFirst().orElseThrow();
        var request = new ReviewDtos.Request(ReviewAction.APPROVE, "Decisao concorrente de teste");
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Callable<String> attempt = () -> {
                start.await();
                try {
                    reviewService.review(pending.getId(), request);
                    return "OK";
                } catch (ApiException exception) {
                    return exception.code();
                }
            };
            List<Future<String>> outcomes = List.of(pool.submit(attempt), pool.submit(attempt));
            start.countDown();
            List<String> results = List.of(outcomes.get(0).get(), outcomes.get(1).get());
            assertEquals(1, results.stream().filter("OK"::equals).count());
            assertEquals(1, results.stream().filter("REVIEW_NOT_ALLOWED"::equals).count());
            assertEquals(1, reviewService.audit(pending.getId()).size());
        } finally {
            pool.shutdownNow();
        }
    }
}
