// Sidebar mobile
function toggleSidebar() {
  const s = document.getElementById('sidebar');
  const o = document.getElementById('sidebarOverlay');
  if (s) s.classList.toggle('open');
  if (o) o.classList.toggle('visible');
}

function closeSidebar() {
  const s = document.getElementById('sidebar');
  const o = document.getElementById('sidebarOverlay');
  if (s) s.classList.remove('open');
  if (o) o.classList.remove('visible');
}

// Polling: verifica leads não lidos e alertas a cada 30s
function iniciarPolling() {
  atualizarNaoLidos();
  atualizarAlertas();
  setInterval(() => {
    atualizarNaoLidos();
    atualizarAlertas();
  }, 30000);
}

function atualizarNaoLidos() {
  fetch('/api/leads/nao-lidos')
    .then(r => r.ok ? r.json() : null)
    .then(data => {
      if (!data) return;
      const el = document.getElementById('sidebar-nao-lidos');
      if (el) {
        if (data.total > 0) {
          el.textContent = data.total;
          el.style.display = 'inline';
        } else {
          el.style.display = 'none';
        }
      }
      const upd = document.getElementById('lastUpdate');
      if (upd) {
        const now = new Date();
        upd.textContent = 'Atualizado ' + now.toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' });
      }
    })
    .catch(() => {});
}

// Define inicial do avatar a partir do nome do usuário (evita email completo no avatar)
function definirInicialAvatar() {
  const nameEl  = document.querySelector('.user-name');
  const avatar  = document.getElementById('userAvatar');
  if (nameEl && avatar) {
    avatar.textContent = nameEl.textContent.trim().charAt(0).toUpperCase();
  }
}

// ── Sino de notificações (alertas do vendedor logado) ──────────────────────

// Abre/fecha o dropdown do sino
function toggleAlertas(ev) {
  if (ev) ev.stopPropagation();
  const dd = document.getElementById('alertasDropdown');
  if (!dd) return;
  dd.style.display = dd.style.display === 'none' ? 'block' : 'none';
}

// Fecha o dropdown ao clicar fora do componente
function fecharAlertasFora(ev) {
  const wrap = document.querySelector('.notif-wrap');
  const dd = document.getElementById('alertasDropdown');
  if (dd && wrap && !wrap.contains(ev.target)) dd.style.display = 'none';
}

// Busca /api/alertas e atualiza badge + lista do dropdown
function atualizarAlertas() {
  fetch('/api/alertas')
    .then(r => r.ok ? r.json() : null)
    .then(data => {
      if (!data) return;

      // Atualiza o badge numérico sobre o sino
      const badge = document.getElementById('notifBadge');
      if (badge) {
        if (data.total > 0) {
          badge.textContent = data.total;
          badge.style.display = 'inline-flex';
        } else {
          badge.style.display = 'none';
        }
      }

      // Renderiza os itens no dropdown
      const lista = document.getElementById('alertasLista');
      if (lista) {
        if (!data.alertas || data.alertas.length === 0) {
          lista.innerHTML = '<div class="notif-empty">Nenhum lead precisa de atenção agora.</div>';
        } else {
          lista.innerHTML = data.alertas.map(a => {
            const urg = a.severidade === 'URGENTE';
            const cls    = urg ? 'badge-perdido' : 'badge-em-atendimento';
            const rotulo = urg ? 'Urgente' : 'Atenção';
            return '<a class="notif-item" href="/leads/' + a.leadId + '">'
                 + '<span class="badge ' + cls + '">' + rotulo + '</span>'
                 + '<span class="notif-msg">' + a.mensagem + '</span>'
                 + '<span class="notif-lead">' + a.leadNome + '</span>'
                 + '</a>';
          }).join('');
        }
      }
    })
    .catch(() => {});
}

document.addEventListener('DOMContentLoaded', () => {
  iniciarPolling();
  definirInicialAvatar();
  // Fecha o dropdown do sino ao clicar fora dele
  document.addEventListener('click', fecharAlertasFora);
});
