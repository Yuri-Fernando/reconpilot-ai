# ReconPilot — Frontend

Console do operador em Angular 22 (standalone components, signals, rotas lazy) com PWA. Fala somente com a API Spring (`/api/v1`); o proxy de desenvolvimento (`proxy.conf.json`) encaminha para `http://localhost:8080`.

| Rota | Tela |
|---|---|
| `/dashboard` | Visão geral da última execução |
| `/imports` | Upload dos CSVs e escolha dos lotes |
| `/reconciliations` | Histórico de execuções |
| `/reconciliations/:id` | Resultados, filtros e drawer de detalhe com decisão humana |
| `/audit` | Trilha de auditoria append-only |
| `/about` | Arquitetura do projeto |

```bash
npm ci               # instala dependências
npm start            # http://localhost:4200 (requer o backend em :8080)
npm test -- --watch=false   # Vitest
npm run build        # build de produção + service worker
```

O service worker faz cache apenas do shell e dos ativos do build; nenhuma rota da API financeira é cacheada (`ngsw-config.json`). A fronteira para capacidades nativas futuras fica em `src/app/core/platform/mobile-adapter.ts`.
