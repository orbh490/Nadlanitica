// ניווט נייד בלבד: החלקה לצדדים בין דפי הסרגל התחתון, וסימון שמחליק יחד עם המעבר
(function () {
  if (!matchMedia('(max-width: 700px)').matches) return;
  var nav = document.querySelector('nav.m-tabs');
  if (!nav) return;
  var tabs = [].slice.call(nav.querySelectorAll('a'));
  var cur = tabs.findIndex(function (a) { return a.classList.contains('active'); });
  if (cur < 0) return;
  var page = document.querySelector('.page');

  var ind = document.createElement('span');
  ind.className = 'm-ind';
  nav.insertBefore(ind, nav.firstChild);
  function place(i) { ind.style.left = tabs[i].offsetLeft + 'px'; ind.style.width = tabs[i].offsetWidth + 'px'; }

  var from = null;
  try { from = JSON.parse(sessionStorage.getItem('mtab') || 'null'); sessionStorage.removeItem('mtab'); } catch (e) {}
  if (from && from.from >= 0 && from.from !== cur) {
    ind.style.transition = 'none';
    place(from.from);
    void ind.offsetWidth; // מאלץ ציור של המיקום הישן לפני האנימציה
    ind.style.transition = '';
    if (page) page.classList.add(cur > from.from ? 'm-enter-fwd' : 'm-enter-back');
  }
  requestAnimationFrame(function () { place(cur); });

  // הדפים השכנים נטענים מראש, כדי שהמעבר בהחלקה יהיה מיידי ולא יחכה לשרת
  [cur - 1, cur + 1].forEach(function (i) {
    if (i < 0 || i >= tabs.length) return;
    var l = document.createElement('link');
    l.rel = 'prefetch'; l.href = tabs[i].href;
    document.head.appendChild(l);
  });

  function go(i) {
    try { sessionStorage.setItem('mtab', JSON.stringify({ from: cur })); } catch (e) {}
    location.href = tabs[i].href;
  }
  tabs.forEach(function (a, i) {
    a.addEventListener('click', function (e) { if (i === cur) return; e.preventDefault(); go(i); });
  });

  // חזרה בכפתור "אחורה" של הדפדפן משחזרת את הדף מהזיכרון כמו שהיה - כולל ההזזה מההחלקה
  addEventListener('pageshow', function (e) { if (e.persisted && page) { page.style.transition = 'none'; page.style.transform = ''; page.style.opacity = ''; } });

  // אזורים שבהם תנועה לצד שייכת לתוכן: שדות, טבלאות וגרפים, וכל מה שנגלל לרוחב
  function ownsHorizontalDrag(el) {
    if (el.closest('input,select,textarea,canvas,table,[data-noswipe]')) return true;
    for (; el && el !== document.body; el = el.parentElement) {
      if (el.scrollWidth > el.clientWidth + 1 && /(auto|scroll)/.test(getComputedStyle(el).overflowX)) return true;
    }
    return false;
  }

  var EDGE = 24; // בקצוות המסך המערכת עצמה מזהה החלקה כ"אחורה" - לא מתחרים בה
  var sx, sy, st, dx, mode; // mode: null = עוד לא הוחלט, 'h' = החלקה בין דפים, 'off' = לא שלנו
  document.addEventListener('touchstart', function (e) {
    mode = 'off';
    if (e.touches.length !== 1 || document.querySelector('.intro-overlay')) return;
    var t = e.touches[0];
    if (t.clientX < EDGE || t.clientX > innerWidth - EDGE) return;
    if (ownsHorizontalDrag(e.target)) return;
    sx = t.clientX; sy = t.clientY; st = Date.now(); dx = 0; mode = null;
  }, { passive: true });

  document.addEventListener('touchmove', function (e) {
    if (mode === 'off' || !page) return;
    if (e.touches.length !== 1) { snapBack(); mode = 'off'; return; }
    var t = e.touches[0], mx = t.clientX - sx, my = t.clientY - sy;
    if (mode === null) {
      if (Math.abs(mx) < 10 && Math.abs(my) < 10) return;
      // גלילה רגילה למעלה/למטה - לא נוגעים בה עד סוף המגע הזה
      mode = Math.abs(mx) > Math.abs(my) * 1.3 ? 'h' : 'off';
      if (mode === 'off') return;
      page.style.transition = 'none';
    }
    dx = mx;
    var n = dx < 0 ? cur + 1 : cur - 1; // החלקה שמאלה = הבא (RTL)
    var edge = n < 0 || n >= tabs.length;
    // אין דף בכיוון הזה: הדף זז מעט ונעצר, כדי שיהיה ברור שזה הסוף
    var shift = edge ? dx / 4 : dx * 0.6;
    page.style.transform = 'translateX(' + shift + 'px)';
    page.style.opacity = edge ? '' : String(1 - Math.min(Math.abs(dx) / innerWidth, 0.5));
  }, { passive: true });

  function snapBack() {
    if (!page) return;
    page.style.transition = 'transform .25s ease, opacity .25s ease';
    page.style.transform = '';
    page.style.opacity = '';
  }

  function endSwipe() {
    if (mode !== 'h') { mode = 'off'; return; }
    mode = 'off';
    var n = dx < 0 ? cur + 1 : cur - 1;
    var fast = Math.abs(dx) > 50 && Date.now() - st < 300;
    if (n >= 0 && n < tabs.length && (Math.abs(dx) > innerWidth * 0.25 || fast)) {
      page.style.transition = 'transform .2s ease, opacity .2s ease';
      page.style.transform = 'translateX(' + (dx < 0 ? -1 : 1) * innerWidth * 0.4 + 'px)';
      page.style.opacity = '.35'; // לא שקוף לגמרי: אם השרת איטי, לא נשאר מסך ריק
      go(n);
    } else {
      snapBack();
    }
  }
  document.addEventListener('touchend', endSwipe, { passive: true });
  document.addEventListener('touchcancel', function () { if (mode === 'h') snapBack(); mode = 'off'; }, { passive: true });
})();
