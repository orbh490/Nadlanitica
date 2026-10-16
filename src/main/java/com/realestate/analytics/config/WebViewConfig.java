package com.realestate.analytics.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * מפה כתובות .html קבועות (נשארות זהות לאלה שהיו קבצים סטטיים) לתבניות Thymeleaf תחת templates/ -
 * כדי שהדפים שעברו ל-fragments משותפים (head/corner-actions/floating-nav/policy-links) ימשיכו
 * להיות נגישים באותו URL בדיוק כמו קודם, בלי לשבור אף קישור קיים באפליקציה.
 */
@Configuration
class WebViewConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/lottery.html").setViewName("lottery");
        registry.addViewController("/land-tenders.html").setViewName("land-tenders");
        registry.addViewController("/dashboard.html").setViewName("dashboard");
        registry.addViewController("/results-board.html").setViewName("results-board");
        registry.addViewController("/account.html").setViewName("account");
        registry.addViewController("/advisor.html").setViewName("advisor");
        registry.addViewController("/settings.html").setViewName("settings");
        registry.addViewController("/home.html").setViewName("home");
    }
}
