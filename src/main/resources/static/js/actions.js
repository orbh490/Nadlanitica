// חיבור כפתורים ושדות לפונקציות בלי onclick="..." בתוך ה-HTML (ה-CSP חוסם קוד שכתוב בתוך הדף):
//   data-click="fn"  - לחיצה קוראת ל-fn()
//   data-change="fn" - שינוי ערך קורא ל-fn()
//   data-arg="x"     - מועבר ל-fn כארגומנט (מחרוזת)
//   data-result="id" - בטלפון, אחרי שהחישוב מסתיים גוללים אל התוצאה (אחרת היא מופיעה מתחת למסך ולא רואים שקרה משהו)
// הפונקציות הן הפונקציות הגלובליות שבקובצי ה-JS של כל דף. האזנה אחת על המסמך תופסת גם כפתורים שנוצרים אחר כך מ-innerHTML.
(function () {
  function run(attr, e) {
    var el = e.target.closest('[' + attr + ']');
    if (!el) return;
    var fn = window[el.getAttribute(attr)];
    if (typeof fn !== 'function') return;
    if (el.tagName === 'A') e.preventDefault(); // קישורים כמו "שכחתי סיסמה" הם פעולה, לא מעבר דף
    var arg = el.getAttribute('data-arg');
    var done = arg === null ? fn() : fn(arg);
    var resultId = el.getAttribute('data-result');
    if (resultId) Promise.resolve(done).then(function () { scrollToResult(resultId); });
  }

  function scrollToResult(id) {
    if (!matchMedia('(max-width: 700px)').matches) return;
    // שדה עם שגיאה = החישוב לא רץ, וההודעה ליד השדה היא מה שצריך לראות
    if (document.querySelector('[aria-invalid="true"]')) return;
    var target = document.getElementById(id);
    if (!target) return;
    // יש חישובים שמציירים את התוצאה רגע אחרי שהפונקציה "מסתיימת" - מחכים לתוכן (עד 10 שניות) ואז גוללים
    if (!target.offsetParent || !target.textContent.trim()) {
      var observer = new MutationObserver(function () {
        if (target.offsetParent && target.textContent.trim()) { observer.disconnect(); scrollToResult(id); }
      });
      observer.observe(target, { childList: true, subtree: true, characterData: true, attributes: true });
      setTimeout(function () { observer.disconnect(); }, 10000);
      return;
    }
    var top = target.getBoundingClientRect().top;
    if (top > 80 && top < innerHeight * 0.55) return; // כבר נראית היטב
    var still = matchMedia('(prefers-reduced-motion: reduce)').matches;
    target.scrollIntoView({ behavior: still ? 'auto' : 'smooth', block: 'start' });
  }
  document.addEventListener('click', function (e) { run('data-click', e); });
  document.addEventListener('change', function (e) { run('data-change', e); });
})();

// לכפתור "רענון הדף" בעמוד השגיאה, שאין לו קובץ JS משלו
function reloadPage() { location.reload(); }
