package com.nadlanitica.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

/**
 * כל שגיאה מה-API חוזרת כ-JSON אחיד {"message": "..."} בעברית, שה-UI יכול להציג ישירות למשתמש.
 * בלי זה Spring מחזיר את גוף השגיאה הגנרי שלו (באנגלית, בלי message) - וחריגה לא צפויה הופכת ל-500 בלי הסבר.
 * מוגבל ל-@RestController בלבד, כדי שעמודי HTML (למשל 404 על עמוד שלא קיים) לא יקבלו פתאום JSON.
 */
@RestControllerAdvice(annotations = RestController.class)
class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);
    private static final String INVALID_REQUEST = "הבקשה אינה תקינה - בדקו את הערכים שהוזנו ונסו שוב";

    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<Map<String, String>> handle(ResponseStatusException ex) {
        return error(ex.getStatusCode(), ex.getReason() != null ? ex.getReason() : messageFor(ex.getStatusCode()));
    }

    /** ולידציה על גוף בקשה (@Valid) - מחזיר את ההודעה של השדה הראשון שנכשל, כפי שהוגדרה ב-record. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Map<String, String>> invalidBody(MethodArgumentNotValidException ex) {
        FieldError field = ex.getBindingResult().getFieldError();
        return error(HttpStatus.BAD_REQUEST, field != null && field.getDefaultMessage() != null
                ? field.getDefaultMessage() : INVALID_REQUEST);
    }

    /** ולידציה על פרמטרים של URL (@RequestParam עם @Min/@Max וכו'). */
    @ExceptionHandler(HandlerMethodValidationException.class)
    ResponseEntity<Map<String, String>> invalidParams(HandlerMethodValidationException ex) {
        String message = ex.getAllErrors().stream()
                .map(e -> e.getDefaultMessage())
                .filter(m -> m != null && !m.isBlank())
                .findFirst()
                .orElse(INVALID_REQUEST);
        return error(HttpStatus.BAD_REQUEST, message);
    }

    /** JSON שבור, או ערך שלא מתאים לטיפוס (למשל קטגוריה שלא קיימת או טקסט בשדה מספרי). */
    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    ResponseEntity<Map<String, String>> unreadable(Exception ex) {
        return error(HttpStatus.BAD_REQUEST, INVALID_REQUEST);
    }

    @ExceptionHandler({MissingServletRequestParameterException.class, MissingRequestHeaderException.class})
    ResponseEntity<Map<String, String>> missing(Exception ex) {
        return error(HttpStatus.BAD_REQUEST, "חסר פרט חובה בבקשה");
    }

    /** כל השאר: שגיאות Spring עם סטטוס משלהן (405, 415...) נשארות עם הסטטוס, וכל חריגה אחרת = 500 עם לוג מלא. */
    @ExceptionHandler(Exception.class)
    ResponseEntity<Map<String, String>> unexpected(Exception ex) {
        if (ex instanceof ErrorResponse errorResponse) {
            return error(errorResponse.getStatusCode(), messageFor(errorResponse.getStatusCode()));
        }
        log.error("שגיאה לא צפויה ב-API", ex);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, messageFor(HttpStatus.INTERNAL_SERVER_ERROR));
    }

    private static String messageFor(HttpStatusCode status) {
        return switch (status.value()) {
            case 400 -> INVALID_REQUEST;
            case 401 -> "יש להתחבר כדי להשתמש בפיצ'ר הזה";
            case 403 -> "גישה נדחתה";
            case 404 -> "לא נמצא";
            case 405, 415 -> "סוג הבקשה אינו נתמך";
            case 429 -> "יותר מדי בקשות - נסו שוב בעוד כמה דקות";
            case 502, 503, 504 -> "שירות חיצוני אינו זמין כרגע - נסו שוב בעוד רגע";
            default -> "אירעה שגיאה בלתי צפויה - נסו שוב מאוחר יותר";
        };
    }

    private static ResponseEntity<Map<String, String>> error(HttpStatusCode status, String message) {
        return ResponseEntity.status(status).body(Map.of("message", message));
    }
}
