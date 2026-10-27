async function refreshSettingsUI() {
  const me = await fetchCurrentUser();
  document.getElementById('loggedOutView').style.display = me.loggedIn ? 'none' : 'block';
  document.getElementById('loggedInView').style.display = me.loggedIn ? 'block' : 'none';
  document.getElementById('deleteAccountSection').style.display = me.loggedIn ? 'block' : 'none';
  if (!me.loggedIn) return;
  document.getElementById('settingsEmail').textContent = me.email;
}

async function handleLogout() {
  await doLogout();
  await refreshSettingsUI();
}

async function sendPasswordReset() {
  const email = document.getElementById('settingsEmail').textContent;
  const msgEl = document.getElementById('settingsMessage');
  msgEl.style.color = 'var(--ink-muted)';
  msgEl.textContent = 'רגע...';
  const res = await fetch('/api/auth/forgot-password', {
    method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ email })
  });
  const data = await res.json();
  msgEl.style.color = 'var(--good-text)';
  msgEl.textContent = data.message;
}

async function deleteAccount() {
  const password = document.getElementById('deleteAccountPassword').value;
  const msgEl = document.getElementById('deleteAccountMessage');
  if (!password) {
    msgEl.innerHTML = '<p class="form-error">יש להזין את הסיסמה כדי למחוק את החשבון</p>';
    return;
  }
  if (!confirm('למחוק לצמיתות את החשבון וכל המידע השמור בו? אי אפשר לבטל את זה.')) return;
  try {
    const data = await postJson('/api/auth/delete-account', { password }, authHeaders());
    clearAuthToken();
    msgEl.style.color = 'var(--good-text)';
    msgEl.textContent = data.message;
    setTimeout(() => location.reload(), 1500);
  } catch (err) {
    msgEl.innerHTML = errorHtml(err);
  }
}

function clearLocalData() {
  clearAuthToken();
  const msgEl = document.getElementById('clearMessage');
  msgEl.style.color = 'var(--good-text)';
  msgEl.textContent = 'נוקה. מרעננים...';
  setTimeout(() => location.reload(), 600);
}

const savedPrefs = displayPrefs.get();
document.getElementById('prefDark').checked = !!savedPrefs.dark;
document.getElementById('prefLargeText').checked = !!savedPrefs.largeText;
document.getElementById('prefDark').addEventListener('change', e => displayPrefs.set('dark', e.target.checked));
document.getElementById('prefLargeText').addEventListener('change', e => displayPrefs.set('largeText', e.target.checked));

refreshSettingsUI();
initPageIntro('settings', 'הגדרות',
  'כאן אפשר לנהל את החשבון שלך, לשנות סיסמה, ולנקות נתונים שמורים במכשיר הזה. ההגדרות הכלליות זמינות לכולם, עם או בלי חשבון.');
