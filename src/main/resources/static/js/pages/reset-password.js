const token = new URLSearchParams(window.location.search).get('token');
if (!token) {
  document.getElementById('resultMessage').style.color = 'var(--bad-text)';
  document.getElementById('resultMessage').textContent = 'קישור לא תקין - יש לבקש קישור איפוס חדש ממסך ההתחברות';
}

async function submitReset() {
  const newPassword = document.getElementById('newPassword').value;
  const msgEl = document.getElementById('resultMessage');
  msgEl.style.color = 'var(--ink-muted)';
  msgEl.textContent = 'רגע...';
  const res = await fetch('/api/auth/reset-password', {
    method: 'POST', headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ token, newPassword })
  });
  const data = await res.json().catch(() => ({}));
  if (!res.ok) {
    msgEl.style.color = 'var(--bad-text)';
    msgEl.textContent = data.message || 'שגיאה - נסו שוב';
    return;
  }
  setAuthToken(data.token);
  window.location.href = '/home.html';
}
