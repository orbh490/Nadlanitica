package com.nadlanitica.landtender;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * מכרזי קרקע לבנייה עצמית (KodYeudMichraz=1, "בנייה נמוכה/צמודת קרקע") מתוך ה-API החי של רמ"י.
 * בניגוד לנתוני "מחיר למשתכן" (תמונת מצב היסטורית קבועה), כאן נשלפים נתונים חיים בכל בקשה -
 * עם מטמון קצר (כמה דקות) רק כדי לא להעמיס על שרתי הממשלה בכל טעינת עמוד.
 */
@Service
public class LandTenderService {

    private static final Logger log = LoggerFactory.getLogger(LandTenderService.class);
    private static final int SELF_BUILD_PURPOSE_CODE = 1;
    private static final Duration LIST_CACHE_TTL = Duration.ofMinutes(10);
    private static final Duration DETAIL_CACHE_TTL = Duration.ofMinutes(5);
    private static final Duration BOOKLET_CACHE_TTL = Duration.ofHours(1);
    private static final int MAX_CACHED_BOOKLETS = 5;
    private static final Duration CLOSED_LIST_CACHE_TTL = Duration.ofHours(1);
    private static final Duration CITY_RESULTS_CACHE_TTL = Duration.ofHours(6);
    private static final int MAX_RESULTS_SAMPLE = 60;
    private static final int RESULTS_LOOKBACK_YEARS = 3;
    private static final int RESULTS_FETCH_CONCURRENCY = 5;

    private final LandTenderApiClient apiClient;
    private final SettlementNames settlementNames;
    private final TenderBookletExtractor bookletExtractor;

    private volatile List<LandTenderApiClient.RmiTenderSummary> cachedActiveTenders = List.of();
    private volatile Instant cachedAt = Instant.EPOCH;
    private volatile List<LandTenderApiClient.RmiTenderSummary> cachedClosedTenders = List.of();
    private volatile Instant cachedClosedAt = Instant.EPOCH;
    private volatile List<CityLandResult> cachedCityResults = List.of();
    private volatile Instant cachedCityResultsAt = Instant.EPOCH;
    private final Map<Long, CachedRawDetail> rawDetailCache = new ConcurrentHashMap<>();
    private final Map<Long, CachedBooklet> bookletCache = new ConcurrentHashMap<>();

    private record CachedRawDetail(LandTenderApiClient.RmiTenderDetail raw, Instant fetchedAt) {
    }

    private record CachedBooklet(byte[] pdfBytes, String text, List<String> discountSnippets, Instant fetchedAt) {
    }

    public record LandTenderSummary(long id, String tenderNumber, String city, String neighborhood, int units,
            LocalDate publishDate, LocalDate closingDate, boolean bookletPublished) {
    }

    public record PlotSummary(String plotId, String areaName, Double areaSqm, Double appraisedPrice,
            Double developmentCost, Double guaranteeDeposit) {
    }

    public record PriorityPopulationOption(int code, String label) {
    }

    public record LandTenderDetail(long id, String tenderNumber, String city, String neighborhood,
            LocalDate openingDate, LocalDate closingDate, List<PriorityPopulationOption> priorityPopulations,
            List<PlotSummary> plots, boolean bookletAvailable) {
    }

    public record BookletDiscountInfo(boolean bookletAvailable, List<String> discountSnippets) {
    }

    /** הצעה לאחוז הנחה לאוכלוסייה נבחרת, מבוססת חיפוש טקסט בחוברת - יש לאשר מול המקור, לא עובדה מחושבת. */
    public record DiscountSuggestion(Double suggestedPercent, String sourceSnippet) {
    }

    /** ממוצע תוצאות מכרזי בנייה עצמית שהוכרעו לאחרונה (זוכה+מחיר סופי בפועל), מקובץ לפי עיר. */
    public record CityLandResult(String city, int decidedPlots, double avgPricePerSqm, double avgFinalPrice) {
    }

    public LandTenderService(LandTenderApiClient apiClient, SettlementNames settlementNames,
            TenderBookletExtractor bookletExtractor) {
        this.apiClient = apiClient;
        this.settlementNames = settlementNames;
        this.bookletExtractor = bookletExtractor;
    }

    public List<LandTenderSummary> activeSelfBuildTenders() {
        List<LandTenderApiClient.RmiTenderSummary> raw = activeTendersCached();
        LocalDate today = LocalDate.now();
        return raw.stream()
                .filter(t -> t.kodYeudMichraz() != null && t.kodYeudMichraz() == SELF_BUILD_PURPOSE_CODE)
                .map(this::toSummary)
                .filter(t -> t.closingDate() == null || !t.closingDate().isBefore(today))
                .sorted(Comparator.comparing(LandTenderSummary::closingDate,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    public LandTenderDetail tenderDetail(long id) {
        LandTenderApiClient.RmiTenderDetail raw = rawDetailCached(id);

        String tenderNumber = activeTendersCached().stream()
                .filter(t -> t.michrazId() == id)
                .map(LandTenderApiClient.RmiTenderSummary::michrazName)
                .findFirst()
                .orElse(String.valueOf(id));

        List<PriorityPopulationOption> populations = raw.uchlusiyot() == null ? List.of() : raw.uchlusiyot().stream()
                .distinct()
                .flatMap(code -> PriorityPopulation.labelFor(code).stream()
                        .map(label -> new PriorityPopulationOption(code, label)))
                .toList();

        List<PlotSummary> plots = raw.tik() == null ? List.of() : raw.tik().stream()
                .map(t -> new PlotSummary(t.tikId(), blankToNull(t.mitchamName()),
                        positiveOrNull(t.shetach()),
                        positiveOrNull(t.mechirSaf() != null && t.mechirSaf() > 0 ? t.mechirSaf() : t.mechirShuma()),
                        positiveOrNull(t.hotzaotPituach()),
                        positiveOrNull(t.schumArvut())))
                .toList();

        return new LandTenderDetail(id, tenderNumber,
                settlementNames.nameFor(raw.kodYeshuv()).orElse("(עיר לא ידועה)"),
                blankToNull(raw.shchuna()), parseDate(raw.ptichaDate()), parseDate(raw.sgiraDate()), populations, plots,
                findBookletDocument(raw).isPresent());
    }

    /** מזרים את קובץ ה-PDF של חוברת המכרז המקורית, ישירות מרמ"י - לצפייה/הורדה על ידי המשתמש. */
    public byte[] bookletPdf(long id) {
        return cachedBooklet(id).pdfBytes();
    }

    /** קטעי טקסט מחוברת המכרז שמזכירים הנחה/הפחתה ליד מילת מפתח של אוכלוסיית עדיפות - ראו TenderBookletExtractor. */
    public BookletDiscountInfo bookletDiscounts(long id) {
        LandTenderApiClient.RmiTenderDetail raw = rawDetailCached(id);
        if (findBookletDocument(raw).isEmpty()) {
            return new BookletDiscountInfo(false, List.of());
        }
        return new BookletDiscountInfo(true, cachedBooklet(id).discountSnippets());
    }

    /** הצעת אחוז הנחה לאוכלוסייה ספציפית שהמשתמש בחר - לאישור מול חוברת המכרז, לא ערך סופי. */
    public DiscountSuggestion discountSuggestion(long id, int populationCode) {
        String keyword = PriorityPopulation.searchKeywordFor(populationCode).orElse(null);
        if (keyword == null) {
            return new DiscountSuggestion(null, null);
        }
        String text = cachedBooklet(id).text();
        return bookletExtractor.suggestForKeyword(text, keyword)
                .map(s -> new DiscountSuggestion(s.percent(), s.snippet()))
                .orElse(new DiscountSuggestion(null, null));
    }

    /**
     * תוצאות אמיתיות (זוכה + מחיר סופי) של מכרזי בנייה עצמית שהוכרעו ב-3 השנים האחרונות, מקובץ לפי עיר.
     * דוגם רק עד MAX_RESULTS_SAMPLE מכרזים (הכי עדכניים) כדי לא להעמיס יותר מדי קריאות על שרתי רמ"י בבת אחת.
     */
    /** מחמם את מטמון תוצאות המכרזים פעם ביום, כדי שהמשתמש הראשון שנכנס לא ישלם את המחיר (עשרות בקשות מקבילות לרמ"י). */
    @Scheduled(cron = "0 0 5 * * *")
    void warmCityLandResultsCache() {
        try {
            cityLandResults();
            log.info("חימום יומי של מטמון תוצאות מכרזי הקרקע הושלם ({} ערים)", cachedCityResults.size());
        } catch (Exception e) {
            log.warn("חימום יומי של מטמון תוצאות מכרזי הקרקע נכשל - ימשיך לפעול על המטמון הקיים: {}", e.getMessage());
        }
    }

    public synchronized List<CityLandResult> cityLandResults() {
        if (!cachedCityResults.isEmpty()
                && Duration.between(cachedCityResultsAt, Instant.now()).compareTo(CITY_RESULTS_CACHE_TTL) < 0) {
            return cachedCityResults;
        }

        LocalDate cutoff = LocalDate.now().minusYears(RESULTS_LOOKBACK_YEARS);
        List<LandTenderApiClient.RmiTenderSummary> candidates = closedTendersCached().stream()
                .filter(t -> t.kodYeudMichraz() != null && t.kodYeudMichraz() == SELF_BUILD_PURPOSE_CODE)
                .filter(t -> parseDate(t.vaadaDate()) != null && !parseDate(t.vaadaDate()).isBefore(cutoff))
                .sorted(Comparator.comparing((LandTenderApiClient.RmiTenderSummary t) -> parseDate(t.vaadaDate()))
                        .reversed())
                .limit(MAX_RESULTS_SAMPLE)
                .toList();

        ExecutorService executor = Executors.newFixedThreadPool(RESULTS_FETCH_CONCURRENCY);
        List<LandTenderApiClient.RmiTenderDetail> details;
        try {
            List<CompletableFuture<LandTenderApiClient.RmiTenderDetail>> futures = candidates.stream()
                    .map(t -> CompletableFuture.supplyAsync(() -> {
                        try {
                            return apiClient.fetchTenderDetail(t.michrazId());
                        } catch (RestClientException e) {
                            return null;
                        }
                    }, executor))
                    .toList();
            details = futures.stream().map(CompletableFuture::join).filter(Objects::nonNull).toList();
        } finally {
            executor.shutdown();
        }

        Map<String, List<Double>> finalPricesByCity = new HashMap<>();
        Map<String, List<Double>> pricePerSqmByCity = new HashMap<>();
        for (LandTenderApiClient.RmiTenderDetail d : details) {
            String city = settlementNames.nameFor(d.kodYeshuv()).orElse(null);
            if (city == null || d.tik() == null) {
                continue;
            }
            for (LandTenderApiClient.RmiTik tik : d.tik()) {
                if (tik.schumZchiya() == null || tik.schumZchiya() <= 0) {
                    continue;
                }
                finalPricesByCity.computeIfAbsent(city, k -> new ArrayList<>()).add(tik.schumZchiya());
                if (tik.shetach() != null && tik.shetach() > 0) {
                    pricePerSqmByCity.computeIfAbsent(city, k -> new ArrayList<>()).add(tik.schumZchiya() / tik.shetach());
                }
            }
        }

        List<CityLandResult> results = finalPricesByCity.entrySet().stream()
                .map(e -> {
                    String city = e.getKey();
                    double avgFinal = average(e.getValue());
                    double avgPerSqm = average(pricePerSqmByCity.getOrDefault(city, List.of()));
                    return new CityLandResult(city, e.getValue().size(), avgPerSqm, avgFinal);
                })
                .sorted(Comparator.comparingDouble(CityLandResult::avgPricePerSqm))
                .toList();

        cachedCityResults = results;
        cachedCityResultsAt = Instant.now();
        return results;
    }

    private synchronized List<LandTenderApiClient.RmiTenderSummary> closedTendersCached() {
        if (Duration.between(cachedClosedAt, Instant.now()).compareTo(CLOSED_LIST_CACHE_TTL) < 0
                && !cachedClosedTenders.isEmpty()) {
            return cachedClosedTenders;
        }
        try {
            cachedClosedTenders = apiClient.fetchClosedTenders();
            cachedClosedAt = Instant.now();
        } catch (RestClientException e) {
            if (!cachedClosedTenders.isEmpty()) {
                return cachedClosedTenders;
            }
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "לא הצלחנו לקבל את היסטוריית המכרזים מרשות מקרקעי ישראל כרגע. נסו שוב בעוד רגע.");
        }
        return cachedClosedTenders;
    }

    private static double average(List<Double> values) {
        return values.isEmpty() ? 0 : values.stream().mapToDouble(Double::doubleValue).average().orElse(0);
    }

    private CachedBooklet cachedBooklet(long id) {
        CachedBooklet cached = bookletCache.get(id);
        if (cached != null && Duration.between(cached.fetchedAt(), Instant.now()).compareTo(BOOKLET_CACHE_TTL) < 0) {
            return cached;
        }
        LandTenderApiClient.RmiTenderDetail raw = rawDetailCached(id);
        LandTenderApiClient.RmiDocument bookletDoc = findBookletDocument(raw)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "לא נמצאה חוברת מכרז למכרז זה"));
        byte[] bytes;
        try {
            bytes = apiClient.fetchDocumentContent(bookletDoc);
        } catch (RestClientException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "לא הצלחנו לקבל את חוברת המכרז מרשות מקרקעי ישראל כרגע. נסו שוב בעוד רגע.");
        }
        String text = bookletExtractor.extractText(bytes);
        List<String> snippets = bookletExtractor.findDiscountSnippets(text);
        CachedBooklet fresh = new CachedBooklet(bytes, text, snippets, Instant.now());
        // כל חוברת היא PDF של כ-9MB - בלי תקרה המטמון גדל עם כל מכרז שמישהו פתח, על שרת של 512MB
        while (bookletCache.size() >= MAX_CACHED_BOOKLETS) {
            bookletCache.entrySet().stream()
                    .min(Comparator.comparing(e -> e.getValue().fetchedAt()))
                    .ifPresent(oldest -> bookletCache.remove(oldest.getKey()));
        }
        bookletCache.put(id, fresh);
        return fresh;
    }

    private static Optional<LandTenderApiClient.RmiDocument> findBookletDocument(LandTenderApiClient.RmiTenderDetail raw) {
        if (raw.michrazDocList() == null) {
            return Optional.empty();
        }
        return raw.michrazDocList().stream()
                .filter(d -> (d.teur() != null && d.teur().contains("חוברת"))
                        || (d.docName() != null && d.docName().contains("חוברת")))
                .findFirst();
    }

    private LandTenderApiClient.RmiTenderDetail rawDetailCached(long id) {
        CachedRawDetail cached = rawDetailCache.get(id);
        if (cached != null && Duration.between(cached.fetchedAt(), Instant.now()).compareTo(DETAIL_CACHE_TTL) < 0) {
            return cached.raw();
        }
        LandTenderApiClient.RmiTenderDetail raw;
        try {
            raw = apiClient.fetchTenderDetail(id);
        } catch (RestClientException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "לא הצלחנו לקבל את פרטי המכרז מרשות מקרקעי ישראל כרגע. נסו שוב בעוד רגע.");
        }
        rawDetailCache.values().removeIf(c -> Duration.between(c.fetchedAt(), Instant.now()).compareTo(DETAIL_CACHE_TTL) >= 0);
        rawDetailCache.put(id, new CachedRawDetail(raw, Instant.now()));
        return raw;
    }

    private synchronized List<LandTenderApiClient.RmiTenderSummary> activeTendersCached() {
        if (Duration.between(cachedAt, Instant.now()).compareTo(LIST_CACHE_TTL) < 0 && !cachedActiveTenders.isEmpty()) {
            return cachedActiveTenders;
        }
        try {
            cachedActiveTenders = apiClient.fetchActiveTenders();
            cachedAt = Instant.now();
        } catch (RestClientException e) {
            if (!cachedActiveTenders.isEmpty()) {
                return cachedActiveTenders;
            }
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "לא הצלחנו לקבל את רשימת המכרזים הפעילים מרשות מקרקעי ישראל כרגע. נסו שוב בעוד רגע.");
        }
        return cachedActiveTenders;
    }

    private LandTenderSummary toSummary(LandTenderApiClient.RmiTenderSummary t) {
        return new LandTenderSummary(t.michrazId(), t.michrazName(),
                settlementNames.nameFor(t.kodYeshuv()).orElse("(עיר לא ידועה)"),
                blankToNull(t.shchuna()), t.yechidotDiur() == null ? 0 : t.yechidotDiur(),
                parseDate(t.pirsumDate()), parseDate(t.sgiraDate()),
                Boolean.TRUE.equals(t.publishedChoveret()));
    }

    private static LocalDate parseDate(String isoDateTime) {
        if (isoDateTime == null || isoDateTime.isBlank()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(isoDateTime).toLocalDate();
        } catch (Exception e) {
            return null;
        }
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private static Double positiveOrNull(Double d) {
        return d == null || d <= 0 ? null : d;
    }
}
