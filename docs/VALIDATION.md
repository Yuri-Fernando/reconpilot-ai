# Evidencias de validacao local

Data: 14/10/2026, Windows/PowerShell, Docker Desktop 29.8.0.

## Toolchain observado

- Java de sistema inicialmente inadequado (Java 8); validacao feita com Temurin JDK 21.0.12.1 portatil.
- Node.js 24.19 e npm 11.8.
- Angular CLI/projeto 22.2.2.
- Spring Boot 4.1.1.
- Python 3.10.8 em `%LOCALAPPDATA%\Programs\Python\Python310\python.exe`.
- PostgreSQL 16 Alpine via Docker.

## Suites executadas (v0.2.0)

```text
Java/Maven: Tests run: 32, Failures: 0, Errors: 0, Skipped: 0; BUILD SUCCESS
Angular/Vitest: Test Files 4 passed; Tests 7 passed
Python/pytest: 9 passed in 19.65s
Angular build: concluido; initial 304.15 kB; warning SCSS nao bloqueante
```

O teste `ReconciliationPostgresIntegrationTest` iniciou PostgreSQL 16 em Testcontainers, aplicou as tres migrations Flyway, importou as fixtures, confirmou 5 matches/11 excecoes, idempotencia da run e `DUPLICATE_IMPORT`.

## Fluxo runtime

- API/DB: 12 linhas BANK + 12 LEDGER, 16 itens, 5 `MATCHED`, 1 `POSSIBLE_MATCH`, 1 `AMOUNT_MISMATCH`, 1 `DATE_MISMATCH`, 1 `AMBIGUOUS`, 2 `DUPLICATE`, 1 `UNMATCHED_BANK`, 4 `UNMATCHED_LEDGER`.
- Taxa: 41,7%; valor absoluto sem par: R$ 928,00.
- Java -> Python 3.10: run com `analyticsStatus=AVAILABLE`, 11 assessments separados e `advisoryOnly=true`.
- Revisao via API: decisao persistida, evento de auditoria criado, valores originais antes/depois inalterados e segunda decisao rejeitada com 409.
- Playwright: upload real dos dois arquivos, selecao, run, dashboard, filtros, tabela e drawer auditavel.

## Defeitos encontrados no E2E e corrigidos

1. POST multipart via `127.0.0.1:4200` recebia 403. A lista CORS passou a aceitar explicitamente `localhost` e `127.0.0.1`, configuravel por `APP_FRONTEND_ORIGINS`.
2. O estado interno selecionava os lotes, mas o elemento `select` ainda exibia `Selecione`. As options agora refletem explicitamente o signal selecionado.

## Nao validado

HTTPS, browsers alem do Chromium local, dispositivos moveis reais, carga/volume de producao, deploy cloud, seguranca ofensiva, recuperacao de desastre e dados financeiros reais.


## Reverificacao em 14/10/2026 (v0.2.1)

Executada na arvore de publicacao (copia local), Temurin JDK 21.0.12.1, Docker Desktop ativo, Node.js 24.19, Python 3.10.8.

```text
Java/Maven: Tests run: 40, Failures: 0, Errors: 0, Skipped: 0; BUILD SUCCESS
Angular/Vitest: Test Files 4 passed; Tests 7 passed
Angular build: concluido; initial 304.15 kB (83.13 kB de transferencia estimada pelo CLI); sem warnings
Python/pytest: 9 passed
```

O `DatasetOracleTest` confirma que o motor reproduz os 16 casos do gabarito humano `datasets/expected_cases.csv`. O `ReconciliationPostgresIntegrationTest` (PostgreSQL 16 em Testcontainers + Flyway) cobre ainda a decisao humana concorrente: de duas chamadas simultaneas, uma e aceita, a outra recebe `REVIEW_NOT_ALLOWED` e existe exatamente um evento de auditoria.

Smoke contra o backend real (`spring-boot:run` + PostgreSQL 16 do compose): `/actuator/health` UP; resumo da run com 5 `MATCHED`, 1 `POSSIBLE_MATCH`, 11 excecoes e taxa de 41,7%; reimport dos CSVs retorna 409 `DUPLICATE_IMPORT`; rota inexistente 404, metodo errado 405, UUID invalido 400. As capturas de `docs/screenshots/` foram geradas com Playwright (Chromium, 1440x900) contra a aplicacao em execucao.

Imagem do `ai-service`: `docker build` com os pins corrigidos (numpy 2.2.6, scikit-learn 1.7.2) em Python 3.10, container como usuario nao-root, `/healthz` OK.

### Falsos negativos observados e tratamento

Rodar tres suites pesadas ao mesmo tempo na mesma maquina produziu dois falsos negativos: (1) timeout de 2 s do cliente HTTP em `HttpAnalyticsClientContractTest`, corrigido dando folga de 10 s aos testes de sucesso (o teste de timeout segue em 100 ms); (2) `Timeout waiting for worker to respond` do Vitest, falha de inicializacao de workers sob carga que passou na reexecucao isolada (7 testes). Os numeros acima sao das execucoes isoladas.

### CI no GitHub Actions

A primeira execucao do workflow (commit `8a9526a`, Ubuntu) terminou verde nos tres jobs: `backend` (`./mvnw verify` com PostgreSQL via Testcontainers), `frontend` (`npm ci`, testes e build) e `analytics` (`pip install -r requirements-dev.txt` com os pins de `requirements.txt` em Python 3.10, seguido de `pytest`). Isso confirma, fora da maquina local, o bit de execucao do `mvnw` e a resolucao dos pins Python.

### Nao validado

Os testes Python locais usaram as versoes ja instaladas na maquina (numpy 2.2.6, scikit-learn 1.7.2, fastapi 0.136.3, pydantic 2.12.5, pytest 9.0.1); a combinacao exata dos pins foi exercitada pelo `docker build` e pelo job `analytics` do CI. Nao ha suite E2E automatizada.
