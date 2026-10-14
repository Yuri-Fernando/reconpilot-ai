import { DatePipe } from '@angular/common';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { Router } from '@angular/router';
import { finalize } from 'rxjs';
import { ApiService } from '../../core/api/api.service';
import { ApiError, ImportBatch, TransactionSource } from '../../core/models/api.models';

@Component({
  selector: 'app-imports-page',
  imports: [DatePipe],
  templateUrl: './imports.page.html',
  styleUrl: './imports.page.scss',
})
export class ImportsPage implements OnInit {
  private readonly api = inject(ApiService);
  private readonly router = inject(Router);
  protected readonly batches = signal<ImportBatch[]>([]);
  protected readonly bankFile = signal<File | null>(null);
  protected readonly ledgerFile = signal<File | null>(null);
  protected readonly selectedBankId = signal('');
  protected readonly selectedLedgerId = signal('');
  protected readonly uploading = signal<TransactionSource | null>(null);
  protected readonly running = signal(false);
  protected readonly loading = signal(true);
  protected readonly error = signal<ApiError | null>(null);
  protected readonly success = signal('');
  protected readonly bankBatches = computed(() => this.batches().filter((batch) => batch.source === 'BANK'));
  protected readonly ledgerBatches = computed(() => this.batches().filter((batch) => batch.source === 'LEDGER'));
  protected readonly ready = computed(() => !!this.selectedBankId() && !!this.selectedLedgerId() && !this.running());

  ngOnInit(): void { this.loadBatches(); }

  protected selectFile(event: Event, source: TransactionSource): void {
    const file = (event.target as HTMLInputElement).files?.[0] ?? null;
    if (source === 'BANK') this.bankFile.set(file); else this.ledgerFile.set(file);
    this.error.set(null);
  }

  protected upload(source: TransactionSource): void {
    const file = source === 'BANK' ? this.bankFile() : this.ledgerFile();
    if (!file) return;
    this.uploading.set(source);
    this.error.set(null);
    this.success.set('');
    this.api.upload(source, file).pipe(finalize(() => this.uploading.set(null))).subscribe({
      next: (result) => {
        if (source === 'BANK') this.selectedBankId.set(result.batchId); else this.selectedLedgerId.set(result.batchId);
        this.success.set(`${result.rowsImported} linhas de ${source === 'BANK' ? 'banco' : 'ERP'} importadas e persistidas.`);
        this.loadBatches(false);
      },
      error: (error: ApiError) => { this.error.set(error); this.loadBatches(false); },
    });
  }

  protected execute(): void {
    if (!this.ready()) return;
    this.running.set(true);
    this.error.set(null);
    this.api.createReconciliation(this.selectedBankId(), this.selectedLedgerId())
      .pipe(finalize(() => this.running.set(false)))
      .subscribe({
        next: (run) => void this.router.navigate(['/reconciliations', run.runId]),
        error: (error: ApiError) => this.error.set(error),
      });
  }

  protected setBatch(source: TransactionSource, event: Event): void {
    const value = (event.target as HTMLSelectElement).value;
    if (source === 'BANK') this.selectedBankId.set(value); else this.selectedLedgerId.set(value);
  }

  private loadBatches(showLoading = true): void {
    if (showLoading) this.loading.set(true);
    this.api.imports().pipe(finalize(() => this.loading.set(false))).subscribe({
      next: (batches) => {
        this.batches.set(batches);
        if (!this.selectedBankId() && batches.find((batch) => batch.source === 'BANK')) {
          this.selectedBankId.set(batches.find((batch) => batch.source === 'BANK')!.batchId);
        }
        if (!this.selectedLedgerId() && batches.find((batch) => batch.source === 'LEDGER')) {
          this.selectedLedgerId.set(batches.find((batch) => batch.source === 'LEDGER')!.batchId);
        }
      },
      error: (error: ApiError) => this.error.set(error),
    });
  }
}
