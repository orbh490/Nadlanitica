package com.nadlanitica.notifications;

import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * מייל לבעל האתר על שגיאת שרת לא צפויה (500) - אחרת היא נשארת רק בלוג של Render ואף אחד לא יודע עליה.
 * מייל אחד ביום לכל סוג שגיאה (סוג החריגה + המקום בקוד שבו נזרקה), כדי שתקלה שחוזרת בכל בקשה לא תציף את התיבה.
 */
@Component
public class ServerErrorAlerts {

    private static final int STACK_LINES = 8;

    private final NotificationEmailService emailService;
    private final DailyAlertGate dailyGate;

    public ServerErrorAlerts(NotificationEmailService emailService, DailyAlertGate dailyGate) {
        this.emailService = emailService;
        this.dailyGate = dailyGate;
    }

    public void report(String path, Throwable ex) {
        StackTraceElement origin = firstAppFrame(ex);
        String where = origin == null ? "?" : origin.getClassName() + "." + origin.getMethodName();
        if (!dailyGate.firstToday("server-error:" + ex.getClass().getSimpleName() + "@" + where)) {
            return;
        }
        String stack = Arrays.stream(ex.getStackTrace()).limit(STACK_LINES)
                .map(e -> "  at " + e).collect(Collectors.joining("\n"));
        emailService.sendAlert("שגיאת שרת באתר: " + ex.getClass().getSimpleName(),
                ("בקשה ל-%s נכשלה בשגיאה לא צפויה (המשתמש קיבל הודעת שגיאה כללית).%n%n"
                        + "סוג: %s%nהודעה: %s%nמקום: %s%n%n%s%n%n"
                        + "הפרטים המלאים בלוג של Render. מייל על אותה שגיאה יישלח לכל היותר פעם ביום.")
                        .formatted(path, ex.getClass().getName(), ex.getMessage(), where, stack));
    }

    private static StackTraceElement firstAppFrame(Throwable ex) {
        return Arrays.stream(ex.getStackTrace())
                .filter(e -> e.getClassName().startsWith("com.nadlanitica"))
                .findFirst()
                .orElse(ex.getStackTrace().length > 0 ? ex.getStackTrace()[0] : null);
    }
}
