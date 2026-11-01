# נדלניטיקה

כלים חינמיים לרוכשי דירה בישראל, במקום אחד ועל בסיס נתונים רשמיים:

- **משכנתא** – הון עצמי, מס רכישה, החזר חודשי, תמהיל מסלולים ורגישות לריבית (ריבית בנק ישראל נשלפת חיה)
- **מחיר למשתכן** – סיכויי זכייה בהגרלות לפי פרויקט, וסטטוס חי מלוח ההגרלות של משרד השיכון
- **מכרזי קרקעות** – מכרזי בנייה עצמית חיים מרשות מקרקעי ישראל, חוברות המכרז ומחשבון
- **לוח תוצאות** – השוואת ערים לפי סיכויי זכייה ומחירים
- **ייעוץ** – השארת פרטים ליועץ משכנתאות
- **אזור אישי** – הרשמה עם אימות מייל, היסטוריית חישובים ואזורי עניין

האתר החי: https://nadlanitica.onrender.com

## טכנולוגיות

Java 25, Spring Boot 4, Thymeleaf, Spring Data JPA, Flyway, PostgreSQL (באתר החי, על Neon) ו-H2 (בפיתוח ובבדיקות).
אפליקציה אחת: השרת מגיש גם את העמודים וגם את ה-API.

```
src/main/java/com/nadlanitica/
  auth/  account/  mortgage/  lottery/  landtender/  leads/  notifications/  common/
src/main/resources/
  templates/pages/    עמודים (נגישים בכתובות ‎.html)
  templates/fragments/ ראש עמוד וסרגלי ניווט משותפים
  static/css, static/js, static/js/pages
  db/migration/       מבנה מסד הנתונים (Flyway)
  data/               נתוני הגרלות היסטוריים
```

## הרצה מקומית

צריך רק JDK 25 (Maven מגיע עם הפרויקט):

```bash
./mvnw spring-boot:run
```

ב-Windows: `mvnw.cmd spring-boot:run`. האתר עולה ב-http://localhost:8080 עם מסד H2 בקובץ `./data` – בלי שום הגדרה.

בלי `MAIL_USERNAME`/`MAIL_PASSWORD` האתר עובד, רק לא שולח מיילים. **שימו לב:** אם הם מוגדרים במחשב, הרשמה מקומית שולחת מייל אמיתי.

## בדיקות

```bash
./mvnw test
```

הבדיקות רצות על H2 בזיכרון, ובדיקת המיגרציות רצה על PostgreSQL מוטמע (בלי Docker). הן אף פעם לא שולחות מיילים. ה-CI ב-GitHub מריץ אותן על כל Pull Request.

## משתני סביבה

הרשימה המלאה עם הסברים: [`.env.example`](.env.example). העיקריים באתר החי:

| משתנה | תפקיד |
|---|---|
| `SPRING_PROFILES_ACTIVE=production` | מסד PostgreSQL (מוגדר כבר ב-`Dockerfile`) |
| `PG_HOST`, `PG_DATABASE`, `PG_USER`, `PG_PASSWORD` | חיבור למסד |
| `MAIL_USERNAME`, `MAIL_PASSWORD` | שליחת מיילים |
| `ADMIN_KEY` | גישה לעמוד הלידים |
| `BASE_URL` | הכתובת הציבורית, לקישורים במיילים |

סיסמאות ומפתחות מגדירים רק בסביבה – אף פעם לא בקבצים בפרויקט.

## שינוי מבנה מסד הנתונים

לא משנים טבלה קיימת ולא סומכים על Hibernate (`ddl-auto=validate`). מוסיפים קובץ מיגרציה חדש:
`src/main/resources/db/migration/V3__תיאור.sql`. הוא ירוץ אוטומטית בעלייה הבאה, מקומית ובאתר החי.

## העלאה לאוויר

מדריך מלא: [`docs/DEPLOY.md`](docs/DEPLOY.md) (Render חינמי + Neon, בלי דומיין). כל push לענף שמחובר ל-Render מעלה גרסה חדשה.

## מצב ותוכנית עבודה

[`docs/STATUS_AND_WORK_PLAN.md`](docs/STATUS_AND_WORK_PLAN.md)

## מקורות נתונים

בנק ישראל, רשות מקרקעי ישראל, משרד הבינוי והשיכון, רשות המסים. הכלים להמחשה בלבד ואינם ייעוץ – ראו [גילוי נאות](https://nadlanitica.onrender.com/disclaimer.html).
