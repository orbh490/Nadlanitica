package com.nadlanitica.common;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

/**
 * robots.txt ו-sitemap.xml למנועי חיפוש. נוצרים מ-app.base-url (ולא קבצים סטטיים) כי הם חייבים כתובות מלאות,
 * והדומיין משתנה בין פיתוח לפרודקשן.
 */
@RestController
class SeoController {

    /** עמודי תוכן ציבוריים בלבד - בלי עמודי חשבון, איפוס סיסמה, אימות מייל או ניהול. */
    private static final List<String> PUBLIC_PAGES = List.of(
            "/", "/dashboard.html", "/lottery.html", "/land-tenders.html", "/results-board.html",
            "/advisor.html", "/privacy.html", "/disclaimer.html", "/accessibility.html");

    private final String baseUrl;

    SeoController(@Value("${app.base-url}") String baseUrl) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    @GetMapping(value = "/robots.txt", produces = MediaType.TEXT_PLAIN_VALUE)
    String robots() {
        return """
                User-agent: *
                Disallow: /api/
                Disallow: /account.html
                Disallow: /settings.html
                Disallow: /leads.html
                Disallow: /reset-password.html
                Disallow: /email-verified.html
                Disallow: /land-tender-discounts.html

                Sitemap: %s/sitemap.xml
                """.formatted(baseUrl);
    }

    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    String sitemap() {
        String urls = PUBLIC_PAGES.stream()
                .map(page -> "  <url><loc>" + baseUrl + page + "</loc></url>")
                .collect(Collectors.joining("\n"));
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
                %s
                </urlset>
                """.formatted(urls);
    }
}
