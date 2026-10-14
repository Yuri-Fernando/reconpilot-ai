package com.reconpilot.api.importation;

import com.reconpilot.api.transaction.TransactionSource;
import java.time.Instant;
import java.util.UUID;

public record ImportResponse(
        UUID batchId,
        TransactionSource source,
        int rowsImported,
        String checksum,
        String status,
        Instant createdAt) {
    static ImportResponse from(ImportBatch batch) {
        return new ImportResponse(batch.getId(), batch.getSource(), batch.getRowsAccepted(),
                batch.getFileSha256(), "IMPORTED", batch.getCreatedAt());
    }
}
