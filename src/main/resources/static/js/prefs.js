// העדפות תצוגה: מצב כהה והגדלת טקסט. נשמרות ב-localStorage ומוחלות בכל הדפים.
// נטען בלי defer כדי ש-window.displayPrefs יהיה זמין גם לסקריפטים שבסוף הדף (למשל הגדרות).
(function () {
  var KEY = 'displayPrefs', SCALE = 1.2, timer = null;

  function read() { try { return JSON.parse(localStorage.getItem(KEY) || '{}'); } catch (e) { return {}; } }
  function write(p) { try { localStorage.setItem(KEY, JSON.stringify(p)); } catch (e) { /* פרטיות/חסימה - ההעדפה פשוט לא נשמרת */ } }

  function applyDark(on) {
    if (on) document.documentElement.setAttribute('data-theme', 'dark');
    else document.documentElement.removeAttribute('data-theme');
  }

  // מחזיר כל טקסט למידה המקורית, ואז מגדיל. מודדים את כולם לפני שמשנים, כדי שלא יוכפל הגודל אצל צאצאים
  function scaleText(on) {
    var all = document.body.querySelectorAll('*');
    all.forEach(function (el) {
      if (el.hasAttribute('data-txt-orig')) {
        el.style.fontSize = el.getAttribute('data-txt-orig');
        el.removeAttribute('data-txt-orig');
      }
    });
    if (!on) return;
    var targets = [];
    all.forEach(function (el) {
      if (el.closest('svg, script, style, header.topbar, nav.m-tabs') || el.classList.contains('material-symbols-outlined')) return;
      var ownText = [].some.call(el.childNodes, function (n) { return n.nodeType === 3 && n.textContent.trim(); });
      if (!ownText && !el.matches('input, select, textarea')) return;
      targets.push({ el: el, base: parseFloat(getComputedStyle(el).fontSize) });
    });
    targets.forEach(function (t) {
      t.el.setAttribute('data-txt-orig', t.el.style.fontSize);
      t.el.style.fontSize = (t.base * SCALE) + 'px';
    });
  }

  function applyAll() {
    var p = read();
    applyDark(!!p.dark);
    scaleText(!!p.largeText);
  }

  window.displayPrefs = {
    get: read,
    set: function (key, value) { var p = read(); p[key] = value; write(p); applyAll(); }
  };

  document.addEventListener('DOMContentLoaded', function () {
    applyAll();
    // תוכן שנטען אחרי הדף (למשל טבלאות וכרטיסים) מקבל את אותו גודל
    new MutationObserver(function () {
      clearTimeout(timer);
      timer = setTimeout(function () { if (read().largeText) scaleText(true); }, 200);
    }).observe(document.body, { childList: true, subtree: true });
  });
})();
