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
 * יכול לקרוא אותו. ה-CSP מתיר להריץ סקריפטים רק מהאתר עצמו ולשלוח בקשות רק אליו - כך שגם אם קוד כזה רץ,
 * אין לו לאן להעביר את הטוקן ואין ממנו לטעון סקריפטים נוספים.
 * 'unsafe-inline' ב-script-src נשאר בינתיים: יש בעמודים onclick="..." וסקריפט מצב-כהה קטן ב-head.
 * כשהם יעברו ל-addEventListener בקובצי ה-JS, אפשר להוריד אותו ולקבל הגנה מלאה.
 */
@Component
class SecurityHeadersFilter extends OncePerRequestFilter {

    static final String CSP = String.join("; ",
            "default-src 'self'",
            "script-src 'self' 'unsafe-inline'",
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
