import { DatePipe } from '@angular/common';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { finalize, forkJoin } from 'rxjs';
import { ApiService } from '../../core/api/api.service';
import { ApiError, DashboardSummary, RunListItem } from '../../core/models/api.models';
import { formatBrl, statusLabel, statusTone } from '../../shared/status-ui';

@Component({
  selector: 'app-dashboard-page',
  imports: [RouterLink, DatePipe],
  templateUrl: './dashboard.page.html',
  styleUrl: './dashboard.page.scss',
})
export class DashboardPage implements OnInit {
  private readonly api = inject(ApiService);
  protected readonly loading = signal(true);
  protected readonly error = signal<ApiError | null>(null);
  protected readonly summary = signal<DashboardSummary | null>(null);
  protected readonly runs = signal<RunListItem[]>([]);
  protected readonly hasData = computed(() => this.summary()?.lastRun !== null);
  protected readonly formatBrl = formatBrl;

  ngOnInit(): void { this.refresh(); }

  protected refresh(): void {
    this.loading.set(true);
    this.error.set(null);
    forkJoin({ summary: this.api.dashboard(), runs: this.api.runs() })
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: ({ summary, runs }) => { this.summary.set(summary); this.runs.set(runs.slice(0, 5)); },
        error: (error: ApiError) => this.error.set(error),
      });
  }

  protected tone(status: string): string {
    return status === 'COMPLETED' ? 'success' : status === 'FAILED' ? 'danger' : 'warning';
  }
  protected runLabel(status: string): string {
    return status === 'COMPLETED' ? 'Concluída' : status === 'FAILED' ? 'Falhou' : 'Em execução';
  }
}
