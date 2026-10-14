# Dados de demonstração

Os CSVs são **100% sintéticos**. Delimitador `;`, codificação UTF-8, data ISO (`yyyy-MM-dd`), decimal com ponto e moeda `BRL`.

| Arquivo | Conteúdo |
|---|---|
| `bank_transactions.csv` | 12 lançamentos do extrato bancário (origem `BANK`) |
| `ledger_entries.csv` | 12 lançamentos do razão contábil/ERP (origem `LEDGER`) |
| `expected_cases.csv` | Gabarito humano: 16 resultados esperados (`bank_external_id;ledger_external_id;expected_status;reason`) |

Cabeçalho dos dados: `external_id;reference;transaction_date;amount;currency;description`.

## Gabarito como oráculo

O gabarito foi definido por caso de negócio, fora do motor. O teste `DatasetOracleTest` (backend) importa os dois CSVs, executa o `ReconciliationEngine` e exige que cada par ou exceção produzido seja **exatamente** o esperado no `expected_cases.csv`.

Convenção para sobras: lançamentos do ERP descartados por ambiguidade ou por candidatos duplicados (`ERP-006`, `ERP-007`, `ERP-009`) são `UNMATCHED_LEDGER`; o status `AMBIGUOUS` aparece apenas do lado bancário (`BANK-006`).

## Casos cobertos

Conciliação exata, tolerância de 2 dias, valor divergente, data divergente, vínculo possível sem referência, ambiguidade de dois candidatos, duplicidade por chave de negócio, valor negativo, centavos e lançamentos sem contraparte nos dois lados.
