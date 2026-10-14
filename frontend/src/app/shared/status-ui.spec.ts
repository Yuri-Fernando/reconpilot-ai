import { formatBrl, statusLabel, statusTone } from './status-ui';

describe('status UI helpers', () => {
  it('uses text in addition to color for every financial state', () => {
    expect(statusLabel('MATCHED')).toBe('Conciliado');
    expect(statusLabel('AMBIGUOUS')).toBe('Ambíguo');
    expect(statusLabel('AMOUNT_MISMATCH')).toBe('Valor divergente');
  });

  it('maps statuses to stable semantic tones', () => {
    expect(statusTone('MATCHED')).toBe('success');
    expect(statusTone('POSSIBLE_MATCH')).toBe('info');
    expect(statusTone('DUPLICATE')).toBe('warning');
    expect(statusTone('UNMATCHED_BANK')).toBe('danger');
  });

  it('formats decimal strings as BRL without binary arithmetic', () => {
    expect(formatBrl('0.30')).toContain('0,30');
    expect(formatBrl('-49.90')).toContain('49,90');
  });
});
