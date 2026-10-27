async function refreshSettingsUI() {
  const me = await fetchCurrentUser();
  document.getElementById('loggedOutView').style.display = me.loggedIn ? 'none' : 'block';
  document.getElementById('loggedInView').style.display = me.loggedIn ? 'block' : 'none';
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
  msgEl.style.color = '#6B8193';
  msgEl.textContent = 'רגע...';
  const res = await fetch('/api/auth/forgot-password', {
    method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ email })
  });
  const data = await res.json();
  msgEl.style.color = '#12805A';
  msgEl.textContent = data.message;
}

function clearLocalData() {
  clearAuthToken();
  const msgEl = document.getElementById('clearMessage');
  msgEl.style.color = '#12805A';
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
