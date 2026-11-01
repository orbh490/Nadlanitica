package com.nadlanitica.leads;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.regex.Pattern;

record LeadRequest(String fullName, String phone, String email, String message) {
}

@RestController
@RequestMapping("/api/leads")
class MortgageLeadController {

    private static final int MAX_FIELD_LENGTH = 300;
    // אותם כללים כמו check.email / check.mobile ב-app.js: הבדיקה בדף היא נוחות, זו כאן היא האכיפה
    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@.]+(\\.[^\\s@.]+)*\\.[A-Za-z]{2,}$");
    private static final Pattern ISRAELI_MOBILE = Pattern.compile("^05\\d{8}$");

    private final MortgageLeadRepository repository;
    private final LeadRateLimiter rateLimiter;

    MortgageLeadController(MortgageLeadRepository repository, LeadRateLimiter rateLimiter) {
        this.repository = repository;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping
    void submit(@RequestBody LeadRequest req, HttpServletRequest request) {
        // קודם הבדיקות ורק אז המונה: מי שטעה בהקלדה לא "מבזבז" את מכסת השליחות שלו
        if (isBlank(req.fullName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "נא למלא שם");
        }
        if (tooLong(req.fullName()) || tooLong(req.phone()) || tooLong(req.email()) || tooLong(req.message())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "אחד השדות ארוך מדי");
        }
        String phone = normalizeMobile(req.phone());
        if (!ISRAELI_MOBILE.matcher(phone).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "מספר נייד לא תקין - 10 ספרות שמתחילות ב-05");
        }
        String email = req.email() == null ? "" : req.email().trim();
        if (!EMAIL.matcher(email).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "כתובת אימייל לא תקינה");
        }
        rateLimiter.checkAndRecord(request.getRemoteAddr());
        repository.save(new MortgageLead(req.fullName().trim(), phone, email, req.message()));
    }

    /** 050-123 4567, ‎+972 50 1234567 ו-972501234567 הופכים כולם ל-0501234567 - כך נשמר וכך נוח לחייג. */
    static String normalizeMobile(String phone) {
        if (phone == null) {
            return "";
        }
        String digits = phone.replaceAll("[\\s\\-().]", "");
        if (digits.startsWith("+972")) {
            digits = "0" + digits.substring(4);
        } else if (digits.startsWith("972")) {
            digits = "0" + digits.substring(3);
        }
        return digits;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static boolean tooLong(String value) {
        return value != null && value.length() > MAX_FIELD_LENGTH;
    }
}
