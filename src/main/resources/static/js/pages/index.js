async function handleLogin() {
  if (!validateAuthForm(false)) return;
  const result = await submit('/api/auth/login', false);
  if (result) {
    setAuthToken(result.data.token);
    window.location.href = '/home.html';
  }
}

async function handleRegister() {
  if (!validateAuthForm(true)) return;
  const result = await submit('/api/auth/register', true);
  if (result) {
    const msgEl = document.getElementById('authMessage');
    msgEl.style.color = 'var(--good-text)';
    msgEl.textContent = result.data.message;
  }
}

async function submit(url, consent) {
  const email = document.getElementById('authEmail').value.trim();
  const password = document.getElementById('authPassword').value;
  const msgEl = document.getElementById('authMessage');
  const resendLink = document.getElementById('resendVerificationLink');
  msgEl.style.color = 'var(--ink-muted)';
  msgEl.textContent = 'רגע...';
  resendLink.style.display = 'none';
  const result = await authRequest(url, email, password, consent);
  if (!result.ok) {
    msgEl.style.color = 'var(--bad-text)';
    msgEl.textContent = result.error;
    resendLink.style.display = result.status === 403 ? 'inline' : 'none';
    return null;
  }
  return result;
}

function toggleForgotPassword() {
  const box = document.getElementById('forgotPasswordBox');
  const showing = box.style.display !== 'none';
  box.style.display = showing ? 'none' : 'block';
  if (!showing) document.getElementById('forgotEmail').value = document.getElementById('authEmail').value;
}

async function submitForgotPassword() {
  if (!validateFields([['forgotEmail', check.email('כתובת אימייל לא תקינה')]])) return;
  const email = document.getElementById('forgotEmail').value.trim();
  const msgEl = document.getElementById('authMessage');
  msgEl.style.color = 'var(--ink-muted)';
  msgEl.textContent = 'רגע...';
  const res = await fetch('/api/auth/forgot-password', {
    method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ email })
  });
  const data = await res.json();
  msgEl.style.color = 'var(--good-text)';
  msgEl.textContent = data.message;
  document.getElementById('forgotPasswordBox').style.display = 'none';
}

async function resendVerification() {
  const email = document.getElementById('authEmail').value.trim();
  await fetch('/api/auth/resend-verification', {
    method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ email })
  });
  const msgEl = document.getElementById('authMessage');
  msgEl.style.color = 'var(--good-text)';
  msgEl.textContent = 'אם הכתובת רשומה ועדיין לא אומתה, נשלח אליה מייל אימות חדש';
  document.getElementById('resendVerificationLink').style.display = 'none';
}

function continueAsGuest() {
  window.location.href = '/home.html';
}

(async () => {
  const me = await fetchCurrentUser();
  if (me.loggedIn) window.location.href = '/home.html';
})();
