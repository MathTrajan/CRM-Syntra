# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Contexto do Projeto

> Gerado por /mapa em 2026-06-03. Vault: `G:\Meu Drive\Obsidian Vault\Graphify\syntra\` · 76 nós · 118 arestas · 7 comunidades

**Para qualquer dúvida arquitetural, leia primeiro `_index.md` e o hub da comunidade relevante antes de explorar nodes/.**

### Ponto de entrada
`G:\Meu Drive\Obsidian Vault\Graphify\syntra\_index.md`

### God nodes (núcleo — ausência quebra o sistema)
- `[[lead-service]]` — 12 conexões — Orquestrador de mutações, histórico imutável, comentários, tarefas, round-robin, regra Lisandra.
- `[[lead-entity]]` — 11 conexões — JPA entity core com status, jornada, vendedor, dadosExtras, comentários, histórico, tarefas.
- `[[lead-api-controller]]` — 10 conexões — REST AJAX: PATCH status/jornada/vendedor, POST comentário, export XLSX (leadExportToMap), bulk-assign.
- `[[lead-repo]]` — 8 conexões — JPQL extensa: busca multi-campo, regexp_replace telefone, métricas do dashboard.
- `[[usuario-entity]]` — 7 conexões — Identidade autenticada com perfil (ADMIN/VENDEDOR) e preferências de UI.

### Comunidades
| Hub | Nós | Papel |
|-----|-----|-------|
| `[[C0-camada-lead-e-dominio-hub]]` | 21 | Camada Lead e Domínio |
| `[[C1-autenticacao-e-conta-hub]]` | 15 | Autenticação e Conta |
| `[[C2-integracao-divia-hub]]` | 5 | Integração Divia |
| `[[C3-controllers-e-mvc-hub]]` | 10 | Controllers e MVC |
| `[[C4-frontend-js-css-templates-hub]]` | 13 | Frontend JS, CSS e Templates |
| `[[C5-configuracao-e-build-hub]]` | 12 | Configuração, Build, Migrations |
| `[[C6-testes-hub]]` | 1 | Testes |

### Anomalias detectadas
- `[[webhook-controller]]` — Sem CSRF, auth via header X-Webhook-Token (intencional).
- `[[lead-api-controller]]` — Acessa `LeadRepository.countByLidoFalse()` direto sem service.
- `[[security-config]]` — `DaoAuthenticationProvider` como local var (intencional, não @Bean).
- `[[syntra-application-tests]]` — Nome legado `LeadflowApplicationTests` ainda no arquivo.
- `[[leads-detalhe-template]]` — Card Follow-up/Tarefas removido da UI, mas endpoints `/follow-up` e `/tarefas` seguem ativos (UI dead code reativável).

## Commands

```bash
# Run in dev mode (H2 in-memory, no Neon required)
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Run tests
mvn test

# Build production JAR
mvn package -DskipTests

# Run production JAR
java -jar target/syntra-0.0.1-SNAPSHOT.jar
```

Dev server starts on `http://localhost:8080`. H2 console available at `/h2-console` (JDBC URL: `jdbc:h2:mem:syntra`, user: `sa`, no password).

Dev credentials are seeded by `DataInitializer` using `${ADMIN_EMAIL}`/`${ADMIN_PASSWORD}` env vars. Production credentials are delivered out-of-band — they must NEVER appear in this file or in git history.

## Architecture

### Stack
Spring Boot 3.3.5 · Java 21 · Thymeleaf (server-side rendering) · Spring Security · Spring Data JPA · Flyway · PostgreSQL (prod) / H2 (dev). **No Lombok** — all entities use explicit getters/setters (incompatible with Java 21+ annotation processing in this build).

### Request flow
```
Browser → Spring Security filter → Controller → Service → Repository → JPA/Hibernate → DB
```

HTML pages use Thymeleaf templates; JavaScript makes AJAX calls to the REST API layer for in-place updates (no page reloads for status changes, comments, or mark-as-read).

### Package layout (`com.syntra`)
| Package | Responsibility |
|---|---|
| `config` | `SecurityConfig` (auth rules), `DataInitializer` (dev seed, `@Profile("dev")`) |
| `controller` | `LeadController`, `DashboardController`, `AuthController` (HTML views) |
| `controller/api` | `LeadApiController` (AJAX REST), `WebhookController` (external webhook) |
| `model` | JPA entities: `Lead`, `Usuario`, `ComentarioLead`, `HistoricoLead` + enums |
| `dto` | Input contracts: `WebhookPayloadDTO`, `LeadUpdateDTO`, `LeadFiltroDTO`, `ComentarioDTO` |
| `repository` | Spring Data interfaces; complex JPQL in `LeadRepository` |
| `service` | `LeadService` (core logic + audit), `DashboardService` (metrics), `UserDetailsServiceImpl` |

### Data model
- **Lead** is the core entity. It has a nullable `vendedor` (ManyToOne → `Usuario`) and two child collections: `ComentarioLead` (internal notes) and `HistoricoLead` (immutable audit log). Both cascade delete with the lead.
- **`StatusLead`** enum drives the pipeline: `NOVO → EM_ATENDIMENTO → AGUARDANDO_RETORNO → CONVERTIDO | PERDIDO`. Each label (Portuguese display string) is stored on the enum itself.
- **`lido`** (boolean) flags unread leads. Any PATCH to a lead sets `lido = true`. The polling endpoint `GET /api/leads/nao-lidos` powers the sidebar badge.

### Critical security config pattern
`DaoAuthenticationProvider` is instantiated as a **local variable inside `filterChain()`**, not as a `@Bean`. Making it a `@Bean` causes Spring Boot's auto-configuration to ignore the explicit `UserDetailsService`, breaking login silently.

### Dev vs prod profile
| | Dev | Prod |
|---|---|---|
| DB | H2 in-memory | Neon PostgreSQL via `${DATABASE_URL}` |
| Schema | `schema-h2.sql` (Spring SQL init) | Flyway `V1__schema_inicial.sql` |
| Enums | VARCHAR columns | Native PostgreSQL `ENUM` types |
| Data seed | `DataInitializer` bean | Flyway `V2__seed_admin.sql` |
| Thymeleaf cache | off | on |

H2 does not support PostgreSQL `ENUM` types — the dev schema uses `VARCHAR(20/30)`. Never use `columnDefinition = "perfil"` or `@JdbcTypeCode(SqlTypes.JSON)` on entity fields or H2 startup will fail.

### Webhook
`POST /api/webhook` is public (no Spring Security auth). It validates an `X-Webhook-Token` header against the `syntra.webhook.secret` property (env var `WEBHOOK_SECRET`). Accepted payload: `{nome, email, telefone, origem, campanha, mensagem, ...extraFields}`. Extra fields are serialized as JSON into `Lead.dadosExtras` via `@JsonAnySetter` on `WebhookPayloadDTO`.

### Frontend
All CSS is in `static/css/app.css` (single file, design system variables at top). JavaScript is split by page: `app.js` (sidebar, polling, avatar initial), `leads.js` (list auto-refresh), `detalhe.js` (AJAX status/comment updates). CSRF tokens are read from a hidden input `id="csrfToken"` and sent as `X-CSRF-TOKEN` header on AJAX requests. Thymeleaf fragments: `fragments/sidebar.html` and `fragments/navbar.html`, included with `th:replace`.

**Static files require server restart.** `spring.thymeleaf.cache=false` only covers `.html` templates. Changes to `static/css/` or `static/js/` only take effect after restart because Maven copies resources to `target/classes/` at startup. To restart cleanly on Windows:
```powershell
# Find and kill the Java process on port 8080
$procId = (netstat -ano | Select-String ":8080 " | Where-Object { $_ -match "LISTENING" } | ForEach-Object { ($_ -split '\s+')[-1] } | Select-Object -First 1)
Stop-Process -Id ([int]$procId) -Force
# Then restart
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

**SVG inline gradients in fragments** — `stroke="url(#gradientId)"` referencing a `<defs>` block may silently fail when the SVG is inside a Thymeleaf fragment. Use hardcoded hex values directly on `stroke` instead: `stroke="#F5B82E"`.

**`<select>` dark theme** — `color-scheme: dark` alone doesn't prevent Chrome from rendering native white backgrounds. Always add `appearance: none; background-color: var(--bg-card-2)` to select elements and style `option` backgrounds explicitly.

### Logout
Logout **must** use `POST`, not `GET`. Spring Security blocks `GET /logout` when CSRF is enabled. Use this pattern in every template that needs a logout button:
```html
<form th:action="@{/logout}" method="post" style="margin:0;padding:0">
    <input type="hidden" th:name="${_csrf.parameterName}" th:value="${_csrf.token}"/>
    <button type="submit" class="logout-btn">...</button>
</form>
```

### Deployment
Fly.io app `syntra-crm` (region `gru`). Built from the multi-stage `Dockerfile` (Maven 3.9 + Temurin 17 → Temurin 17 JRE), config in `fly.toml` (`internal_port=8080`, health check on `/login`). Database is Neon Postgres; secrets are managed with `fly secrets set` on the app. Required secrets: `DATABASE_URL` (Neon JDBC string), `WEBHOOK_SECRET`, `ADMIN_EMAIL`, `ADMIN_PASSWORD`. Deploy with `flyctl deploy --remote-only --ha=false` from this directory. Production URL: https://syntra-crm.fly.dev/.
