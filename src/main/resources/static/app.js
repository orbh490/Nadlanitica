// עיצוב משותף: פורמט אלפים בשדות כספיים + טולטיפים להסברים

function numericValue(input) {
  return Number(String(input.value).replace(/,/g, '')) || 0;
}

function attachThousandsFormatting(input) {
  input.addEventListener('input', () => {
    const cursorFromEnd = input.value.length - input.selectionStart;
    const digitsOnly = input.value.replace(/[^\d]/g, '');
    const formatted = digitsOnly === '' ? '' : Number(digitsOnly).toLocaleString('en-US');
    input.value = formatted;
    const newPos = Math.max(0, formatted.length - cursorFromEnd);
    input.setSelectionRange(newPos, newPos);
  });
}

function initMoneyInputs() {
  document.querySelectorAll('.money-input').forEach(input => {
    if (input.value) {
      input.value = Number(String(input.value).replace(/,/g, '')).toLocaleString('en-US');
    }
    attachThousandsFormatting(input);
  });
}

document.addEventListener('DOMContentLoaded', initMoneyInputs);

// ---- התחברות משותפת: טוקן ב-localStorage (נשאר בין ביקורים, כמו כל "זכור אותי"),
// כל בקשה מאומתת שולחת אותו כ-Authorization: Bearer <token> ----
const AUTH_TOKEN_KEY = 'financialCompassAuthToken';

function getAuthToken() {
  return localStorage.getItem(AUTH_TOKEN_KEY);
}
function setAuthToken(token) {
  localStorage.setItem(AUTH_TOKEN_KEY, token);
}
function clearAuthToken() {
  localStorage.removeItem(AUTH_TOKEN_KEY);
}
function authHeaders() {
  const token = getAuthToken();
  return token ? { 'Authorization': 'Bearer ' + token } : {};
}
async function fetchCurrentUser() {
  const token = getAuthToken();
  if (!token) return { loggedIn: false, email: null };
  try {
    return await (await fetch('/api/auth/me', { headers: authHeaders() })).json();
  } catch {
    return { loggedIn: false, email: null };
  }
}

// ---- בקשת התחברות/הרשמה משותפת - נקודת הכניסה (index.html) ו"האזור האישי" (account.html)
// שתיהן משתמשות באותה לוגיקה, כל אחת מחליטה בעצמה מה קורה אחרי הצלחה (redirect מול רענון במקום) ----
async function authRequest(url, email, password, consent) {
  try {
    const res = await fetch(url, {
      method: 'POST', headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email, password, consent: !!consent })
    });
    const data = await res.json().catch(() => ({}));
    if (!res.ok) {
      return { ok: false, status: res.status, error: data.message || 'שגיאה - בדקו את הפרטים ונסו שוב' };
    }
    return { ok: true, data };
  } catch (e) {
    return { ok: false, status: 0, error: 'לא הצלחנו להתחבר כרגע.' };
  }
}
async function doLogout() {
  await fetch('/api/auth/logout', { method: 'POST', headers: authHeaders() });
  clearAuthToken();
}
