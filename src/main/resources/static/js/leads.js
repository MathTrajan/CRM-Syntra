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

function exportarLeads() {
  const url = new URL('/api/leads/export', window.location.origin);
  url.search = window.location.search;

  fetch(url.toString())
    .then(response => {
      if (!response.ok) {
        throw new Error('Erro ao exportar leads');
      }
      return response.json();
    })
    .then(data => {
      const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json;charset=utf-8' });
      const link = document.createElement('a');
      link.href = URL.createObjectURL(blob);
      link.download = 'leads-export.json';
      document.body.appendChild(link);
      link.click();
      link.remove();
    })
    .catch(error => {
      alert(error.message || 'Falha ao exportar leads.');
    });
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

document.addEventListener('DOMContentLoaded', () => {
  setInterval(verificarNovosLeads, 60000);
  updateBulkActions();
});
