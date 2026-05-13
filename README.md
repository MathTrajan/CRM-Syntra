# Syntra CRM

CRM de gestão de leads desenvolvido com Spring Boot, focado em equipes comerciais, automação de atendimento e integração com plataformas externas de captação de leads.

## ✨ Features

* Gestão completa de Leads
* Pipeline comercial
* Histórico/Auditoria de alterações
* Comentários internos por lead
* Sistema de tarefas
* Dashboard com métricas
* Integração via Webhook
* Integração com plataformas externas (Divia)
* Autenticação e controle de acesso
* Atualização dinâmica via AJAX
* Tema dark moderno
* API REST para operações assíncronas

---

## 🛠️ Tecnologias

### Backend

* Java 21
* Spring Boot 3.3.5
* Spring Security
* Spring Data JPA
* Hibernate
* Flyway

### Frontend

* Thymeleaf
* JavaScript
* CSS3

### Banco de Dados

* PostgreSQL (produção)
* H2 Database (desenvolvimento)

### Infraestrutura

* Railway
* Neon PostgreSQL

---

## 📂 Estrutura do Projeto

```bash
com.syntra
├── config
├── controller
│   └── api
├── dto
├── model
├── repository
├── service
└── static
```

---

## 🔐 Segurança

* Autenticação via Spring Security
* Proteção CSRF
* Controle de acesso por perfil
* Webhook protegido por token
* Histórico imutável de alterações

---

## 🌐 Endpoints principais

| Método | Endpoint               | Descrição             |
| ------ | ---------------------- | --------------------- |
| GET    | `/login`               | Tela de autenticação  |
| GET    | `/dashboard`           | Dashboard principal   |
| GET    | `/leads`               | Lista de leads        |
| POST   | `/api/webhook`         | Recebe leads externos |
| PATCH  | `/api/leads/{id}`      | Atualiza lead         |
| GET    | `/api/leads/nao-lidos` | Leads não lidos       |

---

## ⚙️ Variáveis de Ambiente

```env
DATABASE_URL=
WEBHOOK_SECRET=
ADMIN_EMAIL=
ADMIN_PASSWORD=
PORT=
```

---

## 🚀 Rodando localmente

### Desenvolvimento

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### Build produção

```bash
mvn package -DskipTests
```

### Executar JAR

```bash
java -jar target/syntra-0.0.1-SNAPSHOT.jar
```

---

## 🧪 Ambiente DEV

* Banco H2 em memória
* Console H2 disponível em:
  `/h2-console`

```bash
jdbc:h2:mem:syntra
```

Usuário:

```bash
sa
```

Senha:

```bash
(vazio)
```

---

## 📈 Roadmap

* [ ] Integração com WhatsApp
* [ ] Automação de campanhas
* [ ] Sistema Kanban
* [ ] Notificações em tempo real
* [ ] Multiempresa
* [ ] Relatórios avançados
* [ ] Integração Omnichannel
* [ ] API pública documentada

---

## 🧠 Arquitetura

```text
Browser
   ↓
Spring Security
   ↓
Controller
   ↓
Service
   ↓
Repository
   ↓
JPA/Hibernate
   ↓
Database
```

---

## 📌 Observações Técnicas

* Sem uso de Lombok
* Compatível com Java 21
* H2 usa VARCHAR no lugar de ENUM
* Arquivos estáticos exigem restart do servidor
* Logout obrigatório via POST

---

## 👨‍💻 Autor

Desenvolvido por Matheus Trajano.

---

## 📄 Licença

Este projeto está sob licença MIT.
