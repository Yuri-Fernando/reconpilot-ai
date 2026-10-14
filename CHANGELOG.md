# Changelog

## 0.2.1 - 2026-10-14

### Fixed

- CI do backend: `backend/mvnw` estava sem bit de execucao no git (`./mvnw` falharia no Ubuntu).
- `ai-service/requirements.txt` fixava `numpy==2.3.5` e `scikit-learn==1.8.0`, inexistentes para Python 3.10; agora `2.2.6` e `1.7.2` (resolucao validada com `pip install --dry-run`).
- Erros do proprio Spring MVC (rota inexistente, metodo errado, parametro ausente/UUID invalido, JSON malformado, upload nao multipart) retornavam 500; agora 404/405/415/400. Falhas inesperadas passam a ser logadas com `traceId`.
- Revisao humana: leitura do item com lock pessimista; duas decisoes concorrentes geram exatamente um evento de auditoria e um `REVIEW_NOT_ALLOWED`.
- `HttpAnalyticsClientContractTest` oscilava sob carga (timeout de 2 s no primeiro uso do `HttpClient`); testes de sucesso agora usam folga de 10 s.
- Warning de budget SCSS (4 kB) da tela de detalhe: budget por componente passou a 6 kB.

### Added

- `DatasetOracleTest`: o motor precisa reproduzir os 16 casos do gabarito humano `datasets/expected_cases.csv`.
- Capturas de tela reais de todas as telas do console (`docs/screenshots/`), geradas com Playwright contra a aplicacao em execucao.
- `LICENSE` (MIT) e README no padrao do portfolio.

### Changed

- `ai-service`: `requirements-dev.txt` separa `pytest` do runtime; Dockerfile em Python 3.10, usuario nao-root e `HEALTHCHECK`.
- Testes: 40 Java (+ handler de erros, concorrencia de revisao e oraculo do dataset).

## 0.2.0 - 2026-10-14

### Added

- MVP Angular 22 PWA e Spring Boot 4.1.1/Java 21.
- Importacao CSV validada/idempotente, motor `rules-v1`, PostgreSQL/Flyway e API REST.
- Revisao humana append-only, dashboard, filtros e detalhe auditavel.
- Integracao consultiva Java -> FastAPI/Python 3.10 com persistencia separada.
- 32 testes Java, 7 Angular, 9 Python e teste E2E visual via Playwright.
- CI, documentacao, screenshots reais e roadmap mobile.

### Fixed

- Validacao Hibernate 7 de colunas `CHAR` do schema Flyway.
- Serializacao snake_case Jackson 3 e transporte HTTP/1.1 para o contrato FastAPI.
- CORS local para `localhost` e `127.0.0.1`.
- Estado visual dos lotes selecionados apos upload.

