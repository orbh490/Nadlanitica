function toggleTrackMix() {
  document.getElementById('trackMixDetails').style.display = document.getElementById('enableTrackMix').checked ? 'block' : 'none';
}

async function submitProfile() {
  const body = {
    availableEquity: numericValue(document.getElementById('availableEquity')),
    monthlyIncome: numericValue(document.getElementById('monthlyIncome')),
    category: document.getElementById('category').value,
    marketValue: numericValue(document.getElementById('marketValue')),
    purchasePrice: numericValue(document.getElementById('purchasePrice')),
    subsidized: document.getElementById('subsidized').checked,
    usedBroker: document.getElementById('usedBroker').checked,
    termYears: Number(document.getElementById('termYears').value)
  };
  const res = await fetch('/api/dashboard', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...authHeaders() },
    body: JSON.stringify(body)
  });
  const data = await res.json();
  renderResults(data.summary);
  document.getElementById('saveNote').innerHTML = data.id
    ? '✅ החישוב נשמר <a href="/account.html">באזור האישי שלך</a>.'
    : '🔒 לא מחוברים - החישוב לא נשמר. <a href="/account.html">התחברו/הירשמו</a> כדי לשמור חישובים.';
}

function renderResults(s) {
  const fmt = n => n.toLocaleString(undefined, {maximumFractionDigits: 0});
  document.getElementById('resultsSection').style.display = 'block';

  document.getElementById('affordabilitySummary').innerHTML = `
    <span class="badge ${s.canAffordEquity ? 'ok' : 'bad'}">${s.canAffordEquity ? 'יש לך מספיק הון עצמי' : 'חסר לך הון עצמי'}</span>
    <table>
      <tr><td>הון עצמי נדרש</td><td>${fmt(s.requiredEquity)} ש"ח</td></tr>
      <tr><td>מס רכישה</td><td>${fmt(s.purchaseTax)} ש"ח</td></tr>
      <tr><td>עלויות סגירה (אומדן)</td><td>${fmt(s.closingCosts)} ש"ח</td></tr>
      <tr><td><b>סה"כ מזומן נדרש</b></td><td><b id="totalCashNeededValue">0 ש"ח</b></td></tr>
    </table>`;
  animateNumber(document.getElementById('totalCashNeededValue'), s.totalCashNeeded, { suffix: ' ש"ח' });

  document.getElementById('paymentSummary').innerHTML = `
    <span class="badge ${s.withinLegalDtiLimit ? 'ok' : 'bad'}">${s.withinLegalDtiLimit ? 'בתוך מגבלת ההחזר הבנקאית' : 'חורג ממגבלת ההחזר הבנקאית (50%)'}</span>
    <table>
      <tr><td>סכום הלוואה משוער</td><td>${fmt(s.loanAmount)} ש"ח</td></tr>
      <tr><td>ריבית שנתית ממוצעת</td><td>${s.annualRatePercent.toFixed(2)}%</td></tr>
      <tr><td>תשלום חודשי משוער (קרן+ריבית)</td><td>${fmt(s.monthlyPayment)} ש"ח</td></tr>
      <tr><td>שיעור החזר מהכנסה</td><td>${s.dtiPercent.toFixed(1)}%</td></tr>
      <tr><td><small>ביטוח חיים + מבנה (אומדן שוק)</small></td><td><small>${fmt(s.insuranceEstimate.totalMonthly)} ש"ח לחודש</small></td></tr>
      <tr><td><b>סה"כ עלות חודשית משוערת</b></td><td><b id="totalMonthlyValue">0 ש"ח</b></td></tr>
    </table>
    ${ltvCapNote(s)}
    <p style="font-size:11px;color:#6B8193;margin-top:4px">ביטוח המשכנתא הוא אומדן שוק בלבד (תלוי גיל ובריאות) - ניתן לרכוש פוליסה חיצונית לבנק.</p>
`;
  animateNumber(document.getElementById('totalMonthlyValue'), s.totalMonthlyWithInsurance, { suffix: ' ש"ח' });

  document.getElementById('trackMixLoanAmount').value = Math.round(s.loanAmount).toLocaleString('en-US');
  document.getElementById('trackMixTermYears').value = s.termYears;

  document.getElementById('calcBubbles').style.display = 'grid';
  animateNumber(document.getElementById('bubbleCash'), s.totalCashNeeded, { suffix: ' ש"ח' });
  animateNumber(document.getElementById('bubbleMonthly'), s.monthlyPayment, { suffix: ' ש"ח' });
  document.getElementById('bubbleDti').textContent = s.dtiPercent.toFixed(1) + '%';
  document.getElementById('bubbleDti').className = s.withinLegalDtiLimit ? 'good' : 'bad';
  document.getElementById('bubbleDtiNote').textContent = s.withinLegalDtiLimit ? 'בתוך מגבלת ההחזר של 50%' : 'חורג ממגבלת ההחזר של 50%';

  renderAmortizationChart(s.loanAmount, s.termYears);
  loadRateStressTest(s.loanAmount, s.termYears);
}

// ---- מה קורה אם הריבית עולה (בדיקה עצמית, ראו הסבר בכרטיס) ----
async function loadRateStressTest(loanAmount, termYears) {
  const card = document.getElementById('rateStressCard');
  const monthlyIncome = numericValue(document.getElementById('monthlyIncome'));
  if (!loanAmount || !termYears) { card.style.display = 'none'; return; }
  card.style.display = 'block';
  const fmt = n => Math.round(n).toLocaleString();
  const data = await (await fetch('/api/mortgage/rate-stress-test', {
    method: 'POST', headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ loanAmount, termYears, monthlyIncome })
  })).json();
  document.getElementById('rateStressResult').innerHTML = `
    <table>
      <tr><th>תרחיש</th><th>ריבית שנתית</th><th>תשלום חודשי</th><th>שיעור החזר מהכנסה</th></tr>
      <tr><td>המצב הנוכחי</td><td>${data.baseAnnualRatePercent.toFixed(2)}%</td><td>${fmt(data.basePayment)} ש"ח</td><td>—</td></tr>
      ${data.scenarios.map(sc => `
        <tr>
          <td>עלייה של ${sc.extraRatePercent.toFixed(0)} נקודות אחוז</td>
          <td>${sc.annualRatePercent.toFixed(2)}%</td>
          <td>${fmt(sc.monthlyPayment)} ש"ח</td>
          <td>${sc.dtiPercent.toFixed(1)}% ${sc.withinLegalDtiLimit ? '✅' : '⚠️ חורג מ-50%'}</td>
        </tr>`).join('')}
    </table>
    <p style="font-size:11px;color:#6B8193;margin-top:6px">התרחישים כאן להמחשה בלבד - כדי לבדוק אם תוכלו לעמוד בתשלום גם אם הריבית תעלה, לפני שבוחרים כמה מהמשכנתא לשים במסלול צמוד-ריבית (פריים) בתמהיל למטה.</p>`;
}

// ---- SVG helpers ----
const SVG_NS = 'http://www.w3.org/2000/svg';
function svgEl(tag, attrs) {
  const el = document.createElementNS(SVG_NS, tag);
  for (const [k, v] of Object.entries(attrs || {})) el.setAttribute(k, v);
  return el;
}

const tooltipEl = document.getElementById('chartTooltip');
function showTooltip(evt, rowsHtml) {
  tooltipEl.innerHTML = rowsHtml;
  tooltipEl.style.display = 'block';
  tooltipEl.style.left = (evt.clientX + 14) + 'px';
  tooltipEl.style.top = (evt.clientY + 14) + 'px';
}
function hideTooltip() { tooltipEl.style.display = 'none'; }

function niceMax(value) {
  if (value <= 0) return 1;
  const magnitude = Math.pow(10, Math.floor(Math.log10(value)));
  const normalized = value / magnitude;
  const step = normalized <= 1 ? 1 : normalized <= 2 ? 2 : normalized <= 5 ? 5 : 10;
  return step * magnitude;
}

// ---- Amortization stacked bar chart: קרן מול ריבית לאורך שנות ההלוואה ----
async function renderAmortizationChart(loanAmount, termYears) {
  const container = document.getElementById('amortizationChart');
  if (!loanAmount || !termYears) { container.innerHTML = ''; return; }
  const schedule = await (await fetch(`/api/mortgage/amortization-schedule?loanAmount=${loanAmount}&termYears=${termYears}`)).json();

  // רוחב ה-viewBox נמדד מהקונטיינר בפועל (לא קבוע) כדי שגודל הטקסט יישאר קריא במסך צר - ראו הערה דומה
  // ב-results-board.html שבה אותה בעיה בדיוק גרמה לתוויות בלתי קריאות בנייד.
  const width = Math.max(280, Math.round(container.clientWidth || 640));
  const isNarrow = width < 420;
  const height = 260, marginTop = 10, marginBottom = 24, marginLeft = isNarrow ? 52 : 60, marginRight = 8;
  const plotW = width - marginLeft - marginRight, plotH = height - marginTop - marginBottom;
  const maxYearTotal = Math.max(...schedule.map(y => y.principalPaid + y.interestPaid));
  const yMax = niceMax(maxYearTotal);
  const bandW = plotW / schedule.length;
  const barW = Math.min(16, bandW * 0.5);
  const gap = 2;
  const fmt0 = n => Math.round(n).toLocaleString();

  const svg = svgEl('svg', { viewBox: `0 0 ${width} ${height}`, width: '100%', height, role: 'img',
    'aria-label': 'פילוח תשלום קרן וריבית לפי שנה' });

  [0, 0.5, 1].forEach(f => {
    const y = marginTop + plotH * (1 - f);
    svg.appendChild(svgEl('line', { class: 'grid-line', x1: marginLeft, x2: marginLeft + plotW, y1: y, y2: y }));
    const label = svgEl('text', { class: 'axis-label', x: marginLeft - 6, y: y - 3, 'text-anchor': 'end' });
    label.textContent = fmt0(yMax * f);
    svg.appendChild(label);
  });
  svg.appendChild(svgEl('line', { class: 'axis-line', x1: marginLeft, x2: marginLeft + plotW, y1: marginTop + plotH, y2: marginTop + plotH }));

  schedule.forEach((row, i) => {
    const x = marginLeft + i * bandW + (bandW - barW) / 2;
    const principalH = (row.principalPaid / yMax) * plotH;
    const interestH = (row.interestPaid / yMax) * plotH;
    const baseline = marginTop + plotH;

    const principalRect = svgEl('rect', {
      class: 'bar-seg', x, width: barW, fill: 'var(--series-principal)',
      y: baseline - principalH, height: Math.max(0, principalH), rx: 0
    });
    const interestTop = baseline - principalH - gap - interestH;
    const interestRect = svgEl('rect', {
      class: 'bar-seg', x, width: barW, fill: 'var(--series-interest)',
      y: interestTop, height: Math.max(0, interestH), rx: 4
    });
    [principalRect, interestRect].forEach(rect => {
      rect.addEventListener('pointermove', evt => showTooltip(evt,
        `<b>שנה ${row.year}</b><br>קרן: <b>${fmt0(row.principalPaid)}</b> ש"ח<br>ריבית: <b>${fmt0(row.interestPaid)}</b> ש"ח`));
      rect.addEventListener('pointerleave', hideTooltip);
    });
    svg.appendChild(principalRect);
    svg.appendChild(interestRect);

    if (termYears <= 15 || row.year % 5 === 0 || row.year === 1) {
      const xLabel = svgEl('text', { class: 'axis-label', x: x + barW / 2, y: height - 6, 'text-anchor': 'middle' });
      xLabel.textContent = row.year;
      svg.appendChild(xLabel);
    }
  });

  container.innerHTML = `<div class="chart-legend">
      <span><span class="swatch" style="background:var(--series-principal)"></span>קרן</span>
      <span><span class="swatch" style="background:var(--series-interest)"></span>ריבית</span>
    </div>`;
  container.appendChild(svg);

  const totalPrincipal = schedule.reduce((a, r) => a + r.principalPaid, 0);
  const totalInterest = schedule.reduce((a, r) => a + r.interestPaid, 0);
  const details = document.createElement('details');
  details.className = 'table-toggle';
  details.innerHTML = `<summary>הצג טבלה מלאה לפי שנה</summary>
    <table>
      <tr><th>שנה</th><th>קרן</th><th>ריבית</th><th>יתרת הלוואה</th></tr>
      ${schedule.map(r => `<tr><td>${r.year}</td><td>${fmt0(r.principalPaid)}</td><td>${fmt0(r.interestPaid)}</td><td>${fmt0(r.remainingBalance)}</td></tr>`).join('')}
      <tr><td><b>סה"כ</b></td><td><b>${fmt0(totalPrincipal)}</b></td><td><b>${fmt0(totalInterest)}</b></td><td>—</td></tr>
    </table>`;
  container.appendChild(details);
}

async function calculateTrackMix() {
  const fixedPercent = Number(document.getElementById('fixedPercent').value);
  const cpiPercent = Number(document.getElementById('cpiPercent').value);
  const primePercent = Number(document.getElementById('primePercent').value);
  const resultEl = document.getElementById('trackMixResult');
  const sum = fixedPercent + cpiPercent + primePercent;
  if (Math.round(sum) !== 100) {
    resultEl.innerHTML = `<p style="color:#B3261E;font-size:13px">האחוזים חייבים להסתכם ל-100% (כרגע: ${sum}%)</p>`;
    return;
  }
  const body = {
    loanAmount: numericValue(document.getElementById('trackMixLoanAmount')),
    termYears: Number(document.getElementById('trackMixTermYears').value),
    fixedNonIndexedPercent: fixedPercent,
    cpiLinkedPercent: cpiPercent,
    primePercent: primePercent
  };
  const data = await (await fetch('/api/mortgage/track-mix', {
    method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body)
  })).json();
  const fmt = n => n.toLocaleString(undefined, {maximumFractionDigits: 0});
  const capWarning = data.exceedsVariableRateCap
    ? '<p style="font-size:12px;color:#B3261E;margin-top:8px">⚠ מסלול הפריים חורג מ-66.66% מההלוואה - הוראה 329 של בנק ישראל אוסרת על הבנק לאשר תמהיל כזה.</p>'
    : '';
  resultEl.innerHTML = `
    <div class="badge ok">ריבית ממוצעת משוקללת: ${data.blendedAnnualRatePercent.toFixed(2)}%</div>
    <table>
      <tr><th>מסלול</th><th>אחוז</th><th>ריבית</th><th>סכום</th><th>תשלום חודשי</th></tr>
      ${data.tracks.map(t => `<tr><td>${t.trackName}</td><td>${t.percentOfLoan}%</td><td>${t.annualRatePercent.toFixed(2)}%</td><td>${fmt(t.loanPortion)} ש"ח</td><td>${fmt(t.monthlyPayment)} ש"ח</td></tr>`).join('')}
      <tr><td colspan="4"><b>סה"כ תשלום חודשי</b></td><td><b>${fmt(data.totalMonthlyPayment)} ש"ח</b></td></tr>
    </table>
    ${capWarning}`;
}

initPageIntro('dashboard', 'המשכנתא שלי',
  'מחשבון המשכנתא המרכזי: כמה הון עצמי צריך, מה התשלום החודשי הצפוי, ואיך זה משתנה בתרחישי ריבית שונים - כדי לדעת בדיוק איפה אתה עומד לפני שחותמים.');
