package com.reconpilot.api.reconciliation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.reconpilot.api.transaction.TransactionSource;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ReconciliationEngineTest {
    private final ReconciliationEngine engine = new ReconciliationEngine();

    @Test
    void exactReferenceAmountAndDateMatches() {
        EngineResult result = only(bank("B1", "REF-1", "2026-10-10", "125.00"), ledger("L1", "REF-1", "2026-10-10", "125.00"));
        assertEquals(ReconciliationStatus.MATCHED, result.status());
        assertEquals(new BigDecimal("1.000"), result.confidence());
    }

    @Test
    void referenceMatchAcceptsTwoDayGap() {
        EngineResult result = only(bank("B1", "REF-1", "2026-10-10", "250.00"), ledger("L1", "REF-1", "2026-10-12", "250.00"));
        assertEquals(ReconciliationStatus.MATCHED, result.status());
        assertEquals("REFERENCE_AMOUNT_DATE_TOLERANCE", result.reasonCode());
    }

    @Test
    void uniqueReferencePreservesAmountMismatchPair() {
        EngineResult result = only(bank("B1", "REF-1", "2026-10-10", "315.50"), ledger("L1", "REF-1", "2026-10-10", "310.50"));
        assertEquals(ReconciliationStatus.AMOUNT_MISMATCH, result.status());
        assertEquals(ReviewStatus.PENDING, result.reviewStatus());
    }

    @Test
    void uniqueReferencePreservesDateMismatchPair() {
        EngineResult result = only(bank("B1", "REF-1", "2026-10-10", "87.90"), ledger("L1", "REF-1", "2026-10-14", "87.90"));
        assertEquals(ReconciliationStatus.DATE_MISMATCH, result.status());
    }

    @Test
    void bilaterallyUniqueValueAndDateBecomesPossibleMatch() {
        EngineResult result = only(bank("B1", null, "2026-10-10", "72.40"), ledger("L1", "OTHER", "2026-10-11", "72.40"));
        assertEquals(ReconciliationStatus.POSSIBLE_MATCH, result.status());
        assertEquals(new BigDecimal("0.750"), result.confidence());
    }

    @Test
    void twoLedgerCandidatesRemainAmbiguousAndUnpaired() {
        List<EngineResult> results = engine.reconcile(List.of(
                bank("B1", null, "2026-10-12", "44.00"),
                ledger("L1", "OTHER-1", "2026-10-12", "44.00"),
                ledger("L2", "OTHER-2", "2026-10-12", "44.00")));
        assertEquals(1, count(results, ReconciliationStatus.AMBIGUOUS));
        assertEquals(2, count(results, ReconciliationStatus.UNMATCHED_LEDGER));
        assertFalse(results.stream().anyMatch(r -> r.status() == ReconciliationStatus.POSSIBLE_MATCH));
    }

    @Test
    void bankOnlyIsUnmatchedBank() {
        assertEquals(ReconciliationStatus.UNMATCHED_BANK, only(bank("B1", "NOPE", "2026-10-10", "1.00")).status());
    }

    @Test
    void ledgerOnlyIsUnmatchedLedger() {
        assertEquals(ReconciliationStatus.UNMATCHED_LEDGER, only(ledger("L1", "NOPE", "2026-10-10", "1.00")).status());
    }

    @Test
    void duplicateBusinessKeysAreExcludedFromMatching() {
        List<EngineResult> results = engine.reconcile(List.of(
                bank("B1", "REF-DUP", "2026-10-10", "20.00"),
                bank("B2", "REF-DUP", "2026-10-10", "20.00"),
                ledger("L1", "REF-DUP", "2026-10-10", "20.00")));
        assertEquals(2, count(results, ReconciliationStatus.DUPLICATE));
        assertEquals(1, count(results, ReconciliationStatus.UNMATCHED_LEDGER));
        assertEquals(0, count(results, ReconciliationStatus.MATCHED));
    }

    @Test
    void competingBanksCannotGreedilyConsumeOneLedger() {
        List<EngineResult> results = engine.reconcile(List.of(
                bank("B1", null, "2026-10-10", "50.00"),
                bank("B2", null, "2026-10-11", "50.00"),
                ledger("L1", null, "2026-10-10", "50.00")));
        assertEquals(2, count(results, ReconciliationStatus.AMBIGUOUS));
        assertEquals(1, count(results, ReconciliationStatus.UNMATCHED_LEDGER));
    }

    @Test
    void negativeAmountsMatchWithoutLosingSign() {
        assertEquals(ReconciliationStatus.MATCHED,
                only(bank("B1", "R", "2026-10-10", "-49.90"), ledger("L1", "R", "2026-10-10", "-49.90")).status());
    }

    @Test
    void decimalCentsUseBigDecimal() {
        BigDecimal cents = new BigDecimal("0.10").add(new BigDecimal("0.20"));
        EngineResult result = only(bank("B1", "R", "2026-10-10", cents.toPlainString()), ledger("L1", "R", "2026-10-10", "0.30"));
        assertEquals(ReconciliationStatus.MATCHED, result.status());
        assertEquals(new BigDecimal("0.30"), result.bank().amount());
    }

    @Test
    void referenceWithDifferentCurrencyIsNotMatched() {
        EngineTransaction usd = tx(TransactionSource.LEDGER, "L1", "R", "2026-10-10", "10.00", "USD");
        EngineResult result = only(bank("B1", "R", "2026-10-10", "10.00"), usd);
        assertEquals(ReconciliationStatus.CURRENCY_MISMATCH, result.status());
    }

    @Test
    void resultsAreDeterministicRegardlessOfInputOrder() {
        List<EngineTransaction> input = new ArrayList<>(List.of(
                bank("B2", "R2", "2026-10-11", "2.00"), ledger("L1", "R1", "2026-10-10", "1.00"),
                bank("B1", "R1", "2026-10-10", "1.00"), ledger("L2", "R2", "2026-10-11", "2.00")));
        List<EngineResult> first = engine.reconcile(input);
        input.reversed();
        List<EngineResult> second = engine.reconcile(new ArrayList<>(input.reversed()));
        assertEquals(first, second);
    }

    @Test
    void duplicateTechnicalIdIsRejected() {
        EngineTransaction bank = bank("B1", "R", "2026-10-10", "1.00");
        assertThrows(IllegalArgumentException.class, () -> engine.reconcile(List.of(bank, bank)));
    }

    private EngineResult only(EngineTransaction... transactions) {
        List<EngineResult> results = engine.reconcile(List.of(transactions));
        assertEquals(1, results.size());
        return results.getFirst();
    }

    private long count(List<EngineResult> results, ReconciliationStatus status) {
        return results.stream().filter(result -> result.status() == status).count();
    }

    private EngineTransaction bank(String id, String reference, String date, String amount) {
        return tx(TransactionSource.BANK, id, reference, date, amount, "BRL");
    }

    private EngineTransaction ledger(String id, String reference, String date, String amount) {
        return tx(TransactionSource.LEDGER, id, reference, date, amount, "BRL");
    }

    private EngineTransaction tx(TransactionSource source, String id, String reference, String date, String amount, String currency) {
        UUID uuid = UUID.nameUUIDFromBytes((source + id).getBytes(StandardCharsets.UTF_8));
        return new EngineTransaction(uuid, source, id, reference, LocalDate.parse(date), new BigDecimal(amount), currency, id);
    }
}
