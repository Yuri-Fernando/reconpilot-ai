package com.reconpilot.api.reconciliation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.reconpilot.api.importation.CsvParser;
import com.reconpilot.api.importation.CsvRow;
import com.reconpilot.api.transaction.TransactionSource;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Oraculo independente: o gabarito humano {@code datasets/expected_cases.csv} foi escrito antes e fora do motor.
 * O motor precisa reproduzir o status esperado de cada transacao das fixtures sinteticas.
 */
class DatasetOracleTest {

    @Test
    void engineReproducesHumanExpectedCasesForEveryTransaction() throws Exception {
        CsvParser parser = new CsvParser();
        List<EngineTransaction> input = new ArrayList<>();
        for (CsvRow row : parser.parse(read("bank_transactions.csv"))) input.add(toEngine(TransactionSource.BANK, row));
        for (CsvRow row : parser.parse(read("ledger_entries.csv"))) input.add(toEngine(TransactionSource.LEDGER, row));

        Map<String, String> actual = new TreeMap<>();
        for (EngineResult result : new ReconciliationEngine().reconcile(input)) {
            String key = (result.bank() == null ? "" : result.bank().externalId()) + "|"
                    + (result.ledger() == null ? "" : result.ledger().externalId());
            actual.put(key, result.status().name());
        }

        Map<String, String> expected = new TreeMap<>();
        List<String> lines = Files.readAllLines(datasets("expected_cases.csv"), StandardCharsets.UTF_8);
        for (String line : lines.subList(1, lines.size())) {
            if (line.isBlank()) continue;
            String[] cols = line.split(";", -1);
            expected.put(cols[0] + "|" + cols[1], cols[2]);
        }

        assertEquals(expected, actual);
    }

    private static EngineTransaction toEngine(TransactionSource source, CsvRow row) {
        return new EngineTransaction(UUID.randomUUID(), source, row.externalId(), row.reference(),
                row.transactionDate(), row.amount(), row.currency(), row.description());
    }

    private static byte[] read(String name) throws Exception {
        return Files.readAllBytes(datasets(name));
    }

    private static Path datasets(String name) {
        return Path.of("..", "datasets", name);
    }
}
