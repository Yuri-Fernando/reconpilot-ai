import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ApiService } from './api.service';

describe('ApiService', () => {
  let api: ApiService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [ApiService, provideHttpClient(), provideHttpClientTesting()] });
    api = TestBed.inject(ApiService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('uploads the selected CSV with multipart data to the source endpoint', () => {
    const file = new File(['external_id;reference'], 'bank.csv', { type: 'text/csv' });
    api.upload('BANK', file).subscribe();
    const request = http.expectOne('/api/v1/imports/bank');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toBeInstanceOf(FormData);
    expect((request.request.body as FormData).get('file')).toBe(file);
    request.flush({ batchId: 'b', source: 'BANK', rowsImported: 1, checksum: 'x', status: 'IMPORTED', createdAt: '' });
  });

  it('sends batch ids to the reconciliation endpoint', () => {
    api.createReconciliation('bank-id', 'ledger-id').subscribe();
    const request = http.expectOne('/api/v1/reconciliations');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({ bankBatchId: 'bank-id', ledgerBatchId: 'ledger-id' });
    request.flush({ runId: 'r', status: 'COMPLETED', algorithmVersion: 'rules-v1', created: true,
      analyticsStatus: 'DISABLED', summary: { bankCount: 0, ledgerCount: 0, matched: 0, possible: 0,
        exceptions: 0, reviewed: 0, byStatus: [], unmatchedTotalAmount: '0.00', reconciliationRate: '0.0' } });
  });
});
