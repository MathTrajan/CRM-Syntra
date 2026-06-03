# Exportar Leads XLSX — Implementation Plan (Syntra)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Substituir a exportação JSON da lista de leads por uma exportação XLSX formatada, gerada no navegador com ExcelJS (vendorizado), respeitando os filtros ativos.

**Architecture:** O backend só amplia os dados do endpoint `/api/leads/export` (novo `leadExportToMap`, datas ISO). O frontend (`leads.js`) carrega o ExcelJS sob demanda, busca o JSON e monta a planilha. O botão da tela passa a chamar a exportação Excel.

**Tech Stack:** Spring Boot 3.3.5 · Java 21 · JUnit 5 (`mvn test`) · Thymeleaf · JS puro · ExcelJS UMD.

**Restrições:** Sistema em produção — **sem deploy e sem commit** (usuário valida local antes). Arquivos em `static/` só refletem após restart do servidor.

---

## File Structure

- Modificar: `src/main/java/com/syntra/controller/api/LeadApiController.java` — novo método `static leadExportToMap(Lead)`; `/export` passa a usá-lo.
- Criar: `src/test/java/com/syntra/controller/api/LeadExportMapTest.java` — testa o mapeamento (TDD).
- Criar: `src/main/resources/static/js/vendor/exceljs.min.js` — ExcelJS UMD vendorizado.
- Modificar: `src/main/resources/static/js/leads.js` — reescrever `exportarLeads()` + helpers.
- Modificar: `src/main/resources/templates/leads/lista.html` — rótulo do botão "Exportar JSON" → "Exportar Excel".

---

## Task 1: Backend — `leadExportToMap` (TDD)

**Files:**
- Test: `src/test/java/com/syntra/controller/api/LeadExportMapTest.java`
- Modify: `src/main/java/com/syntra/controller/api/LeadApiController.java`

- [ ] **Step 1: Escrever o teste que falha**

Antes de escrever, confirme por leitura rápida que `Usuario` tem `setNome(String)`, que `JornadaLead.TELEVENDAS` e `StatusLead.EM_ATENDIMENTO`/`StatusLead.NOVO` existem (são usados no projeto). Ajuste os nomes se diferirem.

```java
// src/test/java/com/syntra/controller/api/LeadExportMapTest.java
package com.syntra.controller.api;

import com.syntra.model.Lead;
import com.syntra.model.Usuario;
import com.syntra.model.enums.JornadaLead;
import com.syntra.model.enums.StatusLead;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LeadExportMapTest {

    @Test
    void mapeiaTodosOsCamposComDatasIso() {
        Lead lead = new Lead();
        lead.setNome("Maria");
        lead.setEmail("maria@x.com");
        lead.setTelefone("11999998888");
        lead.setOrigem("Site");
        lead.setCampanha("Black Friday");
        lead.setMensagem("Quero orçamento");
        lead.setJornada(JornadaLead.TELEVENDAS);
        lead.setStatus(StatusLead.EM_ATENDIMENTO);
        Usuario v = new Usuario();
        v.setNome("João Vendedor");
        lead.setVendedor(v);
        lead.setProximaAcao("Ligar amanhã");
        LocalDateTime dt = LocalDateTime.of(2026, 6, 3, 14, 30);
        lead.setProximoContatoEm(dt);
        lead.setUltimaInteracaoEm(dt);
        lead.setCriadoEm(dt);
        lead.setAtualizadoEm(dt);
        lead.setOrigemExterna("DIVIA");
        lead.setDadosExtras("{\"x\":1}");

        Map<String, Object> m = LeadApiController.leadExportToMap(lead);

        assertEquals("Maria", m.get("nome"));
        assertEquals("maria@x.com", m.get("email"));
        assertEquals("João Vendedor", m.get("vendedor"));
        assertEquals(StatusLead.EM_ATENDIMENTO.getLabel(), m.get("statusLabel"));
        assertEquals(JornadaLead.TELEVENDAS.getLabel(), m.get("jornadaLabel"));
        assertEquals("2026-06-03T14:30", m.get("criadoEm"));      // ISO de LocalDateTime.toString()
        assertEquals("2026-06-03T14:30", m.get("proximoContatoEm"));
        assertEquals(false, m.get("lido"));
        assertEquals("DIVIA", m.get("origemExterna"));
        assertEquals("{\"x\":1}", m.get("dadosExtras"));
    }

    @Test
    void camposNulosViramNull() {
        Lead lead = new Lead();
        lead.setNome("Sem dados");
        lead.setStatus(StatusLead.NOVO);
        lead.setProximoContatoEm(null);

        Map<String, Object> m = LeadApiController.leadExportToMap(lead);

        assertNull(m.get("jornadaLabel"));
        assertNull(m.get("vendedor"));
        assertNull(m.get("proximoContatoEm"));
        assertEquals(StatusLead.NOVO.getLabel(), m.get("statusLabel"));
    }
}
```

- [ ] **Step 2: Rodar o teste e ver falhar**

Run: `mvn -q -Dtest=LeadExportMapTest test`
Expected: FALHA de compilação — `leadExportToMap` não existe em `LeadApiController`.

- [ ] **Step 3: Implementar o método e ligar no endpoint**

Em `LeadApiController.java`, adicionar o método (package-private `static`, junto aos outros helpers de map). `Map`, `LinkedHashMap` já estão importados (usados em `leadToMap`).

```java
    // mapa completo para exportacao: dados da API/webhook + campos do sistema; datas em ISO
    static Map<String, Object> leadExportToMap(Lead lead) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("nome", lead.getNome());
        m.put("email", lead.getEmail());
        m.put("telefone", lead.getTelefone());
        m.put("origem", lead.getOrigem());
        m.put("campanha", lead.getCampanha());
        m.put("mensagem", lead.getMensagem());
        m.put("jornadaLabel", lead.getJornada() != null ? lead.getJornada().getLabel() : null);
        m.put("statusLabel", lead.getStatus().getLabel());
        m.put("vendedor", lead.getVendedor() != null ? lead.getVendedor().getNome() : null);
        m.put("proximaAcao", lead.getProximaAcao());
        m.put("proximoContatoEm", lead.getProximoContatoEm() != null ? lead.getProximoContatoEm().toString() : null);
        m.put("ultimaInteracaoEm", lead.getUltimaInteracaoEm() != null ? lead.getUltimaInteracaoEm().toString() : null);
        m.put("lido", lead.isLido());
        m.put("origemExterna", lead.getOrigemExterna());
        m.put("dadosExtras", lead.getDadosExtras());
        m.put("criadoEm", lead.getCriadoEm() != null ? lead.getCriadoEm().toString() : null);
        m.put("atualizadoEm", lead.getAtualizadoEm() != null ? lead.getAtualizadoEm().toString() : null);
        return m;
    }
```

No endpoint `exportar` (GET `/export`), trocar `.map(this::leadToMap)` por `.map(LeadApiController::leadExportToMap)`:

```java
    @GetMapping("/export")
    public ResponseEntity<List<Map<String, Object>>> exportar(LeadFiltroDTO filtro) {
        List<Map<String, Object>> lista = leadService.exportar(filtro)
                .stream()
                .map(LeadApiController::leadExportToMap)
                .toList();
        return ResponseEntity.ok(lista);
    }
```

NÃO alterar `leadToMap` nem `toMap` (usados por `bulk-assign`).

- [ ] **Step 4: Rodar o teste e ver passar**

Run: `mvn -q -Dtest=LeadExportMapTest test`
Expected: PASS (2 testes).

- [ ] **Step 5: Commit** — **PULAR.** Produção, sem commit; usuário valida e decide. (Vale para todas as tasks.)

---

## Task 2: Vendorizar o ExcelJS

**Files:**
- Create: `src/main/resources/static/js/vendor/exceljs.min.js`

- [ ] **Step 1: Baixar o build UMD do ExcelJS**

Run (no diretório `syntra`):
```bash
mkdir -p src/main/resources/static/js/vendor
curl -L -o src/main/resources/static/js/vendor/exceljs.min.js https://cdnjs.cloudflare.com/ajax/libs/exceljs/4.4.0/exceljs.min.js
```

- [ ] **Step 2: Validar o arquivo**

Run:
```bash
node -e "const s=require('fs').statSync('src/main/resources/static/js/vendor/exceljs.min.js'); console.log('bytes', s.size)"
```
Expected: tamanho > 200000 (arquivo real, ~900 KB). Se vier HTML de erro (poucos bytes / contém `<html`), tentar o fallback unpkg:
```bash
curl -L -o src/main/resources/static/js/vendor/exceljs.min.js https://unpkg.com/exceljs@4.4.0/dist/exceljs.min.js
```
Confirmar que o conteúdo expõe o global ExcelJS (procurar a string `ExcelJS` no arquivo):
```bash
node -e "const t=require('fs').readFileSync('src/main/resources/static/js/vendor/exceljs.min.js','utf8'); console.log('temExcelJS', t.includes('ExcelJS'))"
```
Expected: `temExcelJS true`.

- [ ] **Step 3: Commit** — PULAR.

---

## Task 3: Frontend — reescrever `exportarLeads()` em `leads.js`

**Files:**
- Modify: `src/main/resources/static/js/leads.js`

> Sem teste automatizado: o projeto não tem harness de testes JS; validação é manual (Task 5). A lógica de dados (backend) já é testada na Task 1.

- [ ] **Step 1: Substituir a função `exportarLeads()` existente**

Localizar a função atual (baixa JSON) e substituí-la INTEIRA por este bloco (mantém o nome `exportarLeads`, agora `async`, + helpers):

```javascript
let _exceljsPromise = null;

// carrega o ExcelJS vendorizado sob demanda, uma unica vez
function carregarExcelJS() {
  if (window.ExcelJS) return Promise.resolve(window.ExcelJS);
  if (_exceljsPromise) return _exceljsPromise;
  _exceljsPromise = new Promise((resolve, reject) => {
    const s = document.createElement('script');
    s.src = '/js/vendor/exceljs.min.js';
    s.onload = () => resolve(window.ExcelJS);
    s.onerror = () => reject(new Error('Falha ao carregar ExcelJS'));
    document.head.appendChild(s);
  });
  return _exceljsPromise;
}

// converte ISO (LocalDateTime.toString) em Date local; null/invalido -> null
function parseDataIso(iso) {
  if (!iso) return null;
  const d = new Date(iso);
  return isNaN(d.getTime()) ? null : d;
}

// AAAA-MM-DD da data atual (para o nome do arquivo)
function nomeDataHoje() {
  const d = new Date();
  const p = n => String(n).padStart(2, '0');
  return d.getFullYear() + '-' + p(d.getMonth() + 1) + '-' + p(d.getDate());
}

async function exportarLeads() {
  const botao = document.querySelector('button[onclick="exportarLeads()"]');
  if (botao) botao.disabled = true;
  try {
    const ExcelJSLib = await carregarExcelJS();

    const url = new URL('/api/leads/export', window.location.origin);
    url.search = window.location.search; // respeita filtros ativos
    const resp = await fetch(url.toString());
    if (!resp.ok) throw new Error('Erro ao exportar leads');
    const leads = await resp.json();

    const DATA = 'dd/mm/yyyy hh:mm';
    const wb = new ExcelJSLib.Workbook();
    const ws = wb.addWorksheet('Leads');
    ws.columns = [
      { header: 'Nome', key: 'nome', width: 28 },
      { header: 'Email', key: 'email', width: 28 },
      { header: 'Telefone', key: 'telefone', width: 18 },
      { header: 'Origem', key: 'origem', width: 16 },
      { header: 'Campanha', key: 'campanha', width: 18 },
      { header: 'Mensagem', key: 'mensagem', width: 40 },
      { header: 'Jornada', key: 'jornada', width: 18 },
      { header: 'Status', key: 'status', width: 18 },
      { header: 'Vendedor', key: 'vendedor', width: 22 },
      { header: 'Próxima ação', key: 'proximaAcao', width: 24 },
      { header: 'Próximo contato', key: 'proximoContato', width: 18, style: { numFmt: DATA } },
      { header: 'Última interação', key: 'ultimaInteracao', width: 18, style: { numFmt: DATA } },
      { header: 'Lido', key: 'lido', width: 8 },
      { header: 'Origem externa', key: 'origemExterna', width: 16 },
      { header: 'Dados extras', key: 'dadosExtras', width: 30 },
      { header: 'Criado em', key: 'criadoEm', width: 18, style: { numFmt: DATA } },
      { header: 'Atualizado em', key: 'atualizadoEm', width: 18, style: { numFmt: DATA } },
    ];

    leads.forEach(l => {
      ws.addRow({
        nome: l.nome || '',
        email: l.email || '',
        telefone: l.telefone || '',
        origem: l.origem || '',
        campanha: l.campanha || '',
        mensagem: l.mensagem || '',
        jornada: l.jornadaLabel || '',
        status: l.statusLabel || '',
        vendedor: l.vendedor || '',
        proximaAcao: l.proximaAcao || '',
        proximoContato: parseDataIso(l.proximoContatoEm),
        ultimaInteracao: parseDataIso(l.ultimaInteracaoEm),
        lido: l.lido ? 'Sim' : 'Não',
        origemExterna: l.origemExterna || '',
        dadosExtras: l.dadosExtras || '',
        criadoEm: parseDataIso(l.criadoEm),
        atualizadoEm: parseDataIso(l.atualizadoEm),
      });
    });

    // cabecalho: negrito, texto branco, fundo escuro do tema; congela a primeira linha
    const header = ws.getRow(1);
    header.font = { bold: true, color: { argb: 'FFFFFFFF' } };
    header.eachCell(c => {
      c.fill = { type: 'pattern', pattern: 'solid', fgColor: { argb: 'FF1E2A3A' } };
    });
    ws.views = [{ state: 'frozen', ySplit: 1 }];
    ws.autoFilter = { from: 'A1', to: 'Q1' }; // 17 colunas (A..Q)

    const buffer = await wb.xlsx.writeBuffer();
    const blob = new Blob([buffer], {
      type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
    });
    const link = document.createElement('a');
    link.href = URL.createObjectURL(blob);
    link.download = 'leads-' + nomeDataHoje() + '.xlsx';
    document.body.appendChild(link);
    link.click();
    link.remove();
    URL.revokeObjectURL(link.href);
  } catch (e) {
    alert('Não foi possível exportar os leads.');
  } finally {
    if (botao) botao.disabled = false;
  }
}
```

- [ ] **Step 2: Conferir que não restou código antigo da exportação JSON**

Garantir que a antiga implementação (que baixava `leads-export.json` via `JSON.stringify`) foi totalmente removida e que o nome `exportarLeads` aparece uma única vez.

Run: `grep -n "leads-export.json\|function exportarLeads\|JSON.stringify" src/main/resources/static/js/leads.js`
Expected: nenhuma ocorrência de `leads-export.json` nem `JSON.stringify`; `exportarLeads` aparece 1 vez.

- [ ] **Step 3: Commit** — PULAR.

---

## Task 4: Template — rótulo do botão

**Files:**
- Modify: `src/main/resources/templates/leads/lista.html`

- [ ] **Step 1: Renomear o botão**

Trocar o texto do botão de exportação (linha ~217-219):

De:
```html
                    <button type="button" class="btn btn-secondary btn-sm" onclick="exportarLeads()">
                        Exportar JSON
                    </button>
```
Para:
```html
                    <button type="button" class="btn btn-secondary btn-sm" onclick="exportarLeads()">
                        Exportar Excel
                    </button>
```

- [ ] **Step 2: Confirmar**

Run: `grep -n "Exportar Excel\|Exportar JSON" src/main/resources/templates/leads/lista.html`
Expected: aparece `Exportar Excel`; não aparece `Exportar JSON`.

- [ ] **Step 3: Commit** — PULAR.

---

## Task 5: Build + validação manual (sem deploy)

**Files:** nenhum (verificação).

- [ ] **Step 1: Build/test completo do backend**

Run: `mvn -q test`
Expected: BUILD SUCCESS; `LeadExportMapTest` e os testes existentes passam.

- [ ] **Step 2: Subir o servidor dev**

Run: `mvn spring-boot:run -Dspring-boot.run.profiles=dev`
(Servidor em `http://localhost:8080`. Lembrar: mudanças em `static/` exigem restart — aqui é o primeiro start, ok.)

- [ ] **Step 3: Validar no navegador**

1. Login (credenciais dev seedadas por `DataInitializer`).
2. Abrir `/leads`, aplicar alguns filtros (status, vendedor, período, busca).
3. Clicar **Exportar Excel** e abrir o `.xlsx` baixado (`leads-AAAA-MM-DD.xlsx`).
4. Conferir: 17 colunas na ordem do spec; cabeçalho negrito/escuro congelado; autofiltro;
   datas formatadas `dd/mm/aaaa hh:mm`; coluna "Lido" como Sim/Não; linhas refletem o filtro.
5. Testar com filtro que não retorna leads → arquivo só com cabeçalhos, sem erro.
6. (Opcional) Conferir no DevTools que `/js/vendor/exceljs.min.js` só é requisitado ao
   clicar em Exportar (lazy-load), não no load da página.

- [ ] **Step 4: Commit** — PULAR (usuário valida e decide).

---

## Self-Review

- **Cobertura do spec:** dados completos no endpoint (T1), datas ISO (T1), vendor ExcelJS (T2), lazy-load + workbook formatado + 17 colunas + autofiltro + freeze + nome de arquivo (T3), botão renomeado (T4), validação respeitando filtros e lista vazia (T5). Tudo coberto.
- **Placeholders:** nenhum — todo passo de código mostra o código.
- **Consistência:** chaves do `leadExportToMap` (T1) batem 1:1 com as lidas no `leads.js` (T3): nome, email, telefone, origem, campanha, mensagem, jornadaLabel, statusLabel, vendedor, proximaAcao, proximoContatoEm, ultimaInteracaoEm, lido, origemExterna, dadosExtras, criadoEm, atualizadoEm. 17 colunas → range autofiltro A1:Q1. Botão usa `onclick="exportarLeads()"`, selecionado por `button[onclick="exportarLeads()"]`.
- **Sem commit/deploy:** todos os passos de commit marcados como PULAR.
