package com.reconpilot.api.importation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.reconpilot.api.common.ApiException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

class CsvParserTest {
    private static final String HEADER = "external_id;reference;transaction_date;amount;currency;description\n";
    private final CsvParser parser = new CsvParser();

    @Test
    void parsesQuotedDelimiterAndExactMoney() {
        List<CsvRow> rows = parser.parse((HEADER + "B1;REF;2026-10-10;0.30;BRL;\"Pagamento; teste\"\n")
                .getBytes(StandardCharsets.UTF_8));
        assertEquals(1, rows.size());
        assertEquals(new BigDecimal("0.30"), rows.getFirst().amount());
        assertEquals("Pagamento; teste", rows.getFirst().description());
    }

    @Test
    void acceptsUtf8Bom() {
        List<CsvRow> rows = parser.parse(("\uFEFF" + HEADER + "B1;;2026-10-10;-49.90;BRL;Estorno\n")
                .getBytes(StandardCharsets.UTF_8));
        assertEquals(new BigDecimal("-49.90"), rows.getFirst().amount());
    }

    @Test
    void rejectsInvalidCurrency() {
        ApiException exception = assertThrows(ApiException.class,
                () -> parse("B1;R;2026-10-10;10.00;USD;Teste"));
        assertEquals("CSV_SCHEMA_INVALID", exception.code());
    }

    @Test
    void rejectsDuplicateExternalIdAtomicallyBeforePersistence() {
        assertThrows(ApiException.class, () -> parser.parse((HEADER
                + "B1;R1;2026-10-10;10.00;BRL;Um\n"
                + "B1;R2;2026-10-11;11.00;BRL;Dois\n").getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void rejectsNonIsoDate() {
        assertThrows(ApiException.class, () -> parse("B1;R;10/10/2026;10.00;BRL;Teste"));
    }

    @Test
    void rejectsMoneyWithoutTwoDecimals() {
        assertThrows(ApiException.class, () -> parse("B1;R;2026-10-10;10.1;BRL;Teste"));
    }

    @Test
    void rejectsUnexpectedHeader() {
        assertThrows(ApiException.class, () -> parser.parse("id;amount\nB1;10.00\n".getBytes(StandardCharsets.UTF_8)));
    }

    private List<CsvRow> parse(String row) {
        return parser.parse((HEADER + row + "\n").getBytes(StandardCharsets.UTF_8));
    }
}
