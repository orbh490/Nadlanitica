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

let landTabLoaded = false;
function switchTab(tab) {
  document.getElementById('lotteryTab').style.display = tab === 'lottery' ? 'block' : 'none';
  document.getElementById('landTab').style.display = tab === 'land' ? 'block' : 'none';
  document.getElementById('tabBtnLottery').classList.toggle('active', tab === 'lottery');
  document.getElementById('tabBtnLand').classList.toggle('active', tab === 'land');
  if (tab === 'land' && !landTabLoaded) {
    landTabLoaded = true;
    loadLandCityResults();
  }
}

// ---- תרשים עמודות אופקי גנרי - ערים ממוינות, ערך אחד לעמודה, טבלה מלאה מתקפלת ----
function renderHorizontalBarChart(containerId, items, opts) {
  const container = document.getElementById(containerId);
  if (items.length === 0) {
    container.innerHTML = '<p style="color:#6B8193;font-size:13px">אין מספיק נתונים</p>';
    return;
  }
  // רוחב ה-viewBox נמדד מהקונטיינר בפועל (לא קבוע) כדי שגודל הטקסט יישאר קריא במסך צר - אחרת ה-SVG
  // מוקטן כמכלול על ידי width:100%, וכל טקסט "בפנים" מתכווץ יחד איתו עד שהוא בלתי קריא בנייד.
  const width = Math.max(280, Math.round(container.clientWidth || 680));
  const isNarrow = width < 420;
  const rowH = 28, marginLeft = isNarrow ? 76 : 130, marginRight = isNarrow ? 44 : 60, marginTop = 6;
  const cityFontSize = isNarrow ? 10 : 12;
  const maxCityChars = isNarrow ? 9 : 18;
  const height = marginTop + items.length * rowH + 10;
  const plotW = width - marginLeft - marginRight;
  const maxValue = niceMax(Math.max(...items.map(opts.value)));

  const svg = svgEl('svg', { viewBox: `0 0 ${width} ${height}`, width: '100%', height, role: 'img', 'aria-label': opts.ariaLabel });

  items.forEach((item, i) => {
    const y = marginTop + i * rowH;
    const barH = 18;
    const value = opts.value(item);
    const barLen = (value / maxValue) * plotW;

    const cityLabel = svgEl('text', { class: 'axis-label', x: marginLeft - 10, y: y + barH / 2 + 4, 'text-anchor': 'end', direction: 'ltr', style: `font-size:${cityFontSize}px` });
    const cityName = opts.city(item);
    cityLabel.textContent = cityName.length > maxCityChars ? cityName.slice(0, maxCityChars - 1) + '…' : cityName;
    const cityTitle = svgEl('title', {});
    cityTitle.textContent = cityName;
    cityLabel.appendChild(cityTitle);
    svg.appendChild(cityLabel);

    const rect = svgEl('rect', { class: 'bar-single', x: marginLeft, y, width: Math.max(1, barLen), height: barH, rx: 6, fill: 'var(--series-single)' });
    rect.addEventListener('pointermove', evt => showTooltip(evt, opts.tooltip(item)));
    rect.addEventListener('pointerleave', hideTooltip);
    svg.appendChild(rect);

    const valueLabel = svgEl('text', { class: 'value-label', x: marginLeft + barLen + 10, y: y + barH / 2 + 4, 'text-anchor': 'start', direction: 'ltr', style: `font-size:${isNarrow ? 11 : 12}px` });
    valueLabel.textContent = opts.valueLabel(item);
    svg.appendChild(valueLabel);
  });

  container.innerHTML = '';
  container.appendChild(svg);

  const details = document.createElement('details');
  details.className = 'table-toggle';
  details.innerHTML = `<summary>הצג טבלה מלאה</summary>
    <div style="overflow-x:auto"><table>
      <tr>${opts.tableHeaders.map(h => `<th>${h}</th>`).join('')}</tr>
      ${items.map(item => `<tr>${opts.tableRow(item).map(c => `<td>${c}</td>`).join('')}</tr>`).join('')}
    </table></div>`;
  container.appendChild(details);
}

async function loadCityComparison() {
  const localResident = document.getElementById('cityCompareLocal').checked;
  const cities = await (await fetch(`/api/lottery/city-comparison?localResident=${localResident}&minProjects=3&limit=15`)).json();
  renderHorizontalBarChart('cityComparisonChart', cities, {
    ariaLabel: 'השוואת סיכויי זכייה ממוצעים לפי עיר',
    value: c => c.avgOddsPercent,
    city: c => c.city,
    valueLabel: c => c.avgOddsPercent.toFixed(1) + '%',
    tooltip: c => `<b>${c.city}</b><br>סיכוי ממוצע: <b>${c.avgOddsPercent.toFixed(1)}%</b><br>מחיר ממוצע למ"ר: <b>${c.avgPricePerSqm > 0 ? Math.round(c.avgPricePerSqm).toLocaleString() : '—'}</b><br>מס' פרויקטים: <b>${c.projectCount}</b>`,
    tableHeaders: ['עיר', 'סיכוי ממוצע', 'מחיר ממוצע למ"ר', "מס' פרויקטים"],
    tableRow: c => [c.city, c.avgOddsPercent.toFixed(1) + '%', c.avgPricePerSqm > 0 ? Math.round(c.avgPricePerSqm).toLocaleString() : '—', c.projectCount]
  });
}

async function loadLandCityResults() {
  const statusEl = document.getElementById('landResultsStatus');
  statusEl.textContent = 'טוען תוצאות אמיתיות ממכרזי רמ"י (יכול לקחת כמה שניות בפעם הראשונה)...';
  try {
    const cities = await (await fetch('/api/land-tenders/city-results')).json();
    statusEl.textContent = cities.length > 0
      ? `מבוסס על ${cities.reduce((a, c) => a + c.decidedPlots, 0)} מגרשים שהוכרעו בפועל, ב-${cities.length} ערים.`
      : 'עדיין אין מספיק תוצאות מוכרעות זמינות.';
    renderHorizontalBarChart('landResultsChart', cities, {
      ariaLabel: 'השוואת מחיר סופי ממוצע למ"ר לפי עיר',
      value: c => c.avgPricePerSqm,
      city: c => c.city,
      valueLabel: c => Math.round(c.avgPricePerSqm).toLocaleString(),
      tooltip: c => `<b>${c.city}</b><br>מחיר ממוצע למ"ר: <b>${Math.round(c.avgPricePerSqm).toLocaleString()} ש"ח</b><br>מחיר סופי ממוצע למגרש: <b>${Math.round(c.avgFinalPrice).toLocaleString()} ש"ח</b><br>מגרשים שהוכרעו: <b>${c.decidedPlots}</b>`,
      tableHeaders: ['עיר', 'מחיר ממוצע למ"ר', 'מחיר סופי ממוצע למגרש', 'מגרשים שהוכרעו'],
      tableRow: c => [c.city, Math.round(c.avgPricePerSqm).toLocaleString(), Math.round(c.avgFinalPrice).toLocaleString(), c.decidedPlots]
    });
  } catch (e) {
    statusEl.textContent = 'לא הצלחנו לטעון תוצאות מכרזי קרקע כרגע.';
  }
}

loadCityComparison();
initPageIntro('results-board', 'לוח תוצאות',
  'השוואה בין ערים לפי סיכויי זכייה ממוצעים ומחירים בפרויקטי מחיר למשתכן ומכרזי קרקע, כדי לעזור להחליט איפה כדאי להירשם.');
