import { DatePipe } from '@angular/common';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { ApiService } from '../../core/api/api.service';
import { ApiError, RunListItem } from '../../core/models/api.models';

@Component({
  selector: 'app-reconciliations-page',
  imports: [DatePipe, FormsModule, RouterLink],
  templateUrl: './reconciliations.page.html',
  styleUrl: './reconciliations.page.scss',
})
export class ReconciliationsPage implements OnInit {
  private readonly api = inject(ApiService);
  protected readonly loading = signal(true);
  protected readonly error = signal<ApiError | null>(null);
  protected readonly runs = signal<RunListItem[]>([]);
  protected readonly query = signal('');
  protected readonly filtered = computed(() => {
    const query = this.query().trim().toLowerCase();
    return query ? this.runs().filter((run) => run.runId.toLowerCase().includes(query)
      || run.bankBatchId.toLowerCase().includes(query) || run.ledgerBatchId.toLowerCase().includes(query)) : this.runs();
  });

  ngOnInit(): void { this.load(); }
  protected load(): void {
    this.loading.set(true); this.error.set(null);
    this.api.runs().pipe(finalize(() => this.loading.set(false))).subscribe({
      next: (runs) => this.runs.set(runs), error: (error: ApiError) => this.error.set(error),
    });
  }
  protected setQuery(value: string): void { this.query.set(value); }
  protected tone(status: string): string { return status === 'COMPLETED' ? 'success' : status === 'FAILED' ? 'danger' : 'warning'; }
}
