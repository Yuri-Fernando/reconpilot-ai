import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import {
  AnalyticsRun,
  CreateRunResponse,
  DashboardSummary,
  ImportBatch,
  ImportResponse,
  PageResponse,
  ReconciliationItem,
  ReconciliationStatus,
  ReconciliationSummary,
  ReviewEvent,
  RunListItem,
  TransactionSource,
} from '../models/api.models';

@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly http = inject(HttpClient);
  private readonly base = '/api/v1';

  dashboard(): Observable<DashboardSummary> {
    return this.http.get<DashboardSummary>(`${this.base}/dashboard/summary`);
  }

  imports(): Observable<ImportBatch[]> {
    return this.http.get<ImportBatch[]>(`${this.base}/imports`);
  }

  upload(source: TransactionSource, file: File): Observable<ImportResponse> {
    const data = new FormData();
    data.append('file', file);
    return this.http.post<ImportResponse>(`${this.base}/imports/${source.toLowerCase()}`, data);
  }

  createReconciliation(bankBatchId: string, ledgerBatchId: string): Observable<CreateRunResponse> {
    return this.http.post<CreateRunResponse>(`${this.base}/reconciliations`, { bankBatchId, ledgerBatchId });
  }

  runs(): Observable<RunListItem[]> {
    return this.http.get<RunListItem[]>(`${this.base}/reconciliations`);
  }

  summary(runId: string): Observable<ReconciliationSummary> {
    return this.http.get<ReconciliationSummary>(`${this.base}/reconciliations/${runId}/summary`);
  }

  items(runId: string, page = 0, size = 50, status?: ReconciliationStatus): Observable<PageResponse<ReconciliationItem>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (status) params = params.set('status', status);
    return this.http.get<PageResponse<ReconciliationItem>>(`${this.base}/reconciliations/${runId}/items`, { params });
  }

  review(itemId: string, action: 'APPROVE' | 'REJECT', justification: string): Observable<unknown> {
    return this.http.post(`${this.base}/reconciliation-items/${itemId}/reviews`, { action, justification });
  }

  audit(runId: string): Observable<ReviewEvent[]> {
    return this.http.get<ReviewEvent[]>(`${this.base}/audit`, { params: { runId } });
  }

  analytics(runId: string): Observable<AnalyticsRun> {
    return this.http.get<AnalyticsRun>(`${this.base}/reconciliations/${runId}/analytics`);
  }
}
