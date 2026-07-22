<div align="center">

# 🧩 Syntra — CRM de Leads

CRM B2B para gestão do ciclo de vida de leads: captação via webhook, atendimento por vendedor, alertas de leads parados e visão gerencial em dashboard.

![Java](https://img.shields.io/badge/Java_17-ED8B00?style=flat-square&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot_3.3-6DB33F?style=flat-square&logo=spring-boot&logoColor=white)
![Spring Security](https://img.shields.io/badge/Spring_Security_6-6DB33F?style=flat-square&logo=springsecurity&logoColor=white)
![Thymeleaf](https://img.shields.io/badge/Thymeleaf-005F0F?style=flat-square&logo=thymeleaf&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-316192?style=flat-square&logo=postgresql&logoColor=white)
![Flyway](https://img.shields.io/badge/Flyway-CC0200?style=flat-square&logo=flyway&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-2496ED?style=flat-square&logo=docker&logoColor=white)
![Fly.io](https://img.shields.io/badge/Deploy-Fly.io-8B5CF6?style=flat-square)

</div>

---

## Visão geral

O **Syntra** organiza a operação comercial em torno do lead. Cada lead percorre um ciclo de vida com status bem definidos, é atribuído a um vendedor responsável e acumula um histórico de interações e follow-ups. O sistema captura leads automaticamente por **webhook** (integração com fonte externa), sinaliza proativamente os que estão "esfriando" e consolida os números em um **dashboard** gerencial.

Aplicação server-side renderizada com **Thymeleaf**, autenticação e autorização com **Spring Security**, persistência com **JPA/Hibernate** e schema versionado com **Flyway**.

## Funcionalidades

- **Ciclo de vida do lead** com status (`NOVO`, `CADASTRADO_NO_SITE`, `EM_ATENDIMENTO`, `AGUARDANDO_RETORNO`, `CONVERTIDO`, `PERDIDO`, `EM_OUTRO_ATENDIMENTO`)
- **Captação via webhook** com token secreto e integração externa de leads
- **Central de Atenção** — alertas de leads parados, calculados por status × dias parados, exibidos apenas para o vendedor responsável, com severidade (Atenção / Urgente)
- **Sino de notificações** no topo, com contagem atualizada por polling (~30s)
- **Dashboard** com indicadores da operação
- **Atribuição por vendedor** e **RBAC** por perfil de usuário
- **Timeline de interações e follow-ups** por lead
- **Exportação de leads em XLSX** (ExcelJS)
- **Painel administrativo** e gestão de conta do usuário
- **Busca global** e filtros na listagem de leads

## Stack

| Camada | Tecnologia |
|---|---|
| Linguagem | Java 17 |
| Framework | Spring Boot 3.3.5 (Web, Data JPA, Validation) |
| Segurança | Spring Security 6 + Thymeleaf Extras |
| View | Thymeleaf (SSR) + JS/CSS |
| Persistência | PostgreSQL (produção) · H2 em memória (dev) |
| Migrations | Flyway |
| Build | Maven |
| Container / Deploy | Docker · Fly.io |

## Como rodar (desenvolvimento)

Perfil `dev` sobe com banco **H2 em memória** — não requer PostgreSQL local.

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Aplicação em **http://localhost:8080**.

> As migrations Flyway e o seed inicial são aplicados automaticamente no boot.

## Variáveis de ambiente (produção)

Baseadas em [`.env.example`](./.env.example):

| Variável | Descrição |
|---|---|
| `DATABASE_URL` | String JDBC do PostgreSQL (ex.: Neon), com `sslmode=require` |
| `WEBHOOK_SECRET` | Token que valida as chamadas do webhook de leads |
| `DIVIA_API_BASE_URL` / `DIVIA_API_TOKEN` | Integração com a fonte externa de leads |
| `ADMIN_EMAIL` / `ADMIN_PASSWORD` | Credenciais iniciais do admin (apenas primeiro boot) |
| `PORT` | Porta HTTP (padrão `8080`) |

> ⚠️ Nunca faça commit de valores reais. Use variáveis de ambiente / secrets do provedor.

## Estrutura

```text
src/main/java/com/syntra/
├─ config/       # configuração (segurança, seed dev)
├─ controller/   # rotas web (Auth, Dashboard, Lead, Admin, Conta)
│  └─ api/       # endpoints JSON (leads, alertas, webhook)
├─ dto/          # objetos de transferência (inclui divia/)
├─ model/        # entidades JPA
│  └─ enums/     # StatusLead, Perfil, SeveridadeAlerta, JornadaLead...
├─ repository/   # Spring Data JPA
└─ service/      # regras de negócio (alertas, leads, integração)

src/main/resources/db/  # migrations Flyway (V1..V12) + scripts H2
```

## Deploy

Containerizado via **Dockerfile** e publicado no **Fly.io** (`fly.toml`, região `gru`):

```bash
flyctl deploy --remote-only --ha=false
```

---

<div align="center">

Desenvolvido por **Matheus Trajano** · [LinkedIn](https://www.linkedin.com/in/matheus-trajano-5179a7378/)

</div>
