function getCsrf() {
  return document.getElementById('csrfToken')?.value ?? '';
}

function parseJsonOrThrow(response) {
  if (response.ok) {
    return response.json();
  }

  return response
    .json()
    .catch(() => ({ erro: 'Erro inesperado.' }))
    .then(err => Promise.reject(err));
}

function salvarAtualizacao() {
  const id = document.getElementById('leadId').value;
  const jornada = document.getElementById('jornadaSelect').value || null;
  const status = document.getElementById('statusSelect').value;
  const vendedorId = document.getElementById('vendedorSelect').value;
  const btn = document.getElementById('btnSalvar');
  const feedback = document.getElementById('saveFeedback');

  btn.disabled = true;
  btn.textContent = 'Salvando...';

  fetch(`/api/leads/${id}`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json', 'X-CSRF-TOKEN': getCsrf() },
    body: JSON.stringify({ jornada, status, vendedorId })
  })
    .then(parseJsonOrThrow)
    .then(data => {
      document.querySelectorAll('.badge-lg').forEach(b => {
        b.textContent = data.statusLabel;
        b.className = `badge badge-lg badge-${data.status.toLowerCase().replace(/_/g, '-')}`;
      });
      feedback.textContent = 'Salvo com sucesso';
      feedback.style.color = 'var(--green)';
      feedback.classList.add('visible');
      setTimeout(() => feedback.classList.remove('visible'), 2500);
    })
    .catch(err => {
      feedback.textContent = err?.erro || 'Erro ao salvar';
      feedback.style.color = 'var(--red)';
      feedback.classList.add('visible');
      setTimeout(() => feedback.classList.remove('visible'), 3000);
    })
    .finally(() => {
      btn.disabled = false;
      btn.innerHTML = `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" width="15" height="15"><polyline points="20 6 9 17 4 12"/></svg> Salvar Alterações`;
    });
}

function adicionarComentario() {
  const id = document.getElementById('leadId').value;
  const textarea = document.getElementById('novoComentario');
  const texto = textarea.value.trim();
  const btn = document.getElementById('btnComentario');

  if (!texto) return;

  btn.disabled = true;
  btn.textContent = 'Enviando...';

  fetch(`/api/leads/${id}/comentarios`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', 'X-CSRF-TOKEN': getCsrf() },
    body: JSON.stringify({ texto })
  })
    .then(parseJsonOrThrow)
    .then(c => {
      const lista = document.getElementById('comentariosList');
      const vazio = document.getElementById('semComentarios');
      if (vazio) vazio.remove();

      const item = document.createElement('div');
      item.className = 'comment-item slide-in';
      item.innerHTML = `
        <div class="comment-header">
          <span class="comment-author">${escapeHtml(c.autor)}</span>
          <span class="comment-date">${c.criadoEm}</span>
        </div>
        <p class="comment-text">${escapeHtml(c.texto)}</p>
      `;
      lista.insertBefore(item, lista.firstChild);
      prependTimelineComment(c);
      textarea.value = '';
    })
    .catch(err => alert(err?.erro || 'Erro ao adicionar anotação.'))
    .finally(() => {
      btn.disabled = false;
      btn.innerHTML = `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" width="15" height="15"><line x1="22" y1="2" x2="11" y2="13"/><polygon points="22 2 15 22 11 13 2 9 22 2"/></svg> Adicionar`;
    });
}

function escapeHtml(str) {
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');
}

function prependTimelineComment(comentario) {
  const timeline = document.getElementById('historicoLista');
  if (!timeline) return;

  const emptyMessage = timeline.querySelector('.text-muted.text-sm');
  if (emptyMessage) emptyMessage.remove();

  const item = document.createElement('div');
  item.className = 'history-item history-item-rich';
  item.innerHTML = `
    <div class="history-dot"></div>
    <div class="history-body">
      <div class="timeline-topline">
        <span class="timeline-badge">Comentario</span>
        <span class="history-date">${escapeHtml(comentario.criadoEm)}</span>
      </div>
      <p class="history-desc">Comentario interno</p>
      <p class="timeline-description">${escapeHtml(comentario.texto)}</p>
      <div class="timeline-meta">
        <span class="history-author">${escapeHtml(comentario.autor)}</span>
        <span class="timeline-origin">PAINEL</span>
      </div>
    </div>
  `;
  timeline.insertBefore(item, timeline.firstChild);
}

document.addEventListener('DOMContentLoaded', () => {
  const id = document.getElementById('leadId')?.value;
  if (id) {
    fetch(`/api/leads/${id}/lido`, {
      method: 'POST',
      headers: { 'X-CSRF-TOKEN': getCsrf() }
    }).catch(() => {});
  }
});
