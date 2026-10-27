async function submitLead() {
  const fullName = document.getElementById('fullName').value.trim();
  const phone = document.getElementById('phone').value.trim();
  const resultEl = document.getElementById('leadResult');
  if (!fullName || !phone) {
    resultEl.innerHTML = '<p style="color:#B3261E;font-size:13px">נא למלא שם וטלפון</p>';
    return;
  }
  const body = {
    fullName,
    phone,
    email: document.getElementById('email').value.trim(),
    message: document.getElementById('message').value.trim()
  };
  await fetch('/api/leads', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body)
  });
  resultEl.innerHTML = '<div class="badge ok">תודה! ניצור איתך קשר בקרוב</div>';
  document.getElementById('fullName').value = '';
  document.getElementById('phone').value = '';
  document.getElementById('email').value = '';
  document.getElementById('message').value = '';
}

initPageIntro('advisor', 'ייעוץ משכנתאות',
  'כאן אפשר להשאיר פרטים ולקבל תמהיל משכנתא מותאם אישית מיועץ מומחה. החיבור ליועץ דרכנו הוא בחינם, אבל הייעוץ עצמו עלול להיות כרוך בעמלה מטעם היועץ - כדאי לברר את זה ישירות מולו.');
