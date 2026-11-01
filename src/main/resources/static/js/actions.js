// חיבור כפתורים ושדות לפונקציות בלי onclick="..." בתוך ה-HTML (ה-CSP חוסם קוד שכתוב בתוך הדף):
//   data-click="fn"  - לחיצה קוראת ל-fn()
//   data-change="fn" - שינוי ערך קורא ל-fn()
//   data-arg="x"     - מועבר ל-fn כארגומנט (מחרוזת)
// הפונקציות הן הפונקציות הגלובליות שבקובצי ה-JS של כל דף. האזנה אחת על המסמך תופסת גם כפתורים שנוצרים אחר כך מ-innerHTML.
(function () {
  function run(attr, e) {
    var el = e.target.closest('[' + attr + ']');
    if (!el) return;
    var fn = window[el.getAttribute(attr)];
    if (typeof fn !== 'function') return;
    if (el.tagName === 'A') e.preventDefault(); // קישורים כמו "שכחתי סיסמה" הם פעולה, לא מעבר דף
    var arg = el.getAttribute('data-arg');
    arg === null ? fn() : fn(arg);
  }
  document.addEventListener('click', function (e) { run('data-click', e); });
  document.addEventListener('change', function (e) { run('data-change', e); });
})();

// לכפתור "רענון הדף" בעמוד השגיאה, שאין לו קובץ JS משלו
function reloadPage() { location.reload(); }
