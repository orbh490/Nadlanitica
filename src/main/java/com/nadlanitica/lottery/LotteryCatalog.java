package com.nadlanitica.lottery;

import com.nadlanitica.lottery.LotteryLiveService.LiveLotteryProject;
import com.nadlanitica.lottery.LotteryLiveService.RegistrationStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * כל ההגרלות שאפשר לבדוק בהן סיכוי: הקובץ ההיסטורי (הגרלות שפורסמו להן תוצאות) ועוד כל הגרלה מהלוח החי של משרד
 * השיכון שעוד לא בקובץ - הרשמה פתוחה, הרשמה שעומדת להיפתח, או הגרלה טרייה שתוצאותיה עוד לא פורסמו בנתונים הרשמיים.
 * בלי זה הגרלה שנפתחת להרשמה פשוט לא מופיעה באתר, כי הקובץ ההיסטורי מתעדכן רק אחרי פרסום תוצאות.
 * התאמה לפי מספר ההגרלה: LotteryNumber בלוח החי הוא בדיוק LotteryId בקובץ, ונבדק שגם הספירות זהות בשניהם.
 */
@Service
class LotteryCatalog {

    /**
     * הגרלה אחת ברשימה. registration = מצב ההרשמה מהלוח החי (null אם אין לה התאמה בלוח או שהוא לא נטען),
     * fromLiveBoard = הנתונים עצמם מהלוח החי ולא מהקובץ (ולכן מספר הנרשמים יכול עוד להשתנות, ואין זוכים בפועל).
     */
    record Entry(LotteryProject project, RegistrationStatus registration, boolean fromLiveBoard) {
    }

    private final LotteryOddsEngine engine;
    private final LotteryLiveService liveService;
    private final Set<String> historicalIds;

    LotteryCatalog(LotteryOddsEngine engine, LotteryLiveService liveService) {
        this.engine = engine;
        this.liveService = liveService;
        this.historicalIds = engine.allProjects().stream().map(LotteryProject::getLotteryId).collect(Collectors.toSet());
    }

    List<Entry> all() {
        return Stream.concat(engine.allProjects().stream().map(p -> new Entry(p, null, false)), liveOnly()).toList();
    }

    Optional<Entry> find(String lotteryId) {
        return engine.findByLotteryId(lotteryId).map(p -> new Entry(p, null, false))
                .or(() -> liveOnly().filter(e -> e.project().getLotteryId().equals(lotteryId)).findFirst());
    }

    /** הגרלות מהלוח החי שאין בקובץ ההיסטורי - בלי פנייה לרשת, רק מה שהרענון ברקע כבר הביא. */
    private Stream<Entry> liveOnly() {
        return liveService.loadedProjects().stream()
                .filter(live -> live.lotteryNumber() != null && !historicalIds.contains(live.lotteryNumber().trim()))
                .map(LotteryCatalog::toEntry)
                .flatMap(Optional::stream);
    }

    private static Optional<Entry> toEntry(LiveLotteryProject live) {
        int units = orZero(live.targetHousingUnits()) > 0 ? live.targetHousingUnits() : orZero(live.housingUnits());
        if (units <= 0 || live.city() == null || live.city().isBlank()) {
            return Optional.empty(); // כמו בקובץ ההיסטורי: בלי יחידות אין מה לחשב
        }
        String name = live.projectName() == null || live.projectName().isBlank() ? live.neighborhood() : live.projectName();
        LotteryProject project = new LotteryProject(live.lotteryNumber().trim(), live.city().trim(),
                name == null ? "" : name.trim(), live.projectId(), live.lotteryDate(), units,
                orZero(live.localHousingUnits()), orZero(live.totalSubscribers()), orZero(live.totalLocalSubscribers()),
                -1, -1, live.pricePerSqm() == null ? 0 : live.pricePerSqm());
        return Optional.of(new Entry(project, live.status(), true));
    }

    private static int orZero(Integer n) {
        return n == null ? 0 : n;
    }
}
