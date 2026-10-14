CREATE TABLE app.import_batches (
    id UUID PRIMARY KEY,
    source VARCHAR(20) NOT NULL CHECK (source IN ('BANK', 'LEDGER')),
    file_name VARCHAR(250) NOT NULL,
    file_sha256 CHAR(64) NOT NULL,
    rows_received INTEGER NOT NULL,
    rows_accepted INTEGER NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_import_source_hash UNIQUE (source, file_sha256)
);

CREATE TABLE app.financial_transactions (
    id UUID PRIMARY KEY,
    source VARCHAR(20) NOT NULL CHECK (source IN ('BANK', 'LEDGER')),
    import_batch_id UUID NOT NULL REFERENCES app.import_batches(id),
    external_id VARCHAR(100) NOT NULL,
    reference VARCHAR(100),
    transaction_date DATE NOT NULL,
    amount NUMERIC(18,2) NOT NULL,
    currency CHAR(3) NOT NULL DEFAULT 'BRL',
    description VARCHAR(500) NOT NULL,
    normalized_description VARCHAR(500) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_transaction_batch_source_external UNIQUE (import_batch_id, source, external_id)
);

CREATE TABLE app.reconciliation_runs (
    id UUID PRIMARY KEY,
    bank_batch_id UUID NOT NULL REFERENCES app.import_batches(id),
    ledger_batch_id UUID NOT NULL REFERENCES app.import_batches(id),
    algorithm_version VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('RUNNING', 'COMPLETED', 'FAILED')),
    started_at TIMESTAMPTZ NOT NULL,
    finished_at TIMESTAMPTZ,
    CONSTRAINT uq_reconciliation_run UNIQUE (bank_batch_id, ledger_batch_id, algorithm_version)
);

CREATE TABLE app.reconciliation_items (
    id UUID PRIMARY KEY,
    run_id UUID NOT NULL REFERENCES app.reconciliation_runs(id),
    bank_transaction_id UUID REFERENCES app.financial_transactions(id),
    ledger_transaction_id UUID REFERENCES app.financial_transactions(id),
    reconciliation_status VARCHAR(30) NOT NULL,
    confidence NUMERIC(4,3),
    reason_code VARCHAR(50) NOT NULL,
    explanation TEXT NOT NULL,
    review_status VARCHAR(20) NOT NULL,
    reviewed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    CHECK (bank_transaction_id IS NOT NULL OR ledger_transaction_id IS NOT NULL)
);

CREATE TABLE app.review_events (
    id UUID PRIMARY KEY,
    reconciliation_item_id UUID NOT NULL REFERENCES app.reconciliation_items(id),
    action VARCHAR(30) NOT NULL CHECK (action IN ('APPROVE', 'REJECT')),
    actor VARCHAR(100) NOT NULL,
    justification VARCHAR(1000) NOT NULL,
    previous_status VARCHAR(20) NOT NULL,
    new_status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE app.analytics_assessments (
    id UUID PRIMARY KEY,
    run_id UUID NOT NULL REFERENCES app.reconciliation_runs(id),
    item_id UUID NOT NULL REFERENCES app.reconciliation_items(id),
    method VARCHAR(100) NOT NULL,
    model_version VARCHAR(100) NOT NULL,
    score NUMERIC(5,4) NOT NULL CHECK (score >= 0 AND score <= 1),
    severity VARCHAR(20) NOT NULL,
    reasons_json TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_analytics_item_model UNIQUE (item_id, model_version)
);
