package com.nadlanitica.landtender;

import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * מיפוי "סמל יישוב" (הקוד הרשמי של הלמ"ס, המשמש גם את רשות מקרקעי ישראל) לשם העיר/יישוב בעברית.
 * מבוסס בעיקרו על הקודים שכבר אומתו מול נתוני משרד השיכון (lottery_results.csv), עם השלמה
 * ליישובים שלא הופיעו שם. זהו מידע תיאורי (שם בלבד) ולא נתון רגולטורי - דיוק מוחלט אינו קריטי.
 */
@Service
public class SettlementNames {

    private final Map<Integer, String> names = load();

    public Optional<String> nameFor(Integer kodYeshuv) {
        return kodYeshuv == null ? Optional.empty() : Optional.ofNullable(names.get(kodYeshuv));
    }

    private static Map<Integer, String> load() {
        try (InputStream in = SettlementNames.class.getResourceAsStream("/data/settlement_codes.csv")) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            return reader.lines()
                    .skip(1)
                    .map(line -> line.split(";", 2))
                    .filter(f -> f.length == 2)
                    .collect(Collectors.toMap(f -> Integer.parseInt(f[0]), f -> f[1], (a, b) -> a));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
