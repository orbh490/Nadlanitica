package com.nadlanitica.notifications;

import com.nadlanitica.common.AppState;
import com.nadlanitica.common.AppStateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;

/**
 * התראה אחת ביום לכל נושא. בשרת החינמי (שנרדם כשאין ביקורים) הבדיקות רצות גם בכל התעוררות, ולא רק בשעה קבועה -
 * ובלי השער הזה כל התעוררות הייתה שולחת את אותה התראה שוב. התאריך האחרון נשמר במסד, כך שגם הפעלה מחדש לא מאפסת אותו.
 */
@Component
public class DailyAlertGate {

    private static final Logger log = LoggerFactory.getLogger(DailyAlertGate.class);

    private final AppStateRepository appState;
    private final Clock clock;

    @Autowired
    public DailyAlertGate(AppStateRepository appState) {
        this(appState, Clock.systemDefaultZone());
    }

    DailyAlertGate(AppStateRepository appState, Clock clock) {
        this.appState = appState;
        this.clock = clock;
    }

    /** true אם עוד לא נשלחה היום התראה בנושא הזה - ומסמן שנשלחה. */
    public boolean firstToday(String topic) {
        String key = "alert.last-sent." + topic;
        String today = LocalDate.now(clock).toString();
        try {
            if (appState.findById(key).map(s -> today.equals(s.getStateValue())).orElse(false)) {
                return false;
            }
            appState.save(new AppState(key, today));
        } catch (RuntimeException e) {
            // עדיף התראה כפולה על פני התראה שלא נשלחה בגלל תקלה במסד
            log.warn("שמירת מועד ההתראה '{}' נכשלה: {}", topic, e.getMessage());
        }
        return true;
    }
}
