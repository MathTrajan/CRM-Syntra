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

// Polling: verifica leads não lidos a cada 30s
function iniciarPolling() {
  atualizarNaoLidos();
  setInterval(atualizarNaoLidos, 30000);
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

document.addEventListener('DOMContentLoaded', () => {
  iniciarPolling();
  definirInicialAvatar();
});
