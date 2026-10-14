CREATE INDEX idx_transaction_batch ON app.financial_transactions(import_batch_id);
CREATE INDEX idx_transaction_reference ON app.financial_transactions(reference);
CREATE INDEX idx_item_run_status ON app.reconciliation_items(run_id, reconciliation_status);
CREATE INDEX idx_review_item ON app.review_events(reconciliation_item_id, created_at);
CREATE UNIQUE INDEX uq_item_bank_per_run ON app.reconciliation_items(run_id, bank_transaction_id)
    WHERE bank_transaction_id IS NOT NULL;
CREATE UNIQUE INDEX uq_item_ledger_per_run ON app.reconciliation_items(run_id, ledger_transaction_id)
    WHERE ledger_transaction_id IS NOT NULL;
