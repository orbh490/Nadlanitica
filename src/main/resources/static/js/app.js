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

// ---- בועת הסבר לדף - קופצת פעם אחת בכניסה הראשונה לכל דף (נשמר ב-localStorage לפי מפתח ייחודי),
// וכפתור "?" קבוע (id="helpBtn") פותח אותה שוב בכל זמן ----
function initPageIntro(pageKey, title, bodyHtml) {
  const seenKey = 'introSeen:' + pageKey;

  function showIntro() {
    const overlay = document.createElement('div');
    overlay.className = 'intro-overlay';
    overlay.innerHTML = `
      <div class="intro-bubble">
        <h2>${title}</h2>
        <p>${bodyHtml}</p>
        <button type="button">הבנתי, בוא נמשיך</button>
      </div>`;
    document.body.appendChild(overlay);
    overlay.querySelector('button').addEventListener('click', () => {
      localStorage.setItem(seenKey, '1');
      overlay.remove();
    });
  }

  const helpBtn = document.getElementById('helpBtn');
  if (helpBtn) helpBtn.addEventListener('click', e => { e.preventDefault(); showIntro(); });

  if (localStorage.getItem(seenKey) !== '1') showIntro();
}

// ---- הופעה חלקה לכל בלוק תוצאה שמוזרק דינמית - קוראים לזה מיד אחרי שמציבים innerHTML על אלמנט תוצאה ----
function revealResult(el) {
  if (!el) return;
  el.classList.remove('reveal-in');
  void el.offsetWidth; // מאלץ reflow כדי שהאנימציה תתחיל מחדש גם אם קוראים לזה פעמיים ברצף על אותו אלמנט
  el.classList.add('reveal-in');
}

// ---- אנימציית "ספירה" למספר בודד - הופכת תוצאה כספית/אחוז לתחושה "חיה" במקום קפיצה מיידית ----
function animateNumber(el, target, opts = {}) {
  if (!el) return;
  const duration = opts.duration ?? 1100;
  const decimals = opts.decimals ?? 0;
  const prefix = opts.prefix ?? '';
  const suffix = opts.suffix ?? '';
  if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
    el.textContent = prefix + target.toLocaleString(undefined, { minimumFractionDigits: decimals, maximumFractionDigits: decimals }) + suffix;
    return;
  }
  const startTime = performance.now();
  function frame(now) {
    const progress = Math.min(1, (now - startTime) / duration);
    const eased = 1 - Math.pow(1 - progress, 3);
    const value = target * eased;
    el.textContent = prefix + value.toLocaleString(undefined, { minimumFractionDigits: decimals, maximumFractionDigits: decimals }) + suffix;
    if (progress < 1) requestAnimationFrame(frame);
  }
  requestAnimationFrame(frame);
}

// ---- הגנה מ-XSS: כל ערך שמגיע מבחוץ (משתמשים, API ממשלתי, טקסט מחוברות PDF) עובר כאן לפני שנכנס ל-innerHTML ----
function escapeHtml(value) {
  return String(value ?? '')
    .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;').replace(/'/g, '&#39;');
}

// ---- קריאות API משותפות: זורקות Error עם הודעת השגיאה בעברית מהשרת (או הודעה כללית כשאין חיבור),
// כדי שכל עמוד יציג למשתמש מה השתבש במקום להיתקע בשקט ----
async function apiFetch(url, options = {}) {
  let res;
  try {
    res = await fetch(url, options);
  } catch {
    throw new Error('לא הצלחנו להתחבר לשרת - בדקו את החיבור לאינטרנט ונסו שוב');
  }
  const data = await res.json().catch(() => null);
  if (!res.ok) throw new Error((data && data.message) || 'אירעה שגיאה - נסו שוב');
  return data;
}
function postJson(url, body, headers = {}) {
  return apiFetch(url, {
    method: 'POST', headers: { 'Content-Type': 'application/json', ...headers }, body: JSON.stringify(body)
  });
}
function errorHtml(err) {
  return `<p class="form-error">${escapeHtml(err.message)}</p>`;
}
// role="status" כדי שקוראי מסך יודיעו על ההמתנה; הטקסט תמיד קבוע בקוד (לא מגיע מבחוץ)
function loadingHtml(text) {
  return `<p class="loading-note" role="status"><span class="spinner" aria-hidden="true"></span>${text}</p>`;
}

// ---- אזהרה כשההלוואה נחתכה בתקרת המימון (הוראה 329) - משותף לדשבורד המשכנתא ולמחשבון הבנייה העצמית ----
function ltvCapNote(s) {
  if (!s.loanCappedByLtv) return '';
  const fmt = n => n.toLocaleString(undefined, { maximumFractionDigits: 0 });
  return `<p class="badge bad" style="display:block;margin-top:8px;line-height:1.6">⚠️ הבנק לא יכול לממן יותר מ-${fmt(s.maxLoanAmount)} ש"ח לפי מגבלת שיעור המימון של בנק ישראל.
    כדי לסגור את העסקה חסרים לך עוד ${fmt(s.equityShortfall)} ש"ח הון עצמי.</p>`;
}

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
