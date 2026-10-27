package com.nadlanitica.landtender;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * מחפש בטקסט חוברת המכרז (PDF) קטעים שכנראה מתארים הנחה לאוכלוסיות עדיפות (מילואים/חסרי דיור/נכות/בני מקום).
 * זו חיפוש-והצגה של הטקסט המקורי, לא חישוב - שיעור ההנחה המדויק לכל אוכלוסייה משתנה בין מכרזים,
 * ולפעמים כמה שיעורים חלים יחד (ולפעמים דווקא לא - "הנחה אחת בלבד" מופיע בחלק מהחוברות!) - לכן כל
 * מספר שמוצא כאן הוא הצעה לאישור מול המסמך המלא, לא עובדה מחושבת.
 */
@Component
class TenderBookletExtractor {

    private static final String[] GENERIC_KEYWORDS = {
            "מילואים", "חסר דיור", "חסרי דיור", "נכה", "נכות", "בן מקום", "מיעוטים", "הנחה", "הפחתה"
    };
    private static final int LINE_WINDOW = 2;
    private static final int MAX_SNIPPETS = 25;
    private static final int MAX_SNIPPET_CHARS = 400;
    private static final Pattern PERCENT_PATTERN = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*%");

    record DiscountSuggestion(Double percent, String snippet) {
    }

    String extractText(byte[] pdfBytes) {
        try (PDDocument doc = Loader.loadPDF(pdfBytes)) {
            return new PDFTextStripper().getText(doc);
        } catch (IOException e) {
            return "";
        }
    }

    List<String> findDiscountSnippets(String text) {
        String[] lines = text.split("\\r?\\n");
        Set<String> snippets = new LinkedHashSet<>();
        for (int i = 0; i < lines.length && snippets.size() < MAX_SNIPPETS; i++) {
            if (!containsAny(lines[i], GENERIC_KEYWORDS)) {
                continue;
            }
            int from = Math.max(0, i - LINE_WINDOW);
            int to = Math.min(lines.length - 1, i + LINE_WINDOW);
            if (!windowHasPercent(lines, from, to)) {
                continue;
            }
            snippets.add(buildSnippet(lines, from, to));
            i = to; // מדלג על שאר החלון שכבר נלכד, כדי לא לצבור עשרות גרסאות חופפות של אותו קטע
        }
        return new ArrayList<>(snippets);
    }

    /** הצעה לאחוז הנחה לאוכלוסייה ספציפית: המופע הראשון של מילת המפתח עם אחוז בקרבתו. לא עובדה - הצעה לאישור. */
    Optional<DiscountSuggestion> suggestForKeyword(String text, String keyword) {
        String[] lines = text.split("\\r?\\n");
        for (int i = 0; i < lines.length; i++) {
            if (!lines[i].contains(keyword)) {
                continue;
            }
            int from = Math.max(0, i - LINE_WINDOW);
            int to = Math.min(lines.length - 1, i + LINE_WINDOW);
            String snippet = buildSnippet(lines, from, to);
            Matcher m = PERCENT_PATTERN.matcher(snippet);
            if (m.find()) {
                return Optional.of(new DiscountSuggestion(Double.parseDouble(m.group(1)), snippet));
            }
        }
        return Optional.empty();
    }

    private static boolean containsAny(String line, String[] keywords) {
        for (String keyword : keywords) {
            if (line.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private static boolean windowHasPercent(String[] lines, int from, int to) {
        for (int j = from; j <= to; j++) {
            if (lines[j].contains("%")) {
                return true;
            }
        }
        return false;
    }

    private static String buildSnippet(String[] lines, int from, int to) {
        StringBuilder snippet = new StringBuilder();
        for (int j = from; j <= to; j++) {
            String trimmed = lines[j].trim();
            if (!trimmed.isEmpty()) {
                if (!snippet.isEmpty()) {
                    snippet.append(' ');
                }
                snippet.append(trimmed);
            }
        }
        String result = snippet.toString();
        return result.length() > MAX_SNIPPET_CHARS ? result.substring(0, MAX_SNIPPET_CHARS) + "…" : result;
    }
}
