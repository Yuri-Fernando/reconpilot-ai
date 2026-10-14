# Roadmap Web -> Android -> iOS

## v0.1 — Web core
- [x] Angular 22 + Spring Boot 4 + PostgreSQL integrado de ponta a ponta
- [x] Importacao real de CSV sintetico e conciliacao deterministica testadas via API
- [ ] Evidencias visuais finais e auditoria manual em navegadores adicionais

## v0.2 — Python sidecar
- [x] Modulo Python FastAPI executado com Python 3.10 e testes automatizados
- [x] Chamada server-to-server Java com segredo interno, timeout e persistencia separada
- [x] Scores consultivos na UI; nunca usados pelo motor de conciliacao
- [ ] Circuit breaker e telemetria operacional (alem do timeout/fail-open atuais)

## v0.3 — PWA
- [x] Responsividade mobile, rotas, manifesto e service worker sem cache de API
- [x] Adaptador web/mobile isolado em `frontend/src/app/core/platform`
- [ ] Auditoria Lighthouse em HTTPS e matriz de navegadores/dispositivos
- [ ] Demo HTTPS sem dados financeiros reais

## v0.5 — Android via Capacitor
- [ ] Validar o adaptador nativo e a estrategia de armazenamento seguro
- [ ] Angular build e `npx cap add android`
- [ ] Android SDK/Gradle/assinatura, testes com aparelho fisico, upload/download CSV
- [ ] AAB assinado; controles de seguranca; preparar politica de privacidade

## v0.6 — Google Play
- [ ] Play Console, taxa unica US$25, 12 testers/14 dias se aplicavel
- [ ] Data safety, screenshots, target SDK, feedback e revisao

## v0.7 — iOS via Capacitor
- [ ] macOS/Xcode 26+ local ou cloud Mac, apple developer US$99/ano
- [ ] TestFlight, revisão de privacidade, screenshots iPhone, requisitos Apple

## v1.0 — Operacao
- [ ] Backups, CI/CD, incident response, rate limits, versionamento de API
- [ ] Política de privacidade publica, exclusao de conta e retention

**Nota:** o Capacitor reutiliza o Angular, nao o Java/Python — estes permanecem servidores. Nao ha garantia de aprovacao nas lojas.

## Decisao desta entrega

Android e iOS permanecem **PLANEJADOS**. Nenhuma pasta nativa foi criada antes da validacao da UI web. A futura implementacao deve fornecer outra instancia de `MobileAdapter`, mantendo o dominio e os clientes HTTP independentes de Capacitor.
