package com.nadlanitica.common;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

/**
 * server.error.include-message=always לא באמת גרם להודעות שגיאה (ה"reason" של ResponseStatusException) להופיע
 * בתשובת ה-JSON בגרסת Spring Boot הזו - נבדק ישירות מול השרת הרץ ואומת שהשדה message פשוט לא הגיע ללקוח.
 * הפתרון הישיר והוודאי: תופסים כל ResponseStatusException במקום מרכזי אחד ומחזירים JSON עם message במפורש,
 * בלי תלות בהתנהגות ברירת המחדל הפנימית של Spring.
 */
@RestControllerAdvice
class ApiExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<Map<String, String>> handle(ResponseStatusException ex) {
        String message = ex.getReason() != null ? ex.getReason() : "שגיאה לא צפויה";
        return ResponseEntity.status(ex.getStatusCode()).body(Map.of("message", message));
    }
}
