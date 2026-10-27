function paramId() {
  return new URLSearchParams(window.location.search).get('id');
}

async function load() {
  const id = paramId();
  if (!id) {
    document.getElementById('status').textContent = 'לא צוין מכרז.';
    return;
  }
  try {
    const detail = await apiFetch(`/api/land-tenders/${encodeURIComponent(id)}`);
    document.getElementById('tenderHeader').textContent = `${detail.tenderNumber} - ${detail.city}${detail.neighborhood ? ' (' + detail.neighborhood + ')' : ''}`;
    document.getElementById('bookletLinkWrap').innerHTML = detail.bookletAvailable
      ? `<a href="/api/land-tenders/${encodeURIComponent(id)}/booklet" target="_blank" class="secondary" style="display:inline-block;text-decoration:none">📄 צפייה בחוברת המכרז המלאה (PDF מקורי מרשות מקרקעי ישראל)</a>`
      : '';
  } catch (e) {
    document.getElementById('tenderHeader').textContent = '';
  }

  const statusEl = document.getElementById('status');
  const snippetsEl = document.getElementById('snippets');
  try {
    const info = await apiFetch(`/api/land-tenders/${encodeURIComponent(id)}/discounts`);
    if (!info.bookletAvailable) {
      statusEl.textContent = 'חוברת המכרז טרם פורסמה במערכת רמ"י.';
      return;
    }
    if (info.discountSnippets.length === 0) {
      statusEl.textContent = 'לא נמצאו אוטומטית קטעים על הנחות בחוברת - ייתכן שהמידע קיים בניסוח שונה. מומלץ לעיין בחוברת המלאה למעלה.';
      return;
    }
    statusEl.textContent = `נמצאו ${info.discountSnippets.length} קטעים בחוברת שמזכירים הנחה ליד אוכלוסיית עדיפות:`;
    snippetsEl.innerHTML = info.discountSnippets.map(s => `<p style="font-size:13px;background:#f7f8fb;border-radius:8px;padding:10px 12px;margin-top:8px">${escapeHtml(s)}</p>`).join('');
  } catch (err) {
    statusEl.textContent = err.message;
  }
}

load();
