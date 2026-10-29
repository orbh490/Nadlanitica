package com.nadlanitica.mortgage;

import com.nadlanitica.common.AppState;
import com.nadlanitica.common.AppStateRepository;
import com.nadlanitica.notifications.NotificationEmailService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Instant;
import java.util.Optional;

/**
 * ריבית בנק ישראל (הריבית הבסיסית שממנה נגזר מסלול הפריים) - נשלפת חיה מה-API הציבורי הרשמי של בנק ישראל
 * (https://boi.org.il/PublicApi/GetInterest, נבדק ואומת ידנית - endpoint לא מתועד באתר עצמו אך פתוח וללא אימות).
 * מתעדכנת פעם ביום: בנק ישראל משנה את הריבית הזו רק בהחלטות ריבית מתוזמנות (כ-8 בשנה), אז תדירות גבוהה יותר מיותרת.
 * אם השליפה נכשלת (השרת למטה, שינוי ב-API וכו') - נשאר הערך התקף האחרון שהצלחנו לשלוף, ואם עוד לא הצלחנו
 * אף פעם - נופלים לערך ברירת מחדל שאומת ידנית, כדי שהאפליקציה לעולם לא תישבר בגלל תלות חיצונית זו.
 * הערך האחרון נשמר במסד הנתונים (app_state): כך אפשר לשלוח התראה כשהריבית באמת משתנה, גם אחרי שהשרת הופעל מחדש.
 */
@Service
public class BoiRateService {

    private static final Logger log = LoggerFactory.getLogger(BoiRateService.class);
    private static final double FALLBACK_BASE_RATE_PERCENT = 3.5; // אומת ידנית לאחרונה - ראו זיכרון הפרויקט
    private static final String RATE_STATE_KEY = "boi.base-rate-percent";

    private final RestClient restClient = RestClient.create("https://boi.org.il");
    private final NotificationEmailService emailService;
    private final AppStateRepository appState;

    private volatile double currentBaseRatePercent = FALLBACK_BASE_RATE_PERCENT;
    private volatile Instant lastSuccessfulFetch;

    public BoiRateService(NotificationEmailService emailService, AppStateRepository appState) {
        this.emailService = emailService;
        this.appState = appState;
    }

    record BoiInterestResponse(Double currentInterest) {
    }

    public double currentBaseRatePercent() {
        return currentBaseRatePercent;
    }

    public Instant lastSuccessfulFetch() {
        return lastSuccessfulFetch;
    }

    @PostConstruct
    void fetchOnStartup() {
        // אם בנק ישראל לא זמין כרגע - עדיף הערך האחרון שנשמר מאשר ברירת המחדל הקבועה בקוד
        lastKnownRate().ifPresent(rate -> currentBaseRatePercent = rate);
        refresh();
    }

    @Scheduled(cron = "0 15 5 * * *")
    void refresh() {
        try {
            BoiInterestResponse response = restClient.get()
                    .uri("/PublicApi/GetInterest")
                    .retrieve()
                    .body(BoiInterestResponse.class);
            if (response != null && response.currentInterest() != null) {
                onRateFetched(response.currentInterest());
            }
        } catch (RestClientException e) {
            log.warn("שליפת ריבית בנק ישראל נכשלה - ממשיכים עם הערך הקודם ({}%): {}",
                    currentBaseRatePercent, e.getMessage());
            emailService.sendAlert("שליפת ריבית בנק ישראל נכשלה",
                    "ה-API של בנק ישראל (boi.org.il/PublicApi/GetInterest) לא הגיב כצפוי.\n" +
                    "האפליקציה ממשיכה לעבוד עם הערך הידוע האחרון (%.2f%%), אך כדאי לבדוק את התקלה.\n\nשגיאה: %s"
                            .formatted(currentBaseRatePercent, e.getMessage()));
        }
    }

    /** מעדכן את הריבית, ושולח התראה אם היא שונה מהערך האחרון שנשמר (בהפעלה הראשונה אין למה להשוות - רק שומרים). */
    void onRateFetched(double rate) {
        Optional<Double> previous = lastKnownRate();
        currentBaseRatePercent = rate;
        lastSuccessfulFetch = Instant.now();
        log.info("עדכון ריבית בנק ישראל הצליח: {}%", rate);

        boolean changed = previous.isPresent() && Double.compare(previous.get(), rate) != 0;
        if (changed) {
            String direction = rate > previous.get() ? "עלתה" : "ירדה";
            emailService.sendAlert("ריבית בנק ישראל %s: %.2f%% ← %.2f%%".formatted(direction, previous.get(), rate),
                    ("ריבית בנק ישראל %s מ-%.2f%% ל-%.2f%%.\n\n" +
                     "מה כבר התעדכן לבד באתר: מסלול הפריים בתמהיל המשכנתא (ריבית בנק ישראל + מרווח קבוע).\n\n" +
                     "מה כדאי לבדוק ידנית, כי זה לא מתעדכן אוטומטית:\n" +
                     "- טבלת ריבית המשכנתא הממוצעת (MortgageInterestRates) מול boi.org.il\n" +
                     "- הריבית הקבועה הלא צמודה בתמהיל (MortgageTrackMixCalculator)")
                            .formatted(direction, previous.get(), rate));
        }
        if (previous.isEmpty() || changed) {
            try {
                appState.save(new AppState(RATE_STATE_KEY, Double.toString(rate)));
            } catch (RuntimeException e) {
                log.warn("שמירת ריבית בנק ישראל במסד הנתונים נכשלה: {}", e.getMessage());
            }
        }
    }

    private Optional<Double> lastKnownRate() {
        try {
            return appState.findById(RATE_STATE_KEY).map(s -> Double.parseDouble(s.getStateValue()));
        } catch (RuntimeException e) {
            // תקלה במסד הנתונים לא אמורה להפיל את שליפת הריבית - פשוט אין ערך קודם להשוואה
            log.warn("קריאת ריבית בנק ישראל השמורה נכשלה: {}", e.getMessage());
            return Optional.empty();
        }
    }
}
