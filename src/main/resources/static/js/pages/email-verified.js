  const token = new URLSearchParams(window.location.search).get('token');
  const hasError = new URLSearchParams(window.location.search).has('error');
  document.getElementById(hasError || !token ? 'errorCard' : 'confirmCard').style.display = 'block';

  async function confirmVerify() {
    const msgEl = document.getElementById('resultMessage');
    msgEl.style.color = '#8b93a6';
    msgEl.textContent = 'רגע...';
    const res = await fetch('/api/auth/verify-confirm', {
      method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ token })
    });
    if (!res.ok) {
      document.getElementById('confirmCard').style.display = 'none';
      document.getElementById('errorCard').style.display = 'block';
      return;
    }
    document.getElementById('confirmCard').style.display = 'none';
    document.getElementById('successCard').style.display = 'block';
  }
