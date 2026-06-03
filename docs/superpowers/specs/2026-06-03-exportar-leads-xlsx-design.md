# Exportar Leads em XLSX — Design (Syntra)

Data: 2026-06-03
Projeto: Syntra (Spring Boot 3.3.5 · Java 21 · Thymeleaf · JS puro server-side)
Status: aprovado para virar plano de implementação

## Objetivo

Substituir a exportação JSON da lista de leads por uma exportação **XLSX** formatada,
gerada **no frontend** (navegador), com uma aba "Leads" contendo a tabela completa —
dados que vêm da API/webhook + dados preenchidos no sistema — respeitando os filtros
ativos da tela. Sistema em produção: **sem deploy e sem commit** até validação local.

## Decisões tomadas

- **Formato:** XLSX com colunas formatadas e organizadas.
- **Conteúdo:** uma aba "Leads", 1 linha por lead, tabela completa (sem resumo, sem
  comentários/histórico).
- **Geração:** frontend (navegador), usando **ExcelJS**.
- **Biblioteca:** ExcelJS UMD **vendorizado** em `static/js/vendor/exceljs.min.js`,
  carregado **sob demanda** (injeção de `<script>` só ao clicar em Exportar) para não
  pesar a tela de leads. SheetJS community foi descartado (não estiliza células).
- **Botão:** "Exportar JSON" vira **"Exportar Excel"**.
- **Backend:** mudança mínima — apenas expor todos os campos necessários no endpoint de
  export, com datas em ISO. A geração do arquivo permanece no frontend.

## Arquitetura

Fluxo: `Browser (clique Exportar Excel) → lazy-load ExcelJS → GET /api/leads/export?<filtros>
→ JSON → monta Workbook (aba "Leads") → Blob → download`.

### Backend (Java)

- `LeadApiController.exportar` (GET `/api/leads/export`) já existe e já reusa
  `LeadService.exportar(filtro)` (todos os leads do filtro, sem paginação). Mantém-se.
- Criar método privado **`leadExportToMap(Lead)`** em `LeadApiController` (NÃO alterar o
  `leadToMap` existente, usado por `bulk-assign`/`toMap`). O endpoint `/export` passa a
  usar `leadExportToMap`. Campos retornados (chaves do JSON):
  - `nome`, `email`, `telefone`, `origem`, `campanha`, `mensagem`
  - `jornadaLabel` (ou null), `statusLabel`
  - `vendedor` (nome ou null)
  - `proximaAcao`
  - `proximoContatoEm` (ISO `LocalDateTime.toString()` ou null)
  - `ultimaInteracaoEm` (ISO ou null)
  - `lido` (boolean)
  - `origemExterna`
  - `dadosExtras`
  - `criadoEm` (ISO ou null)
  - `atualizadoEm` (ISO ou null)
  - Datas em ISO (sem formatação) para o front criar células de data reais.

### Frontend (JS puro)

- `static/js/vendor/exceljs.min.js` — arquivo vendorizado (build UMD oficial do ExcelJS).
- `static/js/leads.js` — reescrever `exportarLeads()`:
  1. Garante o ExcelJS carregado: se `window.ExcelJS` ausente, injeta o `<script>` de
     `/js/vendor/exceljs.min.js` uma vez e aguarda `onload` (Promise). Se já carregado, segue.
  2. `fetch('/api/leads/export' + window.location.search)` → JSON (array de leads).
  3. Monta `new ExcelJS.Workbook()`, aba "Leads", com as colunas/formatação abaixo.
  4. `workbook.xlsx.writeBuffer()` → `Blob` → `<a download>` → click → revoke.
  5. Trata erro de rede/lib com alerta simples e reabilita o botão.
  - Mantém um estado de "exportando" para evitar cliques duplos (desabilita o botão).
- `templates/leads/lista.html` — botão `onclick="exportarLeads()"` com rótulo
  **"Exportar Excel"** (era "Exportar JSON"). Inclusão do `<script>` do vendor NÃO é fixa
  na página — é injetada sob demanda pelo próprio `exportarLeads()`.

## Aba "Leads" — colunas (ordem)

| # | Cabeçalho | Origem do dado | Formato |
|---|-----------|----------------|---------|
| 1 | Nome | nome | texto |
| 2 | Email | email | texto |
| 3 | Telefone | telefone | texto |
| 4 | Origem | origem | texto |
| 5 | Campanha | campanha | texto |
| 6 | Mensagem | mensagem | texto |
| 7 | Jornada | jornadaLabel | texto |
| 8 | Status | statusLabel | texto |
| 9 | Vendedor | vendedor | texto |
| 10 | Próxima ação | proximaAcao | texto |
| 11 | Próximo contato | proximoContatoEm | data dd/mm/aaaa hh:mm |
| 12 | Última interação | ultimaInteracaoEm | data dd/mm/aaaa hh:mm |
| 13 | Lido | lido → "Sim"/"Não" | texto |
| 14 | Origem externa | origemExterna | texto |
| 15 | Dados extras | dadosExtras | texto |
| 16 | Criado em | criadoEm | data dd/mm/aaaa hh:mm |
| 17 | Atualizado em | atualizadoEm | data dd/mm/aaaa hh:mm |

- Campos nulos → célula vazia. Datas: o JSON traz ISO; o JS converte em `Date` (parse local,
  sem deslocamento) e aplica `numFmt = 'dd/mm/yyyy hh:mm'`. Strings ISO inválidas/nulas → vazio.

## Formatação (ExcelJS)

- Cabeçalho (linha 1): negrito, texto branco, preenchimento sólido na cor do tema
  (usar um tom escuro coerente com o app, ex. ARGB `FF1E2A3A`); linha congelada
  (`views: [{ state: 'frozen', ySplit: 1 }]`).
- Larguras por coluna (texto largo p/ Nome/Email/Mensagem/Dados extras; médio p/ datas).
- Autofiltro no cabeçalho (`worksheet.autoFilter = { from: 'A1', to: '<ultimaColuna>1' }`).
- Nome do arquivo: `leads-AAAA-MM-DD.xlsx` (data atual do navegador).

## Tratamento de erros

- Falha no fetch ou no carregamento da lib → `alert` "Não foi possível exportar os leads."
  e botão reabilitado.
- Lista vazia → gera o arquivo só com cabeçalhos (sem linhas), sem erro.

## Testes / validação (sem deploy)

- Backend: rodar `mvn test` (perfil dev/H2) garantindo que nada quebrou; se houver teste
  do `LeadApiController`, adicionar asserção de que `/api/leads/export` retorna as novas
  chaves. Caso não haja teste de controller, validar manualmente o JSON via navegador.
- Frontend (manual): subir `mvn spring-boot:run -Dspring-boot.run.profiles=dev`, abrir
  `/leads`, aplicar filtros, clicar "Exportar Excel", abrir o `.xlsx` e conferir:
  colunas na ordem, cabeçalho formatado e congelado, autofiltro, datas formatadas,
  "Lido" como Sim/Não, e que os filtros foram respeitados. Testar lista vazia.
- Lembrar: arquivos em `static/js/` só refletem após **restart** do servidor (Maven copia
  resources no startup).

## Fora de escopo (YAGNI)

- Aba de resumo / gráficos.
- Comentários, histórico e tarefas no arquivo.
- Geração no backend (POI).
- Deploy e commit (somente após validação do usuário).

## Arquivos afetados

- Criar: `src/main/resources/static/js/vendor/exceljs.min.js` (vendor).
- Modificar: `src/main/java/com/syntra/controller/api/LeadApiController.java`
  (novo `leadExportToMap`, `/export` passa a usá-lo).
- Modificar: `src/main/resources/static/js/leads.js` (`exportarLeads()` reescrito).
- Modificar: `src/main/resources/templates/leads/lista.html` (rótulo do botão).
