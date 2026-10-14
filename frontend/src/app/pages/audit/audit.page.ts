import { DatePipe } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { finalize } from 'rxjs';
import { ApiService } from '../../core/api/api.service';
import { ApiError, ReviewEvent, RunListItem } from '../../core/models/api.models';

@Component({
  selector: 'app-audit-page',
  imports: [DatePipe],
  templateUrl: './audit.page.html',
  styleUrl: './audit.page.scss',
})
export class AuditPage implements OnInit {
  private readonly api = inject(ApiService);
  protected readonly runs = signal<RunListItem[]>([]);
  protected readonly events = signal<ReviewEvent[]>([]);
  protected readonly selectedRun = signal('');
  protected readonly loading = signal(true);
  protected readonly error = signal<ApiError | null>(null);

  ngOnInit(): void {
    this.api.runs().subscribe({ next: (runs) => { this.runs.set(runs); if (runs[0]) { this.selectedRun.set(runs[0].runId); this.loadAudit(); } else this.loading.set(false); }, error: (error: ApiError) => { this.error.set(error); this.loading.set(false); } });
  }
  protected selectRun(event: Event): void { this.selectedRun.set((event.target as HTMLSelectElement).value); this.loadAudit(); }
  protected loadAudit(): void {
    if (!this.selectedRun()) return;
    this.loading.set(true); this.error.set(null);
    this.api.audit(this.selectedRun()).pipe(finalize(() => this.loading.set(false))).subscribe({
      next: (events) => this.events.set(events), error: (error: ApiError) => this.error.set(error),
    });
  }
}
