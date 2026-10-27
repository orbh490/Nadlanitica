package com.nadlanitica.common;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

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
        registry.addViewController("/").setViewName("pages/index");
        for (String page : PAGES) {
            registry.addViewController("/" + page + ".html").setViewName("pages/" + page);
        }
    }
}
