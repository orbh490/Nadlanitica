package com.nadlanitica.common;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.CssLinkResourceTransformer;
import org.springframework.web.servlet.resource.VersionResourceResolver;

import java.time.Duration;
import java.util.List;

/**
 * מפה כתובות .html קבועות לתבניות Thymeleaf תחת templates/pages/ - כל עמוד נשאר באותו URL בדיוק כמו
 * כשהיה קובץ סטטי, כדי לא לשבור קישורים קיימים (כולל קישורים שכבר נשלחו במיילי אימות ואיפוס סיסמה).
 */
@Configuration
class WebViewConfig implements WebMvcConfigurer {

    private static final List<String> PAGES = List.of(
            "index", "home", "dashboard", "lottery", "land-tenders", "land-tender-discounts", "results-board",
            "account", "advisor", "settings", "privacy", "disclaimer", "accessibility", "email-verified", "reset-password",
            "leads");

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // הכתובת הראשית מציגה את דף הבית (מה שמנועי חיפוש ומשתמשים חדשים רואים קודם); מסך ההתחברות נשאר ב-/index.html
        registry.addViewController("/").setViewName("pages/home");
        for (String page : PAGES) {
            registry.addViewController("/" + page + ".html").setViewName("pages/" + page);
        }
    }

    /**
     * CSS, JS ותמונות נשמרים בדפדפן לשנה. זה בטוח כי הכתובת כוללת טביעת אצבע של התוכן (למשל app-3f9a1c.css):
     * כשקובץ משתנה, הכתובת שלו משתנה, והמשתמשים מקבלים את הגרסה החדשה מיד. בתבניות חייבים להפנות לקבצים
     * עם th:href="@{...}" / th:src="@{...}" כדי שהכתובת תקבל את טביעת האצבע; ב-CSS הקישורים מתעדכנים אוטומטית.
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        for (String dir : List.of("css", "js", "img")) {
            registry.addResourceHandler("/" + dir + "/**")
                    .addResourceLocations("classpath:/static/" + dir + "/")
                    .setCacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic())
                    .resourceChain(true)
                    .addResolver(new VersionResourceResolver().addContentVersionStrategy("/**"))
                    .addTransformer(new CssLinkResourceTransformer());
        }
    }
}
