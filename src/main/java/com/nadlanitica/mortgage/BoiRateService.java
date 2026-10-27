package com.nadlanitica.mortgage;

import com.nadlanitica.notifications.NotificationEmailService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Instant;

/**
 * ריבית בנק ישראל (הריבית הבסיסית שממנה נגזר מסלול הפריים) - נשלפת חיה מה-API הציבורי הרשמי של בנק ישראל
 * (https://boi.org.il/PublicApi/GetInterest, נבדק ואומת ידנית - endpoint לא מתועד באתר עצמו אך פתוח וללא אימות).
 * מתעדכנת פעם ביום: בנק ישראל משנה את הריבית הזו רק בהחלטות ריבית מתוזמנות (כ-8 בשנה), אז תדירות גבוהה יותר מיותרת.
 * אם השליפה נכשלת (השרת למטה, שינוי ב-API וכו') - נשאר הערך התקף האחרון שהצלחנו לשלוף, ואם עוד לא הצלחנו
 * אף פעם - נופלים לערך ברירת מחדל שאומת ידנית, כדי שהאפליקציה לעולם לא תישבר בגלל תלות חיצונית זו.
 */
@Service
public class BoiRateService {

    private static final Logger log = LoggerFactory.getLogger(BoiRateService.class);
    private static final double FALLBACK_BASE_RATE_PERCENT = 3.5; // אומת ידנית לאחרונה - ראו זיכרון הפרויקט

    private final RestClient restClient = RestClient.create("https://boi.org.il");
    private final NotificationEmailService emailService;

    private volatile double currentBaseRatePercent = FALLBACK_BASE_RATE_PERCENT;
    private volatile Instant lastSuccessfulFetch;

    public BoiRateService(NotificationEmailService emailService) {
        this.emailService = emailService;
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
                currentBaseRatePercent = response.currentInterest();
                lastSuccessfulFetch = Instant.now();
                log.info("עדכון ריבית בנק ישראל הצליח: {}%", currentBaseRatePercent);
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
}
