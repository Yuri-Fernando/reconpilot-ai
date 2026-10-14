# Arquitetura e decisoes

## Componentes

- `frontend/`: Angular 22 standalone, rotas lazy, signals, HttpClient, PWA e adapter mobile web isolado.
- `backend/`: Java 21/Spring Boot MVC, DTOs, validacao, services transacionais, JPA e Flyway.
- `ai-service/`: FastAPI em Python 3.10, estritamente consultivo.
- `datasets/`: fixtures sinteticas e gabarito humano.

## Motor deterministico

O `ReconciliationEngine` recebe snapshots, nao entidades mutaveis. Ele exclui duplicados, aplica referencia bilateralmente unica primeiro, preserva divergencias de referencia como excecoes explicadas e so depois procura valor/moeda/data dentro de dois dias. Um candidato e aceito apenas se a relacao for unica nos dois sentidos. Sobras sao classificadas separadamente.

Essa ordem impede que um score, ordem do banco ou heuristica probabilistica altere o resultado. `rules-v1` e salvo em cada run para reproducibilidade.

## Persistencia

Flyway cria o schema `app`, lotes, transacoes, runs, itens, eventos de revisao e assessments analiticos. Checksums impedem reimportacao da mesma origem. Uma constraint unica impede duplicar a mesma combinacao de lotes/algoritmo. Eventos de revisao sao append-only; transacoes importadas nao sao atualizadas. A decisao humana le o item com lock pessimista (`PESSIMISTIC_WRITE`): duas decisoes concorrentes no mesmo item resultam em exatamente um evento de auditoria e um `REVIEW_NOT_ALLOWED` (coberto por teste contra PostgreSQL real).

## Fronteiras de falha

- CSV invalido: nenhuma linha e persistida.
- PostgreSQL indisponivel: a escrita financeira falha; nao ha resultado ficticio.
- Python desabilitado/timeout/erro/contrato invalido: run permanece valida e a analitica fica `DISABLED` ou `UNAVAILABLE`.
- Resposta Python: IDs e intervalos de score sao validados antes de persistir.
- Erros do cliente (rota, metodo, parametro ou JSON invalidos) retornam 4xx tipado; somente falhas inesperadas viram 500 e sao logadas com `traceId`.

## PWA e mobile

O service worker cacheia somente o shell/ativos do build; nao ha `dataGroups` para cachear APIs financeiras. `MobileAdapter` fornece uma fronteira para futuras capacidades nativas. Capacitor, Android e iOS nao foram adicionados nesta entrega.

