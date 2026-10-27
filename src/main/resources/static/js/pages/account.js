async function handleLogin() {
  const result = await submit('/api/auth/login', false);
  if (result) {
    setAuthToken(result.data.token);
    document.getElementById('authEmail').value = '';
    document.getElementById('authPassword').value = '';
    await refreshAccountUI();
  }
}

async function handleRegister() {
  if (!document.getElementById('consentCheckbox').checked) {
    const msgEl = document.getElementById('authMessage');
    msgEl.style.color = '#B3261E';
    msgEl.textContent = 'יש לאשר את מדיניות הפרטיות כדי להירשם';
    return;
  }
  const result = await submit('/api/auth/register', true);
  if (result) {
    const msgEl = document.getElementById('authMessage');
    msgEl.style.color = '#12805A';
    msgEl.textContent = result.data.message;
  }
}

async function submit(url, consent) {
  const email = document.getElementById('authEmail').value.trim();
  const password = document.getElementById('authPassword').value;
  const msgEl = document.getElementById('authMessage');
  const resendLink = document.getElementById('resendVerificationLink');
  msgEl.style.color = '#6B8193';
  msgEl.textContent = 'רגע...';
  resendLink.style.display = 'none';
  const result = await authRequest(url, email, password, consent);
  if (!result.ok) {
    msgEl.style.color = '#B3261E';
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
  const email = document.getElementById('forgotEmail').value.trim();
  const msgEl = document.getElementById('authMessage');
  msgEl.style.color = '#6B8193';
  msgEl.textContent = 'רגע...';
  const res = await fetch('/api/auth/forgot-password', {
    method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ email })
  });
  const data = await res.json();
  msgEl.style.color = '#12805A';
  msgEl.textContent = data.message;
  document.getElementById('forgotPasswordBox').style.display = 'none';
}

async function resendVerification() {
  const email = document.getElementById('authEmail').value.trim();
  await fetch('/api/auth/resend-verification', {
    method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ email })
  });
  const msgEl = document.getElementById('authMessage');
  msgEl.style.color = '#12805A';
  msgEl.textContent = 'אם הכתובת רשומה ועדיין לא אומתה, נשלח אליה מייל אימות חדש';
  document.getElementById('resendVerificationLink').style.display = 'none';
}

async function handleLogout() {
  await doLogout();
  await refreshAccountUI();
}

async function refreshAccountUI() {
  const me = await fetchCurrentUser();
  document.getElementById('loggedOutView').style.display = me.loggedIn ? 'none' : 'block';
  document.getElementById('loggedInView').style.display = me.loggedIn ? 'block' : 'none';
  if (!me.loggedIn) return;
  document.getElementById('authEmailDisplay').textContent = me.email;
  await renderHistory();
  await loadInterestAreas();
}

async function renderHistory() {
  const el = document.getElementById('historyList');
  const history = await (await fetch('/api/dashboard/mine', { headers: authHeaders() })).json();
  document.getElementById('clearHistoryBtn').style.display = history.length ? 'inline-flex' : 'none';
  if (history.length === 0) {
    el.innerHTML = '<p style="color:#6B8193;font-size:13px">עדיין לא שמרת חישובים. חשבו משכנתא בעמוד "משכנתאות" כדי שהיא תישמר כאן.</p>';
    return;
  }
  const row = h => `
    <div class="history-item">
      <span>${h.category} · ${Math.round(h.purchasePrice).toLocaleString()} ש"ח <small style="color:#6B8193">(${new Date(h.createdAt).toLocaleString('he-IL')})</small></span>
      <span><button class="secondary" onclick="loadFromHistory('${h.id}')">צפייה</button></span>
    </div>`;
  el.innerHTML = history.slice(0, 3).map(row).join('')
    + (history.length > 3
      ? `<details class="history-more"><summary>הצג את כל ההיסטוריה (${history.length})</summary>${history.slice(3).map(row).join('')}</details>`
      : '');
}

async function clearHistory() {
  if (!confirm('למחוק את כל ההיסטוריה השמורה? אי אפשר לבטל את זה.')) return;
  const res = await fetch('/api/dashboard/mine', { method: 'DELETE', headers: authHeaders() });
  if (!res.ok) { alert('לא הצלחנו לנקות את ההיסטוריה, נסו שוב'); return; }
  await renderHistory();
}

async function loadInterestAreas() {
  const el = document.getElementById('interestList');
  const areas = await (await fetch('/api/interest-areas', { headers: authHeaders() })).json();
  el.replaceChildren();
  if (areas.length === 0) {
    el.innerHTML = '<p style="color:#6B8193;font-size:13px">עדיין לא הוספתם אזורי עניין.</p>';
    return;
  }
  areas.forEach(a => {
    const chip = document.createElement('span');
    chip.className = 'interest-chip';
    chip.textContent = a.name;
    const btn = document.createElement('button');
    btn.className = 'secondary';
    btn.textContent = '✕';
    btn.setAttribute('aria-label', 'הסרת ' + a.name);
    btn.onclick = () => removeInterestArea(a.id);
    chip.appendChild(btn);
    el.appendChild(chip);
  });
}

async function addInterestArea() {
  const input = document.getElementById('interestInput');
  const msg = document.getElementById('interestMsg');
  const name = input.value.trim();
  if (!name) { msg.style.color = '#B3261E'; msg.textContent = 'יש להזין שם אזור'; return; }
  const res = await fetch('/api/interest-areas', {
    method: 'POST', headers: { ...authHeaders(), 'Content-Type': 'application/json' }, body: JSON.stringify({ name })
  });
  if (!res.ok) {
    msg.style.color = '#B3261E';
    msg.textContent = res.status === 409 ? 'האזור הזה כבר ברשימה' : 'לא הצלחנו להוסיף את האזור, נסו שוב';
    return;
  }
  input.value = '';
  msg.textContent = '';
  await loadInterestAreas();
}

async function removeInterestArea(id) {
  const res = await fetch(`/api/interest-areas/${id}`, { method: 'DELETE', headers: authHeaders() });
  if (!res.ok) { alert('לא הצלחנו להסיר את האזור, נסו שוב'); return; }
  await loadInterestAreas();
}

async function loadFromHistory(id) {
  const res = await fetch(`/api/dashboard/${id}`, { headers: authHeaders() });
  if (!res.ok) { alert('החישוב הזה כבר לא זמין'); return; }
  const data = await res.json();
  const s = data.summary;
  const fmt = n => n.toLocaleString(undefined, { maximumFractionDigits: 0 });
  alert(`הון עצמי נדרש: ${fmt(s.requiredEquity)} ש"ח\nסה"כ מזומן נדרש: ${fmt(s.totalCashNeeded)} ש"ח\nתשלום חודשי משוער: ${fmt(s.monthlyPayment)} ש"ח\nלחישוב מפורט ועדכני, השתמשו בעמוד "משכנתאות".`);
}

async function comparePaths() {
  const availableEquity = numericValue(document.getElementById('cmpEquity'));
  const monthlyIncome = numericValue(document.getElementById('cmpIncome'));
  const category = document.getElementById('cmpCategory').value;
  const termYears = Number(document.getElementById('cmpTermYears').value);
  const marketValue = numericValue(document.getElementById('cmpMarketValue'));
  const purchasePrice = numericValue(document.getElementById('cmpPurchasePrice'));
  const selfBuildBudget = numericValue(document.getElementById('cmpSelfBuildBudget'));

  const scenarios = [
    { name: 'מחיר למשתכן', marketValue, purchasePrice, subsidized: true },
    { name: 'שוק חופשי (יד שנייה/קבלן)', marketValue, purchasePrice: marketValue, subsidized: false },
    { name: 'בנייה עצמית (מכרזי קרקעות)', marketValue: selfBuildBudget, purchasePrice: selfBuildBudget, subsidized: false }
  ];

  const results = await Promise.all(scenarios.map(async sc => {
    const equity = await (await fetch('/api/equity/calculate', {
      method: 'POST', headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ availableEquity, monthlyIncome, category, marketValue: sc.marketValue, purchasePrice: sc.purchasePrice, subsidized: sc.subsidized, usedBroker: false })
    })).json();
    const loanAmount = Math.max(0, sc.purchasePrice - availableEquity);
    const payment = await (await fetch('/api/mortgage/monthly-payment', {
      method: 'POST', headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ loanAmount, termYears, monthlyIncome })
    })).json();
    return { name: sc.name, equity, payment };
  }));

  const fmt = n => Math.round(n).toLocaleString();
  document.getElementById('pathComparisonResult').innerHTML = `
    <table>
      <tr><th>מסלול</th><th>הון עצמי נדרש</th><th>סה"כ מזומן נדרש</th><th>תשלום חודשי משוער</th><th>שיעור החזר מהכנסה</th></tr>
      ${results.map(r => `
        <tr>
          <td>${r.name}</td>
          <td>${fmt(r.equity.requiredEquity)} ש"ח</td>
          <td>${fmt(r.equity.totalCashNeeded)} ש"ח ${r.equity.canAfford ? '✅' : '⚠️'}</td>
          <td>${fmt(r.payment.monthlyPayment)} ש"ח</td>
          <td>${r.payment.dtiPercent.toFixed(1)}%</td>
        </tr>`).join('')}
    </table>
    <p style="font-size:11px;color:#6B8193;margin-top:6px">"מחיר למשתכן" ו"שוק חופשי" מבוססים על שווי השוק/מחיר הרכישה שמילאתם למעלה. "בנייה עצמית" הוא אומדן מפושט על סמך תקציב כולל אחד - למספרים מדויקים למכרז ספציפי היכנסו למחשבון הבנייה העצמית בעמוד "מכרזי קרקעות".</p>`;
}

refreshAccountUI();
initPageIntro('account', 'האזור האישי שלי',
  'היסטוריית החישובים ששמרת (רק למשתמשים רשומים), והשוואה בין מסלולי רכישה שונים - מחיר למשתכן מול שוק חופשי מול בנייה עצמית.');
