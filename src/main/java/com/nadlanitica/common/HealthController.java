package com.nadlanitica.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * בדיקת תקינות לשירות ניטור חיצוני ול-Render: 200 כשהשרת עונה וגם מסד הנתונים עונה, 503 אם המסד לא זמין.
 * בכוונה בלי Spring Actuator: זו בדיקה אחת, ו-Actuator מוסיף זיכרון לשרת של 512MB. לא חושף שום פרט פנימי.
 */
@RestController
class HealthController {

    private static final Logger log = LoggerFactory.getLogger(HealthController.class);

    private final JdbcTemplate jdbc;

    HealthController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping("/api/health")
    ResponseEntity<Map<String, String>> health() {
        try {
            jdbc.queryForObject("SELECT 1", Integer.class);
            return ResponseEntity.ok(Map.of("status", "UP"));
        } catch (RuntimeException e) {
            log.warn("בדיקת תקינות: מסד הנתונים לא עונה: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("status", "DOWN"));
        }
    }
}
