import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { ApiService } from '../../core/api/api.service';
import { DashboardPage } from './dashboard.page';

describe('DashboardPage', () => {
  let fixture: ComponentFixture<DashboardPage>;
  const api = {
    dashboard: () => of({ lastRun: { runId: 'run-1', status: 'COMPLETED', createdAt: '2026-10-13T12:00:00Z', algorithmVersion: 'rules-v1' },
      bankCount: 12, ledgerCount: 12, matched: 5, possible: 1, exceptionsPending: 10, reviewed: 1,
      reconciliationRate: '41.7', volume: '1214.99' }),
    runs: () => of([]),
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [DashboardPage], providers: [provideRouter([]), { provide: ApiService, useValue: api }] }).compileComponents();
    fixture = TestBed.createComponent(DashboardPage);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
  });

  it('renders counters returned by the backend instead of mock arrays', () => {
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('12 lançamentos');
    expect(text).toContain('41.7%');
    expect(text).toContain('10');
  });
});
