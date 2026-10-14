package com.reconpilot.api.importation;

import com.reconpilot.api.common.ApiException;
import com.reconpilot.api.transaction.FinancialTransaction;
import com.reconpilot.api.transaction.TransactionRepository;
import com.reconpilot.api.transaction.TransactionSource;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ImportService {
    private final CsvParser csvParser;
    private final ImportBatchRepository batchRepository;
    private final TransactionRepository transactionRepository;

    public ImportService(CsvParser csvParser, ImportBatchRepository batchRepository,
            TransactionRepository transactionRepository) {
        this.csvParser = csvParser;
        this.batchRepository = batchRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public ImportResponse importCsv(TransactionSource source, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "CSV_EMPTY", "Selecione um arquivo CSV nao vazio");
        }
        String fileName = file.getOriginalFilename() == null ? "upload.csv" : file.getOriginalFilename();
        if (!fileName.toLowerCase().endsWith(".csv")) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "CSV_FILE_REQUIRED", "O arquivo deve ter extensao .csv");
        }
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (Exception exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "FILE_READ_FAILED", "Nao foi possivel ler o upload");
        }
        String checksum = sha256(bytes);
        if (batchRepository.existsBySourceAndFileSha256(source, checksum)) {
            throw new ApiException(HttpStatus.CONFLICT, "DUPLICATE_IMPORT",
                    "Este mesmo CSV ja foi importado para a origem " + source);
        }
        List<CsvRow> rows = csvParser.parse(bytes);
        ImportBatch batch = batchRepository.save(new ImportBatch(source, sanitizeFileName(fileName), checksum, rows.size()));
        List<FinancialTransaction> transactions = rows.stream()
                .map(row -> new FinancialTransaction(batch, row.externalId(), row.reference(), row.transactionDate(),
                        row.amount(), row.currency(), row.description()))
                .toList();
        transactionRepository.saveAll(transactions);
        return ImportResponse.from(batch);
    }

    @Transactional(readOnly = true)
    public List<ImportBatchResponse> list() {
        return batchRepository.findAllByOrderByCreatedAtDesc().stream().map(ImportBatchResponse::from).toList();
    }

    private String sanitizeFileName(String value) {
        String name = value.replace('\\', '/');
        int slash = name.lastIndexOf('/');
        name = slash >= 0 ? name.substring(slash + 1) : name;
        return name.length() > 250 ? name.substring(name.length() - 250) : name;
    }

    private String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }
}
