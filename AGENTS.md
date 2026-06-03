# AGENTS.md — Instruções para agentes de IA (Codex, Gemini, Cursor, Copilot CLI, Aider)

> Auto-gerado por /mapa em 2026-05-19. Leia este arquivo ANTES de qualquer ação no projeto.
> Seções marcadas `<!-- manual:start:NAME -->` ... `<!-- manual:end:NAME -->` são preservadas em re-runs do /mapa.
> O restante (god nodes, anomalias, comandos detectados, contagens) é regenerado a cada execução.

## 0. O que é este projeto

**Syntra** — CRM de leads em Spring Boot 3.3.5 + Java 21 + Thymeleaf + JPA + Flyway + PostgreSQL (Neon em prod, H2 em dev). Em produção no Fly.io.

<!-- manual:start:context -->
- Domínio: capturar leads via webhook/API, atribuir a vendedores, mover pelo pipeline (NOVO → EM_ATENDIMENTO → AGUARDANDO_RETORNO → CONVERTIDO/PERDIDO/EM_OUTRO_ATENDIMENTO).
- Stakeholder único: Matheus Trajano (analista de dados, Colchões Ind).
- URL produção: https://syntra-crm.fly.dev/
- App Fly.io: `syntra-crm` (região `gru`)
- Database produção: Neon Postgres
- Webhook integrado: Divia (lead source externo, sync periódico)
<!-- manual:end:context -->

## 1. Ordem de leitura obrigatória

Leia nesta ordem antes de propor qualquer mudança:

1. **`CLAUDE.md`** (raiz) — convenções específicas, comandos, decisões.
2. **`G:\Meu Drive\Obsidian Vault\Graphify\syntra\_index.md`** — mapa do projeto (knowledge graph).
3. **`G:\Meu Drive\Obsidian Vault\Graphify\syntra\contexts\`** — 4 contextos: architecture, domain, stack, conventions.
4. **`G:\Meu Drive\Obsidian Vault\Graphify\syntra\nodes\C<N>-<nome>-hub.md`** — hub da comunidade tocada pela tarefa.
5. **`nodes/<id>.md`** — nó específico só se hub não responder.
6. Código-fonte — em último caso.

Se não tem acesso ao vault (G:\), leia ao menos `CLAUDE.md` — ele já tem o resumo do mapa.

## 2. Como o Claude Code trabalha aqui (não duplique)

- **`/mapa`** — gera o knowledge graph. Última execução: **2026-05-19** (73 nós, 112 arestas, 7 comunidades).
- **`/sync-vault`** — sincroniza aprendizados com Obsidian.
- Memória persistente do Claude em `C:\Users\User\.claude\projects\` — você (outro agente) não tem acesso; pergunte ao usuário antes de "refazer" algo que parece já decidido.

## 3. God Nodes (não toque sem motivo forte)

<!-- auto:godnodes -->
- `src/main/java/com/syntra/model/Lead.java` — JPA entity core: nome, email, telefone, status, vendedor, comentarios, historico, tarefas (11 conexões)
- `src/main/java/com/syntra/service/LeadService.java` — Orquestrador de mutações em Lead, registra HistoricoLead, gerencia follow-up e timeline (12 conexões)
- `src/main/java/com/syntra/repository/LeadRepository.java` — JpaRepository com JPQL nullable cast e busca regexp_replace em múltiplos campos (8 conexões)
- `src/main/java/com/syntra/controller/api/LeadApiController.java` — REST AJAX: PATCH status/vendor, POST comentario/tarefa, bulk-assign, polling nao-lidos (9 conexões)
- `src/main/java/com/syntra/model/Usuario.java` — JPA entity para autenticação: nome, email, senha (BCrypt), perfil, preferências de notificação (7 conexões)
<!-- /auto:godnodes -->

## 4. Padrões obrigatórios

<!-- manual:start:patterns -->
- **Sem Lombok** — getters/setters explícitos (incompatível com Java 21 annotation processing neste build).
- **CSRF habilitado** — todo form POST inclui `${_csrf.token}` e todo AJAX manda header `X-CSRF-TOKEN`. Logout obrigatoriamente POST.
- **Hash de senha**: BCrypt via `PasswordEncoder` — NUNCA salvar plaintext.
- **Auditoria**: toda mutação em `Lead` deve gravar `HistoricoLead` no `LeadService` (não pule isso).
- **`DaoAuthenticationProvider`** em `SecurityConfig.filterChain()` é **local var, NÃO @Bean** — torná-lo @Bean quebra o login silenciosamente.
- **Flyway** — nunca edite uma migration já aplicada (V1..V12). Crie V13+. O `FlywayConfig` chama `repair()` antes do `migrate()`, mas isso não absolve mudanças destrutivas.
- **Migrations PostgreSQL** — V*.sql roda em Postgres prod. Não use sintaxe H2.
- **`open-in-view=true`** está intencional — templates Thymeleaf acessam `lead.vendedor.nome` lazy.
- **Static files** — mudanças em `static/css/` ou `static/js/` exigem restart do servidor (Spring copia para `target/classes/` na boot).
- **SVG em fragments Thymeleaf** — usar hex direto em `stroke="#XXX"`, não `url(#gradId)` (gradient inline falha silenciosamente).
- **`<select>`** — sempre `appearance: none` + `background-color: var(--bg-card-2)` (Chrome ignora `color-scheme: dark` sozinho).
<!-- manual:end:patterns -->

## 5. Comandos canônicos

<!-- auto:commands -->
**Maven (Java):**
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
mvn test
mvn package -DskipTests
```

**Docker:** build via `Dockerfile` multistage (maven:3.9-eclipse-temurin-17 → eclipse-temurin:17-jre).

**Fly.io:**
```bash
flyctl deploy --remote-only --ha=false
flyctl logs -a syntra-crm
flyctl status -a syntra-crm
flyctl secrets list -a syntra-crm
flyctl ssh console -a syntra-crm
```
<!-- /auto:commands -->

<!-- manual:start:commands -->
**Restart limpo do dev local (Windows PowerShell):**
```powershell
$procId = (netstat -ano | Select-String ":8080 " | Where-Object { $_ -match "LISTENING" } | ForEach-Object { ($_ -split '\s+')[-1] } | Select-Object -First 1)
if ($procId) { Stop-Process -Id ([int]$procId) -Force }
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

**URL produção:** https://syntra-crm.fly.dev/ · health check em `/login`.
<!-- manual:end:commands -->

## 6. Variáveis de ambiente

<!-- manual:start:env -->
| Var | Obrigatória | Descrição |
|-----|-------------|-----------|
| `DATABASE_URL` | sim (prod) | JDBC string do Neon Postgres |
| `WEBHOOK_SECRET` | sim | Valor de `X-Webhook-Token` aceito em `POST /api/webhook` |
| `ADMIN_EMAIL` | recomendado | Email do admin inicial (seed) |
| `ADMIN_PASSWORD` | recomendado | Senha do admin inicial (seed) |
| `DIVIA_API_BASE_URL` | opcional | Endpoint base da API Divia |
| `DIVIA_API_TOKEN` | opcional | Token bearer da API Divia |
| `PORT` | não | Default 8080 |

Em prod: `flyctl secrets set KEY=VALUE -a syntra-crm`. NUNCA commite valores reais — use `.env.example` como template.
<!-- manual:end:env -->

## 7. Anomalias conhecidas (não "corrija" achando que é bug)

<!-- auto:anomalies -->
- `src/main/java/com/syntra/controller/api/WebhookController.java` — Sem CSRF protection, auth via header X-Webhook-Token (intencional, única exceção do projeto).
- `src/main/java/com/syntra/controller/api/LeadApiController.java` — Acessa LeadRepository.countByLidoFalse() direto sem service (conhecido; refatorar com cuidado).
- `src/main/java/com/syntra/config/SecurityConfig.java` — DaoAuthenticationProvider como local var (intencional, não @Bean — torná-lo @Bean quebra login silenciosamente).
- `src/test/java/com/syntra/LeadflowApplicationTests.java` — Nome legado do projeto (LeadFlow → Syntra); renomear é OK.
<!-- /auto:anomalies -->

## 8. Não faça (regras "do not")

<!-- manual:start:donts -->
- ❌ Não rode `git push --force`, `git reset --hard`, `git checkout .` em código não-staged sem pedir.
- ❌ Não delete `target/`, `node_modules/` ou logs sem antes verificar processos Java/Spring rodando (eles seguram arquivos no Windows).
- ❌ Não modifique migrations já aplicadas (V1..V12). Crie V13+.
- ❌ Não suba mudanças destrutivas em DB sem migration reversível.
- ❌ Não adicione Lombok (build vai quebrar com Java 21 annotation processing).
- ❌ Não troque `open-in-view=true` para `false` sem refatorar templates que acessam relações lazy (`lead.vendedor.nome`).
- ❌ Não pule a auditoria em `HistoricoLead` ao mutar um Lead.
- ❌ Não use `--no-verify` em git commits — hooks existem por motivo.
- ❌ Não transforme `DaoAuthenticationProvider` em `@Bean` (login quebra silenciosamente).
- ❌ Não rode `flyctl deploy` sem antes verificar `flyctl status` (deploy em cima de máquina já degradada agrava o problema).
<!-- manual:end:donts -->

## 9. Faça (regras "do")

<!-- manual:start:dos -->
- ✅ Sempre rode `mvn test` antes de propor merge.
- ✅ Sempre adicione/edite migration Flyway nova (V13+) para mudança de schema.
- ✅ Sempre passe mutações em Lead pelo `LeadService` (auditoria automática).
- ✅ Para CSS/JS, lembre que mudanças exigem restart do servidor.
- ✅ Para deploy, use `flyctl deploy --remote-only --ha=false` de dentro da pasta `syntra/`.
- ✅ Quando criar nota nova no vault Obsidian, siga template em `G:\Meu Drive\Obsidian Vault\Graphify\syntra\_meta\conventions.md`.
- ✅ Verifique `flyctl logs -a syntra-crm` ao diagnosticar problemas em produção.
- ✅ Para alterar status/perfil/enums novos, use VARCHAR no Postgres (V6 já dropou o tipo ENUM nativo).
<!-- manual:end:dos -->

## 10. Em caso de dúvida

1. Releia `CLAUDE.md` + `_index.md`.
2. Cheque o hub da comunidade relevante em `G:\Meu Drive\Obsidian Vault\Graphify\syntra\nodes\`.
3. **Pergunte ao usuário** antes de agir em algo destrutivo, deploy, ou refatoração de god node.
