async function submitLead() {
  const fullName = document.getElementById('fullName').value.trim();
  const phone = document.getElementById('phone').value.trim();
  const resultEl = document.getElementById('leadResult');
  if (!validateFields([
    ['fullName', value => check.required('נא למלא שם')(value) || check.maxLength(300, 'השם ארוך מדי')(value)],
    ['phone', value => check.required('נא למלא מספר נייד')(value)
      || check.mobile('מספר נייד לא תקין - 10 ספרות שמתחילות ב-05, למשל 050-1234567')(value)],
    ['email', value => check.required('נא למלא כתובת אימייל')(value)
      || check.email('כתובת אימייל לא תקינה - למשל name@gmail.com')(value)],
    ['message', check.maxLength(300, 'ההודעה ארוכה מדי (עד 300 תווים)')]
  ])) return;
  resultEl.innerHTML = '';
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
