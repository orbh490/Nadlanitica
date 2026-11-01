// טופס ההתחברות/הרשמה עצמו ב-auth-form.js; כאן רק מה שייחודי לעמוד הכניסה
async function onLoginSuccess() {
  window.location.href = '/home.html';
}

function continueAsGuest() {
  window.location.href = '/home.html';
}

(async () => {
  const me = await fetchCurrentUser();
  if (me.loggedIn) window.location.href = '/home.html';
})();
