# Contrato REST do MVP

Base local: `http://localhost:8080/api/v1`. Todas as respostas de dominio sao DTOs; entidades JPA nao saem da camada de persistencia.

| Metodo | Rota | Resultado principal |
|---|---|---|
| `POST` | `/imports/bank` | Importa multipart `file` da origem BANK; 201 |
| `POST` | `/imports/ledger` | Importa multipart `file` da origem LEDGER; 201 |
| `GET` | `/imports` | Lista lotes persistidos |
| `POST` | `/reconciliations` | Cria ou reutiliza run para `bankBatchId` + `ledgerBatchId`; 201/200 |
| `GET` | `/reconciliations` | Lista runs |
| `GET` | `/reconciliations/{id}/summary` | Contagens, taxa e valor sem par |
| `GET` | `/reconciliations/{id}/items` | Pagina resultados; aceita `status`, `page`, `size` |
| `GET` | `/reconciliations/{id}/analytics` | Scores auxiliares separados ou status indisponivel |
| `POST` | `/reconciliation-items/{id}/review` | Registra `APPROVE`/`REJECT` e justificativa |
| `GET` | `/reconciliation-items/{id}/audit` | Trilha append-only do item |
| `GET` | `/audit?runId={id}` | Trilha da run |
| `GET` | `/dashboard/summary` | Agregado da ultima run |
| `GET` | `/actuator/health` | Saude tecnica sem detalhes sensiveis |

## Erro tipado

```json
{
  "type": "about:blank",
  "title": "Conflict",
  "status": 409,
  "code": "DUPLICATE_IMPORT",
  "detail": "Este mesmo CSV ja foi importado para a origem BANK",
  "traceId": "uuid",
  "timestamp": "2026-10-14T00:00:00Z"
}
```

Codigos relevantes: `CSV_EMPTY`, `CSV_FILE_REQUIRED`, `CSV_INVALID_UTF8`, `CSV_INVALID_HEADER`, `CSV_VALIDATION_ERROR`, `CSV_TOO_MANY_ROWS`, `DUPLICATE_IMPORT`, `IMPORT_SOURCE_MISMATCH`, `RECONCILIATION_NOT_FOUND`, `REVIEW_NOT_ALLOWED` e `ANALYTICS_NOT_AVAILABLE`.

## Contrato CSV

Separador `;`, UTF-8 e cabecalho exato:

```text
external_id;reference;transaction_date;amount;currency;description
```

`external_id` deve ser unico no arquivo, `transaction_date` usa ISO, `amount` aceita no maximo duas casas decimais e `currency` deve ser `BRL`. O limite local e 2 MB/5.000 linhas.

