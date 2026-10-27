const KEY_STORAGE = 'financialCompassAdminKey';
document.getElementById('adminKey').value = sessionStorage.getItem(KEY_STORAGE) || '';

function currentKey() {
  const key = document.getElementById('adminKey').value.trim();
  sessionStorage.setItem(KEY_STORAGE, key);
  return key;
}

function escapeHtml(value) {
  const div = document.createElement('div');
  div.textContent = value || '';
  return div.innerHTML;
}

async function loadLeads() {
  const statusEl = document.getElementById('status');
  statusEl.textContent = '';
  const res = await fetch('/api/admin/leads', { headers: { 'X-Admin-Key': currentKey() } });
  if (!res.ok) {
    statusEl.textContent = res.status === 403 ? 'מפתח שגוי' : 'שגיאה בטעינה';
    document.getElementById('leadsTable').style.display = 'none';
    return;
  }
  const leads = await res.json();
  const body = document.getElementById('leadsBody');
  // escapeHtml על כל שדה - הלידים מגיעים מטופס ציבורי לא-מאומת, בלי זה זה stored XSS.
  body.innerHTML = leads.map(l => `
    <tr>
      <td>${escapeHtml(new Date(l.createdAt).toLocaleString('he-IL'))}</td>
      <td>${escapeHtml(l.fullName)}</td>
      <td>${escapeHtml(l.phone)}</td>
      <td>${escapeHtml(l.email)}</td>
      <td>${escapeHtml(l.message)}</td>
    </tr>`).join('');
  document.getElementById('leadsTable').style.display = leads.length ? 'table' : 'none';
  if (!leads.length) statusEl.textContent = 'אין עדיין לידים';
}

async function exportCsv() {
  const statusEl = document.getElementById('status');
  statusEl.textContent = '';
  const res = await fetch('/api/admin/leads/export.csv', { headers: { 'X-Admin-Key': currentKey() } });
  if (!res.ok) {
    statusEl.textContent = res.status === 403 ? 'מפתח שגוי' : 'שגיאה בייצוא';
    return;
  }
  const blob = await res.blob();
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = 'leads.csv';
  a.click();
  URL.revokeObjectURL(url);
}
