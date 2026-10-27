async function submitLead() {
  const fullName = document.getElementById('fullName').value.trim();
  const phone = document.getElementById('phone').value.trim();
  const resultEl = document.getElementById('leadResult');
  if (!fullName || !phone) {
    resultEl.innerHTML = '<p class="form-error">נא למלא שם וטלפון</p>';
    return;
  }
  const body = {
    fullName,
    phone,
    email: document.getElementById('email').value.trim(),
    message: document.getElementById('message').value.trim()
  };
  // קודם ה"תודה" הוצג גם כשהשליחה נכשלה (למשל אחרי חסימת ספאם) - והליד פשוט אבד בלי שהמשתמש ידע
  try {
    await postJson('/api/leads', body);
  } catch (err) {
    resultEl.innerHTML = errorHtml(err);
    return;
  }
  resultEl.innerHTML = '<div class="badge ok">תודה! ניצור איתך קשר בקרוב</div>';
  document.getElementById('fullName').value = '';
  document.getElementById('phone').value = '';
  document.getElementById('email').value = '';
  document.getElementById('message').value = '';
}

initPageIntro('advisor', 'ייעוץ משכנתאות',
  'כאן אפשר להשאיר פרטים ולקבל תמהיל משכנתא מותאם אישית מיועץ מומחה. החיבור ליועץ דרכנו הוא בחינם, אבל הייעוץ עצמו עלול להיות כרוך בעמלה מטעם היועץ - כדאי לברר את זה ישירות מולו.');
