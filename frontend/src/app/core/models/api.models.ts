export type TransactionSource = 'BANK' | 'LEDGER';
export type RunStatus = 'RUNNING' | 'COMPLETED' | 'FAILED';
export type ReviewStatus = 'NOT_REQUIRED' | 'PENDING' | 'APPROVED' | 'REJECTED';
export type ReconciliationStatus =
  | 'MATCHED'
  | 'POSSIBLE_MATCH'
  | 'AMOUNT_MISMATCH'
  | 'DATE_MISMATCH'
  | 'CURRENCY_MISMATCH'
  | 'AMBIGUOUS'
  | 'DUPLICATE'
  | 'UNMATCHED_BANK'
  | 'UNMATCHED_LEDGER';

export interface ApiError {
  status: number;
  code: string;
  detail: string;
  traceId?: string;
}

export interface ImportBatch {
  batchId: string;
  source: TransactionSource;
  fileName: string;
  rowCount: number;
  createdAt: string;
}

export interface ImportResponse {
  batchId: string;
  source: TransactionSource;
  rowsImported: number;
  checksum: string;
  status: string;
  createdAt: string;
}

export interface TransactionView {
  id: string;
  externalId: string;
  reference: string | null;
  transactionDate: string;
  amount: string;
  currency: string;
  description: string;
}

export interface ReconciliationItem {
  id: string;
  bank: TransactionView | null;
  ledger: TransactionView | null;
  status: ReconciliationStatus;
  reasonCode: string;
  explanation: string;
  reviewStatus: ReviewStatus;
  confidence: number | null;
  createdAt: string;
}

export interface StatusCount {
  status: ReconciliationStatus;
  count: number;
}

export interface ReconciliationSummary {
  bankCount: number;
  ledgerCount: number;
  matched: number;
  possible: number;
  exceptions: number;
  reviewed: number;
  byStatus: StatusCount[];
  unmatchedTotalAmount: string;
  reconciliationRate: string;
}

export interface RunListItem {
  runId: string;
  createdAt: string;
  finishedAt: string | null;
  bankBatchId: string;
  ledgerBatchId: string;
  status: RunStatus;
  algorithmVersion: string;
}

export interface CreateRunResponse {
  runId: string;
  status: RunStatus;
  algorithmVersion: string;
  created: boolean;
  analyticsStatus: string;
  summary: ReconciliationSummary;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  page: number;
  size: number;
}

export interface DashboardSummary {
  lastRun: { runId: string; status: RunStatus; createdAt: string; algorithmVersion: string } | null;
  bankCount: number;
  ledgerCount: number;
  matched: number;
  possible: number;
  exceptionsPending: number;
  reviewed: number;
  reconciliationRate: string;
  volume: string;
}

export interface ReviewEvent {
  id: string;
  itemId: string;
  action: 'APPROVE' | 'REJECT';
  actor: string;
  justification: string;
  previousStatus: ReviewStatus;
  newStatus: ReviewStatus;
  createdAt: string;
}

export interface AnalyticsAssessment {
  id: string;
  itemId: string;
  method: string;
  modelVersion: string;
  score: number;
  severity: 'low' | 'medium' | 'high';
  reasonsJson: string;
  createdAt: string;
}

export interface AnalyticsRun {
  status: string;
  advisoryOnly: boolean;
  items: AnalyticsAssessment[];
}
