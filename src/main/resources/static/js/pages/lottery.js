function toggleScoreCalc() {
  document.getElementById('scoreDetails').style.display = document.getElementById('enableScoreCalc').checked ? 'block' : 'none';
}

let projectsByLabel = {};
let lastScoreInfo = null;
let allProjects = [];

function labelFor(p) { return `${p.city} - ${p.projectName}`; }

async function loadProjects() {
  allProjects = await (await fetch('/api/lottery/projects')).json();
  allProjects.sort((a, b) => labelFor(a).localeCompare(labelFor(b), 'he'));
  renderProjectOptions();
}

function renderProjectOptions() {
  const filter = document.getElementById('liveFilter').value;
  const projects = filter === 'open' ? allProjects.filter(p => p.liveStatus === 'OPEN')
    : filter === 'open_or_upcoming' ? allProjects.filter(p => p.liveStatus === 'OPEN' || p.liveStatus === 'NOT_YET_OPEN')
    : allProjects;
  const datalist = document.getElementById('projectOptions');
  datalist.innerHTML = '';
  projectsByLabel = {};
  projects.forEach(p => {
    const label = labelFor(p);
    projectsByLabel[label] = p;
    const option = document.createElement('option');
    option.value = label;
    datalist.appendChild(option);
  });
  document.getElementById('projectSearch').value = '';
  const openCount = allProjects.filter(p => p.liveStatus === 'OPEN').length;
  const upcomingCount = allProjects.filter(p => p.liveStatus === 'NOT_YET_OPEN').length;
  document.getElementById('openCountStatus').textContent =
    `${openCount} פרויקטים עם הרשמה פתוחה כרגע, ${upcomingCount} עומדים להיפתח בקרוב (מתוך ${allProjects.length} ברשימה)`;
}

async function calculateScore() {
  const body = {
    numberOfChildren: Number(document.getElementById('numberOfChildren').value),
    completedMilitaryOrNationalService: document.getElementById('completedMilitaryOrNationalService').checked,
    newImmigrant: document.getElementById('newImmigrant').checked,
    disabledCoupleExemptFromService: document.getElementById('disabledCoupleExemptFromService').checked,
    medicalDisability65Plus: document.getElementById('medicalDisability65Plus').checked,
    earningIncapacity75Plus: document.getElementById('earningIncapacity75Plus').checked,
    lowIncomeHousehold: document.getElementById('lowIncomeHousehold').checked,
    failedPriorApplications: Number(document.getElementById('failedPriorApplications').value)
  };
  const data = await (await fetch('/api/lottery/eligibility-score', {
    method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body)
  })).json();
  lastScoreInfo = data;
  const resultEl = document.getElementById('scoreResult');
  if (!data.meetsMinimumThreshold) {
    resultEl.innerHTML = `<div class="badge bad">ניקוד: <span id="scoreValue">0</span></div><p style="font-size:13px;margin-top:8px">ניקוד זה נמוך מסף הרישום המינימלי (11 נקודות) במסלול הכללי.</p>`;
    revealResult(resultEl);
    animateNumber(document.getElementById('scoreValue'), data.score, { duration: 400 });
    return;
  }
  resultEl.innerHTML = `
    <div class="badge ok">ניקוד: <span id="scoreValue">0</span></div>
    <p style="font-size:13px;margin-top:8px">את/ה בקבוצת <b>${data.bracketLabel}</b>, שמקבלת <b>${data.unitSharePercent}%</b> מהדירות בכל הגרלה. ההגרלה בתוך הקבוצה אקראית לחלוטין.</p>`;
  revealResult(resultEl);
  animateNumber(document.getElementById('scoreValue'), data.score, { duration: 400 });
}

function fmtDate(iso) {
  if (!iso) return '—';
  const d = new Date(iso);
  return d.toLocaleDateString('he-IL');
}

// העוגה לא ממלאת לפי האחוז המוחלט (שם היא תמיד נראית ריקה - סיכויים אמיתיים נעים סביב כמה אחוזים בודדים).
// במקום זה, כשיש נתון היסטורי אמיתי, היא מחלקת את הטבעת בין שני נתונים אמיתיים בלבד - הסיכוי החזוי מול
// שיעור הזכייה שהיה בפועל בעבר - ביחס זה לזה, כדי שההשוואה תהיה קריאה. בלי "פירוק לגורמים" מומצא.
function oddsDonutHtml(predictedPercent, historicalPercent, poolUnits, poolApplicants) {
  const size = 180, strokeWidth = 20, radius = (size - strokeWidth) / 2, center = size / 2;
  const circumference = 2 * Math.PI * radius;
  const hasCompetitors = poolApplicants > poolUnits && poolApplicants > 0;
  const legendPool = `<span><span style="display:inline-block;width:10px;height:10px;border-radius:2px;background:var(--primary-500);margin-inline-end:5px"></span>${poolUnits.toLocaleString()} יחידות בקבוצה שלך</span>
        ${hasCompetitors ? `<span><span style="display:inline-block;width:10px;height:10px;border-radius:2px;background:var(--border);margin-inline-end:5px"></span>${poolApplicants.toLocaleString()} נרשמים מתחרים בקבוצה</span>` : ''}`;

  if (historicalPercent == null) {
    const wonFraction = Math.max(0, Math.min(1, predictedPercent / 100));
    const wonLength = circumference * wonFraction;
    return `
      <div style="display:flex;flex-direction:column;align-items:center;margin-top:8px">
        <svg viewBox="0 0 ${size} ${size}" width="${size}" height="${size}" role="img" aria-label="סיכוי זכייה חזוי ${predictedPercent.toFixed(1)} אחוז">
          <circle cx="${center}" cy="${center}" r="${radius}" fill="none" stroke="var(--border)" stroke-width="${strokeWidth}" />
          <circle cx="${center}" cy="${center}" r="${radius}" fill="none" stroke="var(--primary-500)" stroke-width="${strokeWidth}"
            stroke-dasharray="${wonLength.toFixed(1)} ${(circumference - wonLength).toFixed(1)}" stroke-linecap="round"
            transform="rotate(-90 ${center} ${center})" />
          <text x="${center}" y="${center - 2}" text-anchor="middle" font-size="30" font-weight="700" fill="var(--ink)" id="oddsDonutPercent">0%</text>
          <text x="${center}" y="${center + 22}" text-anchor="middle" font-size="12" fill="var(--ink-secondary)">סיכוי חזוי</text>
        </svg>
        <div style="display:flex;gap:18px;margin-top:10px;font-size:12px;color:var(--ink-secondary);flex-wrap:wrap;justify-content:center">${legendPool}</div>
      </div>`;
  }

  // שני פלחים אמיתיים בלבד, ביחס זה לזה (לא ליחס מתוך 100) - כדי שההשוואה תיראה גם כששני המספרים קטנים
  const total = predictedPercent + historicalPercent;
  const predictedFraction = total > 0 ? predictedPercent / total : 0.5;
  const predictedLength = circumference * predictedFraction;
  const historicalLength = circumference - predictedLength;
  return `
    <div style="display:flex;flex-direction:column;align-items:center;margin-top:8px">
      <svg viewBox="0 0 ${size} ${size}" width="${size}" height="${size}" role="img" aria-label="סיכוי חזוי ${predictedPercent.toFixed(1)} אחוז, שיעור זכייה בפועל בעבר ${historicalPercent.toFixed(1)} אחוז">
        <circle cx="${center}" cy="${center}" r="${radius}" fill="none" stroke="var(--border)" stroke-width="${strokeWidth}" />
        <circle cx="${center}" cy="${center}" r="${radius}" fill="none" stroke="var(--primary-500)" stroke-width="${strokeWidth}"
          stroke-dasharray="${predictedLength.toFixed(1)} ${(circumference - predictedLength).toFixed(1)}" stroke-linecap="round"
          transform="rotate(-90 ${center} ${center})" />
        <circle cx="${center}" cy="${center}" r="${radius}" fill="none" stroke="#e08a2c" stroke-width="${strokeWidth}"
          stroke-dasharray="${historicalLength.toFixed(1)} ${(circumference - historicalLength).toFixed(1)}" stroke-linecap="round"
          transform="rotate(${-90 + predictedFraction * 360} ${center} ${center})" />
        <text x="${center}" y="${center - 2}" text-anchor="middle" font-size="30" font-weight="700" fill="var(--ink)" id="oddsDonutPercent">0%</text>
        <text x="${center}" y="${center + 22}" text-anchor="middle" font-size="12" fill="var(--ink-secondary)">סיכוי חזוי</text>
      </svg>
      <div style="display:flex;gap:18px;margin-top:10px;font-size:12px;color:var(--ink-secondary);flex-wrap:wrap;justify-content:center">
        <span><span style="display:inline-block;width:10px;height:10px;border-radius:2px;background:var(--primary-500);margin-inline-end:5px"></span>${predictedPercent.toFixed(1)}% סיכוי חזוי</span>
        <span><span style="display:inline-block;width:10px;height:10px;border-radius:2px;background:#e08a2c;margin-inline-end:5px"></span>${historicalPercent.toFixed(1)}% זכייה בפועל בעבר</span>
      </div>
      <div style="display:flex;gap:18px;margin-top:6px;font-size:12px;color:var(--ink-secondary);flex-wrap:wrap;justify-content:center">${legendPool}</div>
    </div>`;
}

function liveStatusHtml(live) {
  if (!live || !live.hasLiveData) {
    return '<p style="font-size:13px;color:#6B8193;margin-top:8px">אין כרגע הרשמה פתוחה או עתידית לפרויקט זה בלוח החי של משרד הבינוי והשיכון.</p>';
  }
  const p = live.liveProject;
  if (p.status === 'NOT_YET_OPEN') {
    return `<p style="font-size:13px;color:#a6650a;margin-top:8px">🕓 טרם נפתח להרשמה - צפוי להיפתח ב-${fmtDate(p.applicationStartDate)}.</p>`;
  }
  return `
    <div class="badge ok" style="margin-top:8px">✅ פתוח להרשמה כרגע</div>
    <table style="margin-top:8px">
      <tr><td>סגירת הרשמה</td><td>${fmtDate(p.applicationEndDate)}</td></tr>
      <tr><td>מסלול</td><td>${p.processName ?? '—'}</td></tr>
      <tr><td>זכאות</td><td>${p.entitlement ?? '—'}</td></tr>
      <tr><td>יח"ד</td><td>${p.housingUnits ?? '—'}</td></tr>
      <tr><td>מחיר בסיס למ"ר</td><td>${p.pricePerSqm ? Math.round(p.pricePerSqm).toLocaleString() + ' ש"ח (לפני הנחה)' : '—'}</td></tr>
      <tr><td>יזם</td><td>${p.contractorName ?? '—'}</td></tr>
      <tr><td>נרשמים עד כה</td><td>${p.totalSubscribers ?? '—'}${p.totalLocalSubscribers ? ' (מתוכם ' + p.totalLocalSubscribers + ' בני מקום)' : ''}</td></tr>
    </table>
    <p style="font-size:11px;color:#6B8193;margin-top:6px">מחיר הבסיס למ"ר הוא לפני הנחה - שיעור ההנחה בפועל ותנאיו משתנים בין מכרזים, יש לבדוק את תנאי המכרז הספציפי.</p>`;
}

async function calculateOdds() {
  const label = document.getElementById('projectSearch').value;
  const p = projectsByLabel[label];
  const resultEl = document.getElementById('oddsResult');
  if (!p) {
    resultEl.innerHTML = '<p style="color:#B3261E;font-size:13px">בחרו פרויקט מהרשימה (הקלידו כדי לסנן)</p>';
    return;
  }
  const localResident = document.getElementById('localResident').checked;
  const url = `/api/lottery/odds?city=${encodeURIComponent(p.city)}&project=${encodeURIComponent(p.projectName)}&localResident=${localResident}`;
  const oddsPromise = fetch(url).then(r => r.json());
  const livePromise = p.projectId
    ? fetch(`/api/lottery/live-status?projectId=${encodeURIComponent(p.projectId)}`).then(r => r.json())
    : Promise.resolve({ hasLiveData: false });
  const [data, live] = await Promise.all([oddsPromise, livePromise]);
  const historicalLine = data.actualHistoricalWinRatePercent == null
    ? '<p style="font-size:13px;margin-top:10px;text-align:center"><small style="color:#6B8193">אין נתון זכייה בפועל זמין להגרלה זו</small></p>'
    : '';
  // המספר למעלה הוא הממוצע האמיתי של כלל הנרשמים בקבוצה (בן מקום/כלל הארץ) - לא לפי קבוצת הניקוד.
  // אין נתון רשמי על כמה נרשמים נמצאים בכל קבוצת ניקוד בהגרלה הספציפית הזו, ולכן אי אפשר לחשב באופן אמין
  // סיכוי מדויק לקבוצת הניקוד שלך - רק להציג איזה חלק מהדירות (מתוך התקנון) שמור לה.
  const scoreNote = lastScoreInfo && lastScoreInfo.meetsMinimumThreshold
    ? `<p style="font-size:12px;color:#6B8193;margin-top:10px">המספר הזה הוא הממוצע האמיתי של כלל הנרשמים בקבוצה שלך - לא מותאם לניקוד, כי אין נתון רשמי על פילוג הנרשמים בין קבוצות הניקוד. מה שכן ידוע: הניקוד שלך (${lastScoreInfo.score}, קבוצת ${lastScoreInfo.bracketLabel}) מזכה אתכם ב-${lastScoreInfo.unitSharePercent}% מהדירות בהגרלה, וההגרלה בתוך הקבוצה אקראית לחלוטין.</p>`
    : '<p style="font-size:12px;color:#6B8193;margin-top:10px">חשבו קודם את הניקוד שלכם למעלה כדי לדעת איזה חלק מהדירות שמור לקבוצת הניקוד שלכם.</p>';
  const peripheryNote = data.peripheryGrantEligibleCity
    ? '<p style="font-size:12px;color:#12805A;margin-top:6px">🎁 עיר זו מוכרת כישוב עדיפות לאומית - ייתכן שתהיו זכאים למענק מותנה של כ-50,000-60,000 ש"ח, שבנק ישראל מכיר בו כחלק מההון העצמי (בכפוף למגורים בדירה 5-7 שנים). בדקו זכאות מדויקת מול משרד השיכון.</p>'
    : '';
  resultEl.innerHTML = `
    <div class="badge ok">${data.city} - ${data.projectName}</div>
    ${oddsDonutHtml(data.winProbabilityPercent, data.actualHistoricalWinRatePercent, data.poolUnits, data.poolApplicants)}
    ${historicalLine}
    ${liveStatusHtml(live)}
    ${scoreNote}
    ${peripheryNote}`;
  revealResult(resultEl);
  animateNumber(document.getElementById('oddsDonutPercent'), data.winProbabilityPercent, { decimals: 1, suffix: '%' });
}

loadProjects();
initPageIntro('lottery', 'מחיר למשתכן - סיכויי זכייה',
  'כאן בודקים את הסיכוי הריאלי שלך לזכות בהגרלת מחיר למשתכן לפרויקט ספציפי. קודם מחשבים את הניקוד האישי שלך, ואז בוחרים פרויקט כדי לראות את הסיכוי המדויק - כולל השוואה לנתון ההיסטורי האמיתי של ההגרלה הזו, לא רק הערכה תיאורטית.');
