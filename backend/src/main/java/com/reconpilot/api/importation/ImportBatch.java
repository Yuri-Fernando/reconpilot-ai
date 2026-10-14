package com.reconpilot.api.importation;

import com.reconpilot.api.transaction.TransactionSource;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "import_batches", schema = "app")
public class ImportBatch {
    @Id
    private UUID id;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionSource source;
    @Column(name = "file_name", nullable = false, length = 250)
    private String fileName;
    @Column(name = "file_sha256", nullable = false, length = 64, columnDefinition = "CHAR(64)")
    @JdbcTypeCode(SqlTypes.CHAR)
    private String fileSha256;
    @Column(name = "rows_received", nullable = false)
    private int rowsReceived;
    @Column(name = "rows_accepted", nullable = false)
    private int rowsAccepted;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ImportBatch() {}

    public ImportBatch(TransactionSource source, String fileName, String fileSha256, int rowCount) {
        this.id = UUID.randomUUID();
        this.source = source;
        this.fileName = fileName;
        this.fileSha256 = fileSha256;
        this.rowsReceived = rowCount;
        this.rowsAccepted = rowCount;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public TransactionSource getSource() { return source; }
    public String getFileName() { return fileName; }
    public String getFileSha256() { return fileSha256; }
    public int getRowsReceived() { return rowsReceived; }
    public int getRowsAccepted() { return rowsAccepted; }
    public Instant getCreatedAt() { return createdAt; }
}
