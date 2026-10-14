package com.reconpilot.api.importation;

import com.reconpilot.api.common.ApiException;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class CsvParser {
    private static final List<String> HEADERS = List.of(
            "external_id", "reference", "transaction_date", "amount", "currency", "description");
    private static final Pattern MONEY = Pattern.compile("-?\\d{1,16}\\.\\d{2}");
    private static final int MAX_ROWS = 5_000;

    public List<CsvRow> parse(byte[] bytes) {
        String content = decodeUtf8(bytes);
        if (content.startsWith("\uFEFF")) content = content.substring(1);
        if (content.isBlank()) throw invalid("O arquivo CSV esta vazio");

        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setDelimiter(';')
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreEmptyLines(true)
                .setTrim(true)
                .get();

        try (Reader reader = new StringReader(content); CSVParser parser = format.parse(reader)) {
            if (!parser.getHeaderNames().equals(HEADERS)) {
                throw invalid("Cabecalho esperado: " + String.join(";", HEADERS));
            }
            List<CsvRow> rows = new ArrayList<>();
            Set<String> externalIds = new HashSet<>();
            for (CSVRecord record : parser) {
                long line = record.getRecordNumber() + 1;
                if (rows.size() >= MAX_ROWS) throw invalid("O CSV excede o limite de 5000 linhas");
                String externalId = required(record, "external_id", line, 100);
                if (!externalIds.add(externalId)) {
                    throw invalid("Linha " + line + ": external_id duplicado no arquivo: " + externalId);
                }
                String reference = optional(record.get("reference"), 100, "reference", line);
                LocalDate date = parseDate(record.get("transaction_date"), line);
                BigDecimal amount = parseAmount(record.get("amount"), line);
                String currency = required(record, "currency", line, 3).toUpperCase();
                if (!"BRL".equals(currency)) throw invalid("Linha " + line + ": currency deve ser BRL");
                String description = required(record, "description", line, 500);
                rows.add(new CsvRow(externalId, reference, date, amount, currency, description));
            }
            if (rows.isEmpty()) throw invalid("O CSV nao contem linhas de dados");
            return List.copyOf(rows);
        } catch (ApiException exception) {
            throw exception;
        } catch (IOException | IllegalArgumentException exception) {
            throw invalid("CSV malformado: " + exception.getMessage());
        }
    }

    private String decodeUtf8(byte[] bytes) {
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes)).toString();
        } catch (CharacterCodingException exception) {
            throw invalid("O arquivo deve usar UTF-8 valido");
        }
    }

    private String required(CSVRecord record, String field, long line, int maxLength) {
        String value = record.get(field).trim();
        if (value.isEmpty()) throw invalid("Linha " + line + ": " + field + " e obrigatorio");
        if (value.length() > maxLength) throw invalid("Linha " + line + ": " + field + " excede " + maxLength + " caracteres");
        return value;
    }

    private String optional(String value, int maxLength, String field, long line) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.length() > maxLength) throw invalid("Linha " + line + ": " + field + " excede " + maxLength + " caracteres");
        return trimmed.isEmpty() ? null : trimmed;
    }

    private LocalDate parseDate(String raw, long line) {
        try {
            return LocalDate.parse(raw.trim());
        } catch (DateTimeParseException exception) {
            throw invalid("Linha " + line + ": transaction_date deve usar yyyy-MM-dd");
        }
    }

    private BigDecimal parseAmount(String raw, long line) {
        String value = raw.trim();
        if (!MONEY.matcher(value).matches()) {
            throw invalid("Linha " + line + ": amount deve ter ponto e duas casas decimais");
        }
        try {
            return new BigDecimal(value).setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException | NumberFormatException exception) {
            throw invalid("Linha " + line + ": amount invalido");
        }
    }

    private ApiException invalid(String detail) {
        return new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "CSV_SCHEMA_INVALID", detail);
    }
}
