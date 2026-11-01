package com.nadlanitica.common;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * כותרות אבטחה לכל תשובה. החשובה שבהן היא CSP: טוקן ההתחברות נשמר ב-localStorage, אז קוד זר שיצליח לרוץ בדף
 * יכול לקרוא אותו. ה-CSP מתיר להריץ סקריפטים רק מקבצים של האתר עצמו - לא קוד שכתוב בתוך הדף (onclick="...",
 * <script> בלי src) - ולשלוח בקשות רק אליו. כך גם טקסט זדוני שיגיע לדף לא ירוץ, ואין לו לאן להעביר את הטוקן.
 * לכן בעמודים אין קוד בתוך ה-HTML: כפתורים מחוברים דרך data-click / data-change (ראו static/js/actions.js).
 * style-src עדיין מתיר inline (style="..." בעמודים) - סגנונות לא מריצים קוד, אז הסיכון שם נמוך.
 */
@Component
class SecurityHeadersFilter extends OncePerRequestFilter {

    static final String CSP = String.join("; ",
            "default-src 'self'",
            // inline-speculation-rules: רק הוראות JSON לדפדפן לטעון מראש את הדפים השכנים בנייד (mobile.js) - לא קוד
            "script-src 'self' 'inline-speculation-rules'",
            "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com",
            "font-src 'self' https://fonts.gstatic.com",
            "img-src 'self' data:",
            "connect-src 'self'",
            "object-src 'none'",
            "base-uri 'self'",
            "form-action 'self'",
            "frame-ancestors 'none'");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        // CSP רק לעמודים: על תשובות ה-API (JSON, וחוברות מכרז כ-PDF שנפתחות בצופה של הדפדפן) היא לא מגינה על כלום
        if (!request.getRequestURI().startsWith("/api/")) {
            response.setHeader("Content-Security-Policy", CSP);
        }
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "DENY");
        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        response.setHeader("Permissions-Policy", "camera=(), microphone=(), geolocation=(), payment=()");
        // רק כשהבקשה הגיעה ב-https (מאחורי Render זה מזוהה לפי X-Forwarded-Proto): מכאן והלאה הדפדפן לא ינסה http
        if (request.isSecure()) {
            response.setHeader("Strict-Transport-Security", "max-age=31536000");
        }
        chain.doFilter(request, response);
    }
}
