// ניווט נייד בלבד: החלקה לצדדים בין חמשת דפי הסרגל התחתון, וסימון שזז יחד עם הדף
(function () {
  if (!matchMedia('(max-width: 700px)').matches) return;
  var nav = document.querySelector('nav.m-tabs');
  if (!nav) return;
  var tabs = [].slice.call(nav.querySelectorAll('a'));
  var cur = tabs.findIndex(function (a) { return a.classList.contains('active'); });
  if (cur < 0) return; // החלקה רק בחמשת הדפים הראשיים
  var page = document.querySelector('.page');

  var ind = document.createElement('span');
  ind.className = 'm-ind';
  nav.insertBefore(ind, nav.firstChild);

  // מיקום הסימון: על לשונית i, או בדרך מ-i ל-j (t בין 0 ל-1) בזמן החלקה
  function place(i, j, t) {
    var a = tabs[i], b = tabs[j == null ? i : j];
    t = t || 0;
    ind.style.left = a.offsetLeft + (b.offsetLeft - a.offsetLeft) * t + 'px';
    ind.style.width = a.offsetWidth + (b.offsetWidth - a.offsetWidth) * t + 'px';
  }
  function animated(on) { ind.style.transition = on ? '' : 'none'; }

  // הסרגל משנה רוחב אחרי הטעינה (גופן האייקונים מגיע מאוחר, עיצובים נטענים) - בכל שינוי ממקמים שוב, בלי אנימציה
  function settle() { if (mode === 'h' || leaving) return; animated(false); place(cur); }
  settle();
  if (window.ResizeObserver) { var ro = new ResizeObserver(settle); ro.observe(nav); tabs.forEach(function (a) { ro.observe(a); }); }
  if (document.fonts) document.fonts.ready.then(settle);
  addEventListener('load', settle);

  var cameFrom = null;
  try { cameFrom = JSON.parse(sessionStorage.getItem('mtab') || 'null'); sessionStorage.removeItem('mtab'); } catch (e) {}
  if (cameFrom && page && cameFrom.from !== cur) page.classList.add(cameFrom.from < cur ? 'm-enter-fwd' : 'm-enter-back');

  // הדפים השכנים נטענים מראש, כדי שהמעבר יהיה מהיר
  [cur - 1, cur + 1].forEach(function (i) {
    if (!tabs[i]) return;
    var l = document.createElement('link');
    l.rel = 'prefetch'; l.href = tabs[i].href;
    document.head.appendChild(l);
  });

  var leaving = false;
  // מעבר לדף i: הסימון מחליק ללשונית החדשה והדף יוצא לכיוון ההחלקה, ורק אז עוברים
  function go(i) {
    if (leaving) return;
    leaving = true;
    try { sessionStorage.setItem('mtab', JSON.stringify({ from: cur })); } catch (e) {}
    animated(true);
    place(i);
    if (page) {
      page.style.transition = 'transform .25s ease, opacity .25s ease';
      // הדף הבא נמצא משמאל (RTL), אז הדף הנוכחי יוצא ימינה - ולהפך
      page.style.transform = 'translateX(' + (i > cur ? 1 : -1) * innerWidth * 0.5 + 'px)';
      page.style.opacity = '.35';
    }
    setTimeout(function () { location.href = tabs[i].href; }, 250);
  }
  tabs.forEach(function (a, i) {
    a.addEventListener('click', function (e) { if (i === cur) return; e.preventDefault(); go(i); });
  });

  // חזרה בכפתור "אחורה" משחזרת את הדף מהזיכרון כמו שהיה - כולל ההזזה והסימון
  addEventListener('pageshow', function (e) {
    if (!e.persisted) return;
    leaving = false;
    if (page) { page.style.transition = 'none'; page.style.transform = ''; page.style.opacity = ''; }
    animated(false);
    place(cur);
  });

  // אזורים שבהם תנועה לצד שייכת לתוכן: שדות, טבלאות, גרפים וכל מה שנגלל לרוחב
  function ownsHorizontalDrag(el) {
    if (el.closest('input,select,textarea,canvas,table,[data-noswipe]')) return true;
    for (; el && el !== document.body; el = el.parentElement) {
      if (el.scrollWidth > el.clientWidth + 1 && /(auto|scroll)/.test(getComputedStyle(el).overflowX)) return true;
    }
    return false;
  }

  // החלקה ימינה = הדף שמשמאל (הבא בסרגל), החלקה שמאלה = הדף שמימין. אין דף בכיוון הזה - לא זז בכלל
  function target(dx) { var n = dx > 0 ? cur + 1 : cur - 1; return tabs[n] ? n : -1; }

  var EDGE = 24; // בקצוות המסך הטלפון עצמו מזהה החלקה כ"אחורה" - לא מתחרים בו
  var sx, sy, st, dx, mode; // mode: null = עוד לא הוחלט, 'h' = החלקה בין דפים, 'off' = לא שלנו
  document.addEventListener('touchstart', function (e) {
    mode = 'off';
    if (leaving || e.touches.length !== 1 || document.querySelector('.intro-overlay')) return;
    var t = e.touches[0];
    if (t.clientX < EDGE || t.clientX > innerWidth - EDGE) return;
    if (ownsHorizontalDrag(e.target)) return;
    sx = t.clientX; sy = t.clientY; st = Date.now(); dx = 0; mode = null;
  }, { passive: true });

  document.addEventListener('touchmove', function (e) {
    if (mode === 'off') return;
    if (e.touches.length !== 1) { snapBack(); mode = 'off'; return; }
    var t = e.touches[0], mx = t.clientX - sx, my = t.clientY - sy;
    if (mode === null) {
      if (Math.abs(mx) < 10 && Math.abs(my) < 10) return;
      // גלילה רגילה למעלה/למטה - לא נוגעים בה עד סוף המגע הזה
      mode = Math.abs(mx) > Math.abs(my) * 1.3 ? 'h' : 'off';
      if (mode === 'off') return;
      animated(false);
      if (page) page.style.transition = 'none';
    }
    dx = mx;
    var n = target(dx);
    if (n < 0) { dx = 0; } // אין לאן לעבור
    var progress = Math.min(Math.abs(dx) / innerWidth, 1);
    if (page) {
      page.style.transform = dx ? 'translateX(' + dx + 'px)' : '';
      page.style.opacity = dx ? String(1 - progress * 0.6) : '';
    }
    if (n >= 0) place(cur, n, progress); else place(cur);
  }, { passive: true });

  function snapBack() {
    animated(true);
    place(cur);
    if (!page) return;
    page.style.transition = 'transform .25s ease, opacity .25s ease';
    page.style.transform = '';
    page.style.opacity = '';
  }

  document.addEventListener('touchend', function () {
    if (mode !== 'h') { mode = 'off'; return; }
    mode = 'off';
    var n = target(dx);
    var fast = Math.abs(dx) > 50 && Date.now() - st < 300;
    if (dx && n >= 0 && (Math.abs(dx) > innerWidth * 0.25 || fast)) go(n);
    else snapBack();
  }, { passive: true });
  document.addEventListener('touchcancel', function () { if (mode === 'h') snapBack(); mode = 'off'; }, { passive: true });
})();
