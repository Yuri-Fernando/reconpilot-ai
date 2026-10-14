package com.reconpilot.api.importation;

import com.reconpilot.api.transaction.TransactionSource;
import java.time.Instant;
import java.util.UUID;

public record ImportBatchResponse(
        UUID batchId,
        TransactionSource source,
        String fileName,
        int rowCount,
        Instant createdAt) {
    static ImportBatchResponse from(ImportBatch batch) {
        return new ImportBatchResponse(batch.getId(), batch.getSource(), batch.getFileName(),
                batch.getRowsAccepted(), batch.getCreatedAt());
    }
}
