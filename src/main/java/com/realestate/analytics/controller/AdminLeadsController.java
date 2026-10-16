package com.realestate.analytics.controller;

import com.realestate.analytics.model.MortgageLead;
import com.realestate.analytics.repository.MortgageLeadRepository;
import com.realestate.analytics.service.AdminGuard;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/admin/leads")
class AdminLeadsController {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.of("Asia/Jerusalem"));

    private final MortgageLeadRepository repository;
    private final AdminGuard adminGuard;

    AdminLeadsController(MortgageLeadRepository repository, AdminGuard adminGuard) {
        this.repository = repository;
        this.adminGuard = adminGuard;
    }

    @GetMapping
    List<MortgageLead> list(@RequestHeader("X-Admin-Key") String key) {
        adminGuard.check(key);
        return repository.findAllByOrderByCreatedAtDesc();
    }

    @GetMapping("/export.csv")
    ResponseEntity<byte[]> exportCsv(@RequestHeader("X-Admin-Key") String key) {
        adminGuard.check(key);
        StringBuilder csv = new StringBuilder();
        csv.append((char) 0xFEFF);
        csv.append("תאריך,שם מלא,טלפון,אימייל,הודעה\n");
        for (MortgageLead lead : repository.findAllByOrderByCreatedAtDesc()) {
            csv.append(DATE_FORMAT.format(lead.getCreatedAt())).append(',')
                    .append(csvField(lead.getFullName())).append(',')
                    .append(csvField(lead.getPhone())).append(',')
                    .append(csvField(lead.getEmail())).append(',')
                    .append(csvField(lead.getMessage())).append('\n');
        }
        byte[] bytes = csv.toString().getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=leads.csv")
                .body(bytes);
    }

    private static String csvField(String value) {
        if (value == null) return "";
        // מניעת CSV/Formula Injection: שדה שמתחיל ב-=+-@ מתפרש כנוסחה ע"י Excel/Sheets כשפותחים את
        // הקובץ - והשדות האלה מגיעים ישירות ממשתמש לא-מאומת (טופס הלידים הציבורי). תו ' מוביל מנטרל זאת.
        String safe = value.matches("^[=+\\-@].*") ? "'" + value : value;
        return "\"" + safe.replace("\"", "\"\"") + "\"";
    }
}
