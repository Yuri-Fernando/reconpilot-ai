package com.reconpilot.api.reconciliation;

import com.reconpilot.api.transaction.TransactionSource;
import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class ReconciliationEngine {
    public static final String ALGORITHM_VERSION = "rules-v1";
    private static final BigDecimal EXACT_CONFIDENCE = new BigDecimal("1.000");
    private static final BigDecimal POSSIBLE_CONFIDENCE = new BigDecimal("0.750");
    private static final Comparator<EngineTransaction> ORDER = Comparator
            .comparing(EngineTransaction::transactionDate)
            .thenComparing(EngineTransaction::externalId)
            .thenComparing(EngineTransaction::id);

    public List<EngineResult> reconcile(List<EngineTransaction> input) {
        ensureValidInput(input);
        List<EngineTransaction> banks = input.stream().filter(t -> t.source() == TransactionSource.BANK).sorted(ORDER).toList();
        List<EngineTransaction> ledgers = input.stream().filter(t -> t.source() == TransactionSource.LEDGER).sorted(ORDER).toList();
        List<EngineResult> results = new ArrayList<>();
        Set<UUID> consumedBank = new HashSet<>();
        Set<UUID> consumedLedger = new HashSet<>();

        Set<UUID> bankDuplicates = duplicateIds(banks);
        Set<UUID> ledgerDuplicates = duplicateIds(ledgers);
        banks.stream().filter(t -> bankDuplicates.contains(t.id())).forEach(t -> {
            consumedBank.add(t.id());
            results.add(single(t, null, ReconciliationStatus.DUPLICATE, "DUPLICATE_BUSINESS_KEY",
                    "Lancamento bancario duplicado por referencia, valor e data"));
        });
        ledgers.stream().filter(t -> ledgerDuplicates.contains(t.id())).forEach(t -> {
            consumedLedger.add(t.id());
            results.add(single(null, t, ReconciliationStatus.DUPLICATE, "DUPLICATE_BUSINESS_KEY",
                    "Lancamento contabil duplicado por referencia, valor e data"));
        });

        Map<String, List<EngineTransaction>> bankByReference = groupByReference(banks, consumedBank);
        Map<String, List<EngineTransaction>> ledgerByReference = groupByReference(ledgers, consumedLedger);
        bankByReference.keySet().stream().filter(ledgerByReference::containsKey).sorted().forEach(reference -> {
            List<EngineTransaction> bankCandidates = bankByReference.get(reference);
            List<EngineTransaction> ledgerCandidates = ledgerByReference.get(reference);
            if (bankCandidates.size() == 1 && ledgerCandidates.size() == 1) {
                EngineTransaction bank = bankCandidates.getFirst();
                EngineTransaction ledger = ledgerCandidates.getFirst();
                consumedBank.add(bank.id());
                consumedLedger.add(ledger.id());
                results.add(referenceResult(bank, ledger));
            }
        });

        List<EngineTransaction> residualBanks = banks.stream().filter(t -> !consumedBank.contains(t.id())).toList();
        List<EngineTransaction> residualLedgers = ledgers.stream().filter(t -> !consumedLedger.contains(t.id())).toList();
        Map<UUID, List<EngineTransaction>> candidatesByBank = new LinkedHashMap<>();
        Map<UUID, List<EngineTransaction>> candidatesByLedger = new HashMap<>();
        for (EngineTransaction bank : residualBanks) {
            List<EngineTransaction> candidates = residualLedgers.stream().filter(ledger -> residualCandidate(bank, ledger)).toList();
            candidatesByBank.put(bank.id(), candidates);
            for (EngineTransaction ledger : candidates) {
                candidatesByLedger.computeIfAbsent(ledger.id(), ignored -> new ArrayList<>()).add(bank);
            }
        }

        for (EngineTransaction bank : residualBanks) {
            List<EngineTransaction> candidates = candidatesByBank.get(bank.id());
            if (candidates.size() == 1) {
                EngineTransaction ledger = candidates.getFirst();
                if (candidatesByLedger.getOrDefault(ledger.id(), List.of()).size() == 1) {
                    consumedBank.add(bank.id());
                    consumedLedger.add(ledger.id());
                    results.add(new EngineResult(bank, ledger, ReconciliationStatus.POSSIBLE_MATCH,
                            POSSIBLE_CONFIDENCE, "UNIQUE_VALUE_DATE_BILATERAL",
                            "Valor, moeda e data dentro de 2 dias formam candidato unico nos dois sentidos; revisao humana obrigatoria",
                            ReviewStatus.PENDING));
                }
            }
        }

        for (EngineTransaction bank : residualBanks) {
            if (consumedBank.contains(bank.id())) continue;
            List<EngineTransaction> candidates = candidatesByBank.getOrDefault(bank.id(), List.of());
            ReconciliationStatus status = candidates.isEmpty() ? ReconciliationStatus.UNMATCHED_BANK : ReconciliationStatus.AMBIGUOUS;
            String code = candidates.isEmpty() ? "NO_LEDGER_CANDIDATE" : "MULTIPLE_OR_COMPETING_CANDIDATES";
            String text = candidates.isEmpty()
                    ? "Nenhum lancamento contabil elegivel foi encontrado"
                    : "Existem candidatos concorrentes; nenhum pareamento automatico foi criado";
            results.add(single(bank, null, status, code, text));
            consumedBank.add(bank.id());
        }
        for (EngineTransaction ledger : residualLedgers) {
            if (consumedLedger.add(ledger.id())) {
                results.add(single(null, ledger, ReconciliationStatus.UNMATCHED_LEDGER,
                        "NO_BANK_CANDIDATE_SELECTED", "Lancamento contabil sem par bancario selecionado"));
            }
        }

        return results.stream().sorted(Comparator
                .comparing((EngineResult r) -> r.bank() == null ? "~" : r.bank().externalId())
                .thenComparing(r -> r.ledger() == null ? "~" : r.ledger().externalId()))
                .toList();
    }

    private void ensureValidInput(List<EngineTransaction> input) {
        Set<UUID> ids = new HashSet<>();
        for (EngineTransaction transaction : input) {
            if (!ids.add(transaction.id())) throw new IllegalArgumentException("Transaction id duplicado: " + transaction.id());
        }
    }

    private Set<UUID> duplicateIds(List<EngineTransaction> transactions) {
        return transactions.stream()
                .filter(t -> t.reference() != null)
                .collect(Collectors.groupingBy(t -> new BusinessKey(t.reference(), t.amount(), t.transactionDate().toString(), t.currency())))
                .values().stream()
                .filter(group -> group.size() > 1)
                .flatMap(List::stream)
                .map(EngineTransaction::id)
                .collect(Collectors.toSet());
    }

    private Map<String, List<EngineTransaction>> groupByReference(List<EngineTransaction> values, Set<UUID> consumed) {
        return values.stream()
                .filter(t -> !consumed.contains(t.id()) && t.reference() != null)
                .collect(Collectors.groupingBy(EngineTransaction::reference, LinkedHashMap::new, Collectors.toList()));
    }

    private EngineResult referenceResult(EngineTransaction bank, EngineTransaction ledger) {
        long gap = Math.abs(ChronoUnit.DAYS.between(bank.transactionDate(), ledger.transactionDate()));
        if (!bank.currency().equals(ledger.currency())) {
            return paired(bank, ledger, ReconciliationStatus.CURRENCY_MISMATCH, "REFERENCE_CURRENCY_MISMATCH",
                    "Referencia unica igual, mas moedas divergentes", null);
        }
        if (bank.amount().compareTo(ledger.amount()) != 0) {
            BigDecimal delta = bank.amount().subtract(ledger.amount()).abs();
            return paired(bank, ledger, ReconciliationStatus.AMOUNT_MISMATCH, "REFERENCE_AMOUNT_MISMATCH",
                    "Referencia unica igual, com diferenca de valor de BRL " + delta.toPlainString(), null);
        }
        if (gap > 2) {
            return paired(bank, ledger, ReconciliationStatus.DATE_MISMATCH, "REFERENCE_DATE_OUTSIDE_TOLERANCE",
                    "Referencia e valor iguais, mas datas distam " + gap + " dias", null);
        }
        return paired(bank, ledger, ReconciliationStatus.MATCHED,
                gap == 0 ? "REFERENCE_AMOUNT_DATE_EXACT" : "REFERENCE_AMOUNT_DATE_TOLERANCE",
                gap == 0 ? "Referencia, valor e data correspondem" : "Referencia e valor correspondem; datas dentro da tolerancia de 2 dias",
                EXACT_CONFIDENCE);
    }

    private EngineResult paired(EngineTransaction bank, EngineTransaction ledger, ReconciliationStatus status,
            String code, String explanation, BigDecimal confidence) {
        ReviewStatus review = status == ReconciliationStatus.MATCHED ? ReviewStatus.NOT_REQUIRED : ReviewStatus.PENDING;
        return new EngineResult(bank, ledger, status, confidence, code, explanation, review);
    }

    private EngineResult single(EngineTransaction bank, EngineTransaction ledger, ReconciliationStatus status,
            String code, String explanation) {
        return new EngineResult(bank, ledger, status, null, code, explanation, ReviewStatus.NOT_REQUIRED);
    }

    private boolean residualCandidate(EngineTransaction bank, EngineTransaction ledger) {
        return bank.currency().equals(ledger.currency())
                && bank.amount().compareTo(ledger.amount()) == 0
                && Math.abs(ChronoUnit.DAYS.between(bank.transactionDate(), ledger.transactionDate())) <= 2;
    }

    private record BusinessKey(String reference, BigDecimal amount, String date, String currency) {}
}
