// ניווט נייד בלבד: החלקה לצדדים בין דפי הסרגל התחתון, וסימון שמחליק יחד עם המעבר
(function () {
  if (!matchMedia('(max-width: 700px)').matches) return;
  var nav = document.querySelector('nav.m-tabs');
  if (!nav) return;
  var tabs = [].slice.call(nav.querySelectorAll('a'));
  var cur = tabs.findIndex(function (a) { return a.classList.contains('active'); });
  if (cur < 0) return;

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
    var pg = document.querySelector('.page');
    if (pg) pg.classList.add(cur > from.from ? 'm-enter-fwd' : 'm-enter-back');
  }
  requestAnimationFrame(function () { place(cur); });

  function go(i) {
    try { sessionStorage.setItem('mtab', JSON.stringify({ from: cur })); } catch (e) {}
    location.href = tabs[i].href;
  }
  tabs.forEach(function (a, i) {
    a.addEventListener('click', function (e) { if (i === cur) return; e.preventDefault(); go(i); });
  });

  var sx, sy, st;
  document.addEventListener('touchstart', function (e) {
    var t = e.touches[0];
    sx = t.clientX; sy = t.clientY;
    st = e.target.closest('table,input,select,textarea,[data-noswipe]') ? null : Date.now();
  }, { passive: true });
  document.addEventListener('touchend', function (e) {
    if (st == null || document.querySelector('.intro-overlay')) return;
    var t = e.changedTouches[0], dx = t.clientX - sx, dy = t.clientY - sy;
    if (Math.abs(dx) < 70 || Math.abs(dy) > Math.abs(dx) * 0.6 || Date.now() - st > 800) return;
    var n = dx < 0 ? cur + 1 : cur - 1; // החלקה שמאלה = הבא (RTL)
    if (n >= 0 && n < tabs.length) go(n);
  }, { passive: true });
})();
