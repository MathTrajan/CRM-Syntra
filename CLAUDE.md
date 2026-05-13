# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Contexto do Projeto

> Gerado por /mapa em 2026-05-13. Grafo: `G:\Meu Drive\Obsidian Vault\Graphify\syntra\graphify-out\Syntra - Grafo.md` · 72 arquivos · 580 nós · 752 arestas · 48 comunidades

### Núcleo do sistema (god nodes)
- `Lead` — 46 conexões — entidade central do CRM, agrega comentários, histórico, tarefas, vendedor
- `DiviaLeadDTO` — 29 conexões — payload da integração externa Divia (sincroniza leads de marketing)
- `LeadService` — 28 conexões — orquestra ciclo de vida do lead, audit trail e regras de pipeline
- `Usuario` — 24 conexões — identidade autenticada (vendedor/admin) ligada a leads e tarefas
- `HistoricoLead` — 22 conexões — log imutável de mudanças por campo (antes/depois) com origem da ação

### Comunidades principais
| ID | Nome | Tamanho |
|---|---|---|
| C0 | Camada de Serviço | 46 nós |
| C1 | Divia Lead Payload | 42 nós |
| C2 | Entidade Lead | 41 nós |
| C3 | Conta e Perfil | 36 nós |
| C4 | Lead Controller (HTML) | 32 nós |
| C5 | Repositórios JPA | 31 nós |
| C6 | Usuário e Autenticação | 29 nós |
| C7 | Histórico (Audit) | 26 nós |
| C8 | Tarefa do Lead | 22 nós |
| C9 | Timeline DTO | 21 nós |
| C10 | Seed e Dashboard | 20 nós |
| C12 | Padrões Arquiteturais | 19 nós |
| C13 | Lead REST API | 18 nós |

### Conexões não óbvias
- `LeadService` é ponte entre Repositórios JPA, Timeline DTO, Lead Controller (HTML) e Histórico — betweenness 0.150, qualquer mudança nele toca 4 comunidades
- `UsuarioRepository` une Camada de Serviço a Conta/Perfil e Autenticação — alterar consultas aqui afeta login E perfil
- 21 nós isolados incluindo `SyntraApplication`, `SecurityConfig`, `AuthController`, `DashboardController` — pontos de entrada legítimos, mas indicam fronteiras pouco documentadas

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
Railway detects the project via `pom.xml` (Nixpacks). `railway.toml` sets the start command. Required environment variables: `DATABASE_URL` (Neon JDBC string), `WEBHOOK_SECRET`, `PORT` (set automatically by Railway).
