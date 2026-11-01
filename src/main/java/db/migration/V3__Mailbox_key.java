package db.migration;

import com.nadlanitica.auth.MailboxKey;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * עמודת mailbox_key ב-app_user: התיבה האמיתית מאחורי הכתובת (ראו MailboxKey), כדי לחסום חשבון שני על אותה תיבה
 * עם orbh+1@ או נקודות בג'ימייל. מיגרציה ב-Java ולא ב-SQL כי צריך לחשב את המפתח למשתמשים הקיימים באותו כלל בדיוק.
 * בכוונה בלי UNIQUE: ייתכן שכבר קיימים במסד שני חשבונות על אותה תיבה (מלפני השינוי), ולא מוחקים חשבונות אוטומטית.
 * החסימה נעשית ב-AuthService בזמן ההרשמה.
 */
public class V3__Mailbox_key extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection c = context.getConnection();
        try (Statement st = c.createStatement()) {
            st.execute("ALTER TABLE app_user ADD COLUMN mailbox_key VARCHAR(254)");
        }
        try (Statement select = c.createStatement();
             ResultSet rows = select.executeQuery("SELECT id, email FROM app_user");
             PreparedStatement update = c.prepareStatement("UPDATE app_user SET mailbox_key = ? WHERE id = ?")) {
            while (rows.next()) {
                update.setString(1, MailboxKey.of(rows.getString("email")));
                update.setString(2, rows.getString("id"));
                update.executeUpdate();
            }
        }
        try (Statement st = c.createStatement()) {
            st.execute("ALTER TABLE app_user ALTER COLUMN mailbox_key SET NOT NULL");
            st.execute("CREATE INDEX ix_app_user_mailbox_key ON app_user (mailbox_key)");
        }
    }
}
