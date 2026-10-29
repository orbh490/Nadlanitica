-- ערכים קטנים שהאפליקציה צריכה לזכור בין הפעלות (בשרת החינמי השרת נרדם ומופעל מחדש הרבה).
-- למשל: ריבית בנק ישראל האחרונה שנשלפה, כדי לשלוח התראה רק כשהיא באמת משתנה.
CREATE TABLE app_state (
    state_key   VARCHAR(100)                NOT NULL PRIMARY KEY,
    state_value VARCHAR(500)                NOT NULL,
    updated_at  TIMESTAMP(6) WITH TIME ZONE NOT NULL
);
