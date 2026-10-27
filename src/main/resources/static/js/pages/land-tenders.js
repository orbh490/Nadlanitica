function toggleDiscount() {
  document.getElementById('discountDetails').style.display = document.getElementById('enableDiscount').checked ? 'block' : 'none';
}

let tendersByLabel = {};
let selectedTenderId = null;

function labelFor(t) { return `${t.tenderNumber} - ${t.city}${t.neighborhood ? ' (' + t.neighborhood + ')' : ''}`; }

async function loadTenders() {
  const statusEl = document.getElementById('tenderListStatus');
  try {
    const tenders = await apiFetch('/api/land-tenders/self-build');
    const datalist = document.getElementById('tenderOptions');
    datalist.innerHTML = '';
    tendersByLabel = {};
    tenders.forEach(t => {
      const label = labelFor(t);
      tendersByLabel[label] = t;
      const option = document.createElement('option');
      option.value = label;
      datalist.appendChild(option);
    });
    statusEl.textContent = `${tenders.length} מכרזי בנייה עצמית פעילים כרגע (נטען עכשיו מרשות מקרקעי ישראל)`;
  } catch (e) {
    statusEl.textContent = 'לא הצלחנו לטעון את רשימת המכרזים כרגע. נסו לרענן את הדף בעוד רגע.';
  }
}

async function loadTenderDetail() {
  const label = document.getElementById('tenderSearch').value;
  const t = tendersByLabel[label];
  if (!t) {
    alert('בחרו מכרז מהרשימה (הקלידו כדי לסנן)');
    return;
  }
  const card = document.getElementById('tenderDetailCard');
  let detail;
  try {
    detail = await apiFetch(`/api/land-tenders/${t.id}`);
  } catch (err) {
    card.style.display = 'block';
    document.getElementById('tenderDetailSummary').innerHTML = errorHtml(err);
    return;
  }
  selectedTenderId = t.id;
  card.style.display = 'block';
  const fmt = n => n == null ? '—' : Math.round(n).toLocaleString();

  document.getElementById('tenderDetailSummary').innerHTML = `
    <div class="badge ok">${escapeHtml(detail.tenderNumber)} - ${escapeHtml(detail.city)}${detail.neighborhood ? ' (' + escapeHtml(detail.neighborhood) + ')' : ''}</div>
    <table>
      <tr><td>מועד פתיחת המכרז</td><td>${escapeHtml(detail.openingDate ?? 'לא פורסם')}</td></tr>
      <tr><td>מועד סגירת הגשות</td><td>${escapeHtml(detail.closingDate ?? 'לא פורסם')}</td></tr>
    </table>
    <p style="margin-top:8px">
      <a href="https://apps.land.gov.il/MichrazimSite/#/michraz/${t.id}" target="_blank" class="secondary" style="display:inline-block;text-decoration:none">🔗 המכרז באתר רמ"י</a>
    </p>`;

  document.getElementById('populationsNote').innerHTML = detail.priorityPopulations.length > 0
    ? `<p style="font-size:13px;color:#12805A">🎯 במכרז זה יש עדיפות/מכסה נפרדת לאוכלוסיות: ${detail.priorityPopulations.map(p => escapeHtml(p.label)).join(', ')}. אפשר לבחור את הקטגוריה שלכם במחשבון למטה ולקבל הצעה לאחוז ההנחה.</p>`
    : `<p style="font-size:13px;color:#6B8193">לא צוינה במכרז זה עדיפות לאוכלוסייה מסוימת.</p>`;

  const categoryWrap = document.getElementById('discountCategoryWrap');
  const categorySelect = document.getElementById('discountCategory');
  if (detail.priorityPopulations.length > 0) {
    categorySelect.innerHTML = '<option value="">בחרו קטגוריה...</option>' +
      detail.priorityPopulations.map(p => `<option value="${escapeHtml(p.code)}">${escapeHtml(p.label)}</option>`).join('') +
      '<option value="none">אף אחת מהאוכלוסיות הרשומות לא חלה עליי</option>';
    categoryWrap.style.display = 'block';
  } else {
    categoryWrap.style.display = 'none';
  }
  document.getElementById('discountSuggestionNote').innerHTML = '';

  const bookletWrap = document.getElementById('bookletLinkWrap');
  const discountsSection = document.getElementById('discountsSection');
  if (detail.bookletAvailable) {
    bookletWrap.innerHTML = `<a href="/api/land-tenders/${t.id}/booklet" target="_blank" class="secondary" style="display:inline-block;text-decoration:none">📄 צפייה בחוברת המכרז המלאה (PDF מקורי מרשות מקרקעי ישראל)</a>`;
    discountsSection.style.display = 'block';
    document.getElementById('discountsStatus').textContent = 'מחפש הנחות בחוברת המכרז...';
    document.getElementById('discountsPageLink').style.display = 'none';
    apiFetch(`/api/land-tenders/${t.id}/discounts`).then(info => {
      const statusEl = document.getElementById('discountsStatus');
      const linkEl = document.getElementById('discountsPageLink');
      if (info.discountSnippets.length === 0) {
        statusEl.textContent = 'לא נמצאו אוטומטית קטעים על הנחות בחוברת - ייתכן שהמידע קיים בניסוח שונה. מומלץ לעיין בחוברת המלאה למעלה.';
        return;
      }
      statusEl.textContent = `נמצאו ${info.discountSnippets.length} קטעים בחוברת שמזכירים הנחה ליד אוכלוסיית עדיפות.`;
      linkEl.href = `/land-tender-discounts.html?id=${t.id}`;
      linkEl.style.display = 'inline-block';
    }).catch(() => {
      document.getElementById('discountsStatus').textContent = 'לא הצלחנו לחפש הנחות בחוברת כרגע.';
    });
  } else {
    bookletWrap.innerHTML = '<p style="font-size:13px;color:#6B8193">חוברת המכרז טרם פורסמה במערכת רמ"י.</p>';
    discountsSection.style.display = 'none';
  }

  const plotsEl = document.getElementById('plotsTable');
  const fmtMoney = n => n == null ? '<span style="color:var(--bad-text)">טרם פורסם ע"י רמ"י</span>' : fmt(n) + ' ש"ח';
  if (detail.plots.length === 0) {
    plotsEl.innerHTML = '<p style="color:#6B8193;font-size:13px">אין עדיין נתוני מגרשים מפורטים למכרז זה</p>';
  } else {
    const anyMissingPrice = detail.plots.some(p => p.appraisedPrice == null);
    const rowHtml = (p, i) => `
        <tr>
          <td>${escapeHtml(p.areaName ?? p.plotId)}</td>
          <td>${escapeHtml(p.areaSqm ?? '—')}</td>
          <td>${fmtMoney(p.appraisedPrice)}</td>
          <td>${fmtMoney(p.developmentCost)}</td>
          <td>${fmtMoney(p.guaranteeDeposit)}</td>
          <td><button class="secondary" onclick="usePlotInCalculator(${i})">השתמש במחשבון</button></td>
        </tr>`;
    const PLOTS_SHOWN_INITIALLY = 3;
    const firstRows = detail.plots.slice(0, PLOTS_SHOWN_INITIALLY).map(rowHtml).join('');
    const restRows = detail.plots.slice(PLOTS_SHOWN_INITIALLY).map(rowHtml).join('');
    plotsEl.innerHTML = `
      ${anyMissingPrice ? '<p style="font-size:12px;color:#6B8193;margin-bottom:8px">חלק מהמגרשים עדיין ללא מחיר שמאי מפורסם במערכת רמ"י (לא קשור לתקלה אצלנו - חסר גם באתר הרשמי) - אפשר להזין הערכה ידנית במחשבון למטה.</p>' : ''}
      <div style="overflow-x:auto">
      <table>
      <tr><th>מגרש</th><th>שטח (מ"ר)</th><th>מחיר סף/שמאי</th><th>עלות פיתוח</th><th>ערבות</th><th></th></tr>
      ${firstRows}
      </table>
      </div>
    ${detail.plots.length > PLOTS_SHOWN_INITIALLY ? `
    <details class="table-toggle">
      <summary>הצג את כל ${detail.plots.length} המגרשים</summary>
      <div style="overflow-x:auto"><table>${restRows}</table></div>
    </details>` : ''}`;
  }
  window.lastPlots = detail.plots;
}

async function loadDiscountSuggestion() {
  const code = document.getElementById('discountCategory').value;
  const noteEl = document.getElementById('discountSuggestionNote');
  if (!code || code === 'none' || !selectedTenderId) {
    noteEl.innerHTML = '';
    return;
  }
  noteEl.style.color = '#6B8193';
  noteEl.textContent = 'מחפש הצעה בחוברת המכרז...';
  try {
    const suggestion = await apiFetch(`/api/land-tenders/${selectedTenderId}/discount-suggestion?populationCode=${encodeURIComponent(code)}`);
    if (suggestion.suggestedPercent == null) {
      noteEl.style.color = '#6B8193';
      noteEl.innerHTML = 'לא נמצאה אוטומטית הצעה לקטגוריה זו - בדקו את חוברת המכרז והזינו את האחוז ידנית למטה.';
      return;
    }
    document.getElementById('discountPercent').value = suggestion.suggestedPercent;
    noteEl.style.color = '#12805A';
    noteEl.innerHTML = `הצעה למילוי אוטומטי: <b>${escapeHtml(suggestion.suggestedPercent)}%</b> (ניתן לערוך למטה). מבוסס על הקטע הבא בחוברת - יש לאשר:<br>
      <span style="display:block;background:#F4F8FA;border-radius:8px;padding:8px 10px;margin-top:4px;color:#333">${escapeHtml(suggestion.sourceSnippet)}</span>`;
  } catch (e) {
    noteEl.style.color = '#6B8193';
    noteEl.textContent = 'לא הצלחנו לחפש הצעה כרגע - הזינו את האחוז ידנית לפי החוברת.';
  }
}

function usePlotInCalculator(index) {
  const p = window.lastPlots[index];
  if (p.appraisedPrice) document.getElementById('landPrice').value = Math.round(p.appraisedPrice).toLocaleString('en-US');
  if (p.developmentCost) document.getElementById('developmentCost').value = Math.round(p.developmentCost).toLocaleString('en-US');
  if (p.guaranteeDeposit) document.getElementById('guaranteeDeposit').value = Math.round(p.guaranteeDeposit).toLocaleString('en-US');
  document.getElementById('selfBuildResult').scrollIntoView({ behavior: 'smooth' });
}

async function calculateSelfBuild() {
  const body = {
    landPrice: numericValue(document.getElementById('landPrice')),
    developmentCost: numericValue(document.getElementById('developmentCost')),
    discountPercent: document.getElementById('enableDiscount').checked ? Number(document.getElementById('discountPercent').value) : 0,
    guaranteeDeposit: numericValue(document.getElementById('guaranteeDeposit')),
    constructionBudget: numericValue(document.getElementById('constructionBudget')),
    availableEquity: numericValue(document.getElementById('availableEquity')),
    monthlyIncome: numericValue(document.getElementById('monthlyIncome')),
    category: document.getElementById('category').value,
    termYears: Number(document.getElementById('termYears').value)
  };
  const el = document.getElementById('selfBuildResult');
  let data;
  try {
    data = await postJson('/api/self-build/calculate', body);
  } catch (err) {
    el.innerHTML = errorHtml(err);
    return;
  }
  const fmt = n => n.toLocaleString(undefined, { maximumFractionDigits: 0 });
  el.innerHTML = `
    <span class="badge ${data.canAffordEquity ? 'ok' : 'bad'}">${data.canAffordEquity ? 'יש לך מספיק הון עצמי' : 'חסר לך הון עצמי'}</span>
    <table>
      <tr><td>שווי פרויקט כולל (קרקע+פיתוח+בנייה)</td><td>${fmt(data.totalPropertyValue)} ש"ח</td></tr>
      <tr><td>הון עצמי נדרש (לפי שווי הפרויקט)</td><td>${fmt(data.totalCashNeeded - data.purchaseTax - data.closingCosts - data.guaranteeDeposit)} ש"ח</td></tr>
      <tr><td>מס רכישה (על הקרקע בלבד)</td><td>${fmt(data.purchaseTax)} ש"ח</td></tr>
      <tr><td>עלויות סגירה (אומדן)</td><td>${fmt(data.closingCosts)} ש"ח</td></tr>
      <tr><td>ערבות מכרז</td><td>${fmt(data.guaranteeDeposit)} ש"ח</td></tr>
      <tr><td><b>סה"כ מזומן נדרש</b></td><td><b>${fmt(data.totalCashNeeded)} ש"ח</b></td></tr>
    </table>
    <table style="margin-top:10px">
      <tr><td>סכום הלוואה משוער</td><td>${fmt(data.loanAmount)} ש"ח</td></tr>
      <tr><td>תשלום חודשי משוער</td><td>${fmt(data.monthlyPayment)} ש"ח</td></tr>
      <tr><td>שיעור החזר מהכנסה</td><td>${data.dtiPercent.toFixed(1)}% ${data.withinLegalDtiLimit ? '(בתוך המגבלה)' : '(חורג מהמגבלה הבנקאית של 50%!)'}</td></tr>
    </table>
    ${ltvCapNote(data)}
    <p style="font-size:11px;color:#6B8193;margin-top:6px">הערכה בלבד: תקציב הבנייה הוא הזנה ידנית שלכם, ומדיניות הבנק המדויקת למשכנתא לבנייה עצמית (למשל פריסת ההלוואה לשלבים) משתנה בין בנקים.</p>`;
}

loadTenders();
initPageIntro('land-tenders', 'מכרזי קרקעות - בנייה עצמית',
  'רשימה חיה של מכרזי קרקע פעילים מרשות מקרקעי ישראל, עם מחשבון שמעריך את העלות הכוללת של בנייה עצמית - מחיר הקרקע, היטלי פיתוח ועלויות בנייה - כדי להשוות מול אפשרויות רכישה אחרות.');
