import { DatePipe } from '@angular/common';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { finalize, forkJoin } from 'rxjs';
import { ApiService } from '../../core/api/api.service';
import {
  AnalyticsAssessment,
  AnalyticsRun,
  ApiError,
  PageResponse,
  ReconciliationItem,
  ReconciliationStatus,
  ReconciliationSummary,
} from '../../core/models/api.models';
import { formatBrl, statusLabel, statusTone } from '../../shared/status-ui';

@Component({
  selector: 'app-reconciliation-detail-page',
  imports: [DatePipe, ReactiveFormsModule],
  templateUrl: './reconciliation-detail.page.html',
  styleUrl: './reconciliation-detail.page.scss',
})
export class ReconciliationDetailPage implements OnInit {
  private readonly api = inject(ApiService);
  private readonly route = inject(ActivatedRoute);
  protected readonly runId = this.route.snapshot.paramMap.get('id') ?? '';
  protected readonly loading = signal(true);
  protected readonly reviewing = signal(false);
  protected readonly error = signal<ApiError | null>(null);
  protected readonly success = signal('');
  protected readonly summary = signal<ReconciliationSummary | null>(null);
  protected readonly page = signal<PageResponse<ReconciliationItem> | null>(null);
  protected readonly analytics = signal<AnalyticsRun | null>(null);
  protected readonly selected = signal<ReconciliationItem | null>(null);
  protected readonly filter = signal<ReconciliationStatus | ''>('');
  protected readonly query = signal('');
  protected readonly reviewJustification = new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.minLength(5), Validators.maxLength(1000)] });
  protected readonly statuses: ReconciliationStatus[] = ['MATCHED', 'POSSIBLE_MATCH', 'AMOUNT_MISMATCH', 'DATE_MISMATCH', 'AMBIGUOUS', 'DUPLICATE', 'UNMATCHED_BANK', 'UNMATCHED_LEDGER'];
  protected readonly visibleItems = computed(() => {
    const query = this.query().trim().toLowerCase();
    const items = this.page()?.content ?? [];
    if (!query) return items;
    return items.filter((item) => [item.bank?.externalId, item.ledger?.externalId, item.bank?.reference,
      item.ledger?.reference, item.reasonCode, item.explanation].some((value) => value?.toLowerCase().includes(query)));
  });
  private readonly assessmentMap = computed(() => new Map((this.analytics()?.items ?? []).map((item) => [item.itemId, item])));
  protected readonly statusLabel = statusLabel;
  protected readonly statusTone = statusTone;
  protected readonly formatBrl = formatBrl;

  ngOnInit(): void { this.load(); }

  protected load(page = this.page()?.page ?? 0): void {
    this.loading.set(true); this.error.set(null);
    forkJoin({
      summary: this.api.summary(this.runId),
      page: this.api.items(this.runId, page, 50, this.filter() || undefined),
      analytics: this.api.analytics(this.runId),
    }).pipe(finalize(() => this.loading.set(false))).subscribe({
      next: ({ summary, page: result, analytics }) => {
        this.summary.set(summary); this.page.set(result); this.analytics.set(analytics);
        const selected = this.selected();
        if (selected) this.selected.set(result.content.find((item) => item.id === selected.id) ?? selected);
      },
      error: (error: ApiError) => this.error.set(error),
    });
  }

  protected setFilter(event: Event): void {
    this.filter.set((event.target as HTMLSelectElement).value as ReconciliationStatus | '');
    this.load(0);
  }
  protected setQuery(event: Event): void { this.query.set((event.target as HTMLInputElement).value); }
  protected open(item: ReconciliationItem): void { this.selected.set(item); this.reviewJustification.reset(); this.success.set(''); }
  protected close(): void { this.selected.set(null); }
  protected assessment(itemId: string): AnalyticsAssessment | undefined { return this.assessmentMap().get(itemId); }
  protected count(status: ReconciliationStatus): number { return this.summary()?.byStatus.find((value) => value.status === status)?.count ?? 0; }

  protected review(action: 'APPROVE' | 'REJECT'): void {
    const item = this.selected();
    if (!item || this.reviewJustification.invalid) { this.reviewJustification.markAsTouched(); return; }
    this.reviewing.set(true); this.error.set(null);
    this.api.review(item.id, action, this.reviewJustification.value).pipe(finalize(() => this.reviewing.set(false))).subscribe({
      next: () => { this.success.set(action === 'APPROVE' ? 'Vínculo aprovado e auditado.' : 'Vínculo rejeitado e auditado.'); this.load(); },
      error: (error: ApiError) => this.error.set(error),
    });
  }
}
