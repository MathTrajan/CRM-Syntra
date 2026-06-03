// Polling que recarrega a lista de leads a cada 60s se houver novos
let ultimoTotal = null;

function verificarNovosLeads() {
  fetch('/api/leads/nao-lidos')
    .then(r => r.ok ? r.json() : null)
    .then(data => {
      if (!data) return;
      if (ultimoTotal !== null && data.total > ultimoTotal) {
        // Há novos leads — recarrega página suavemente
        location.reload();
      }
      ultimoTotal = data.total;
    })
    .catch(() => {});
}

function toggleSelectAll(source) {
  const checkboxes = document.querySelectorAll('.select-lead');
  checkboxes.forEach(cb => cb.checked = source.checked);
  updateBulkActions();
}

function updateBulkActions() {
  const selecionados = document.querySelectorAll('.select-lead:checked').length;
  const bulkButton = document.querySelector('button[onclick="atribuirSelecionados()"]');
  if (bulkButton) {
    bulkButton.disabled = selecionados === 0;
    bulkButton.textContent = selecionados > 0
      ? `Atribuir ${selecionados} selecionado(s)`
      : 'Atribuir selecionados';
  }
}

function getCsrfToken() {
  return document.getElementById('csrfToken')?.value || '';
}

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

function atribuirSelecionados() {
  const selected = [...document.querySelectorAll('.select-lead:checked')].map(cb => cb.value);
  if (selected.length === 0) {
    alert('Selecione ao menos um lead para atribuir.');
    return;
  }

  const vendedorId = document.getElementById('bulkVendedorSelect').value;
  if (!vendedorId) {
    if (!confirm('Deseja remover o vendedor de todos os leads selecionados?')) {
      return;
    }
  }

  fetch('/api/leads/bulk-assign', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'X-CSRF-TOKEN': getCsrfToken()
    },
    body: JSON.stringify({ leadIds: selected, vendedorId })
  })
    .then(response => {
      if (!response.ok) {
        return response.text().then(text => { throw new Error(text || 'Erro ao atribuir leads.'); });
      }
      return response.json();
    })
    .then(() => location.reload())
    .catch(error => alert(error.message || 'Falha ao atribuir leads.'));
}

// Linha inteira clicavel: abre o detalhe do lead. Ignora cliques em controles
// interativos (input, label, a, button) para nao roubar foco do checkbox/whatsapp.
function bindRowClick() {
  document.querySelectorAll('tr.row-clickable[data-lead-url]').forEach(row => {
    row.addEventListener('click', (e) => {
      if (e.target.closest('a, button, input, label, .select-lead')) return;
      if (window.getSelection && String(window.getSelection())) return; // permitir selecionar texto
      const url = row.dataset.leadUrl;
      if (e.ctrlKey || e.metaKey || e.button === 1) {
        window.open(url, '_blank');
      } else {
        window.location = url;
      }
    });
  });
}

document.addEventListener('DOMContentLoaded', () => {
  setInterval(verificarNovosLeads, 60000);
  updateBulkActions();
  bindRowClick();
});
