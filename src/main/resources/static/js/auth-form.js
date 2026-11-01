// טופס התחברות/הרשמה (fragments/auth.html) - משותף לעמוד הכניסה ולאזור האישי.
// כל עמוד מגדיר בעצמו onLoginSuccess() - מה קורה אחרי התחברות (מעבר לדף הבית מול רענון האזור האישי).

const AUTH_MODES = {
  login: {
    title: 'התחברות לחשבון',
    intro: 'ברוכים השבים! התחברו כדי לראות את החישובים השמורים שלכם.',
    passwordLabel: 'סיסמה',
    passwordAutocomplete: 'current-password',
    submit: 'התחברות'
  },
  register: {
    title: 'יצירת חשבון חדש',
    intro: 'החשבון חינמי: הוא שומר את החישובים שלכם ומאפשר להשוות בין מסלולים. אחרי ההרשמה נשלח אליכם מייל לאימות הכתובת.',
    passwordLabel: 'בחרו סיסמה (לפחות 6 תווים)',
    passwordAutocomplete: 'new-password',
    submit: 'יצירת חשבון'
  }
};
let authMode = 'login';

function setAuthMode(mode) {
  authMode = mode === 'register' ? 'register' : 'login';
  const m = AUTH_MODES[authMode], isRegister = authMode === 'register';
  document.getElementById('authTitle').textContent = m.title;
  document.getElementById('authIntro').textContent = m.intro;
  document.getElementById('authPasswordLabel').textContent = m.passwordLabel;
  document.getElementById('authPassword').setAttribute('autocomplete', m.passwordAutocomplete);
  document.getElementById('authSubmit').textContent = m.submit;
  document.getElementById('consentRow').style.display = isRegister ? '' : 'none';
  document.getElementById('forgotRow').style.display = isRegister ? 'none' : '';
  document.getElementById('switchToRegister').style.display = isRegister ? 'none' : '';
  document.getElementById('switchToLogin').style.display = isRegister ? '' : 'none';
  [['authTabLogin', !isRegister], ['authTabRegister', isRegister]].forEach(([id, on]) => {
    const tab = document.getElementById(id);
    tab.classList.toggle('active', on);
    tab.setAttribute('aria-selected', String(on));
  });
  // הודעות ושגיאות מהמצב הקודם לא רלוונטיות למצב החדש
  document.getElementById('forgotPasswordBox').style.display = 'none';
  document.getElementById('authMessage').textContent = '';
  document.getElementById('resendVerificationLink').style.display = 'none';
  ['authEmail', 'authPassword', 'consentCheckbox'].forEach(id => clearFieldError(document.getElementById(id)));
}

async function submitAuth() {
  const isRegister = authMode === 'register';
  if (!validateAuthForm(isRegister)) return;
  const result = await sendAuth(isRegister ? '/api/auth/register' : '/api/auth/login', isRegister);
  if (!result) return;
  if (isRegister) {
    showAuthMessage(result.data.message, 'var(--good-text)');
    document.getElementById('authPassword').value = '';
  } else {
    setAuthToken(result.data.token);
    document.getElementById('authPassword').value = '';
    await onLoginSuccess();
  }
}

function showAuthMessage(text, color) {
  const msgEl = document.getElementById('authMessage');
  msgEl.style.color = color;
  msgEl.textContent = text;
}

async function sendAuth(url, consent) {
  const email = document.getElementById('authEmail').value.trim();
  const password = document.getElementById('authPassword').value;
  const resendLink = document.getElementById('resendVerificationLink');
  showAuthMessage('רגע...', 'var(--ink-muted)');
  resendLink.style.display = 'none';
  const result = await authRequest(url, email, password, consent);
  if (!result.ok) {
    showAuthMessage(result.error, 'var(--bad-text)');
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
  showAuthMessage('רגע...', 'var(--ink-muted)');
  const res = await fetch('/api/auth/forgot-password', {
    method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ email })
  });
  const data = await res.json();
  showAuthMessage(data.message, 'var(--good-text)');
  document.getElementById('forgotPasswordBox').style.display = 'none';
}

async function resendVerification() {
  const email = document.getElementById('authEmail').value.trim();
  await fetch('/api/auth/resend-verification', {
    method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ email })
  });
  showAuthMessage('אם הכתובת רשומה ועדיין לא אומתה, נשלח אליה מייל אימות חדש', 'var(--good-text)');
  document.getElementById('resendVerificationLink').style.display = 'none';
}

// Enter בשדות שולח את הטופס במצב הנוכחי
['authEmail', 'authPassword'].forEach(id => document.getElementById(id).addEventListener('keydown', e => {
  if (e.key === 'Enter') submitAuth();
}));

// קישור ישיר למצב הרשמה: index.html#register
setAuthMode(location.hash === '#register' ? 'register' : 'login');
