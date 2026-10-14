import { ReconciliationStatus } from '../core/models/api.models';

const LABELS: Record<ReconciliationStatus, string> = {
  MATCHED: 'Conciliado',
  POSSIBLE_MATCH: 'Possível vínculo',
  AMOUNT_MISMATCH: 'Valor divergente',
  DATE_MISMATCH: 'Data divergente',
  CURRENCY_MISMATCH: 'Moeda divergente',
  AMBIGUOUS: 'Ambíguo',
  DUPLICATE: 'Duplicado',
  UNMATCHED_BANK: 'Só no banco',
  UNMATCHED_LEDGER: 'Só no ERP',
};

export const statusLabel = (status: ReconciliationStatus): string => LABELS[status];
export const statusTone = (status: ReconciliationStatus): string => {
  if (status === 'MATCHED') return 'success';
  if (status === 'POSSIBLE_MATCH') return 'info';
  if (status === 'AMBIGUOUS' || status === 'DUPLICATE') return 'warning';
  return 'danger';
};
export const formatBrl = (value: string | number | null | undefined): string =>
  new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(Number(value ?? 0));
