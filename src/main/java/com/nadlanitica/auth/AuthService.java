package com.nadlanitica.auth;

import com.nadlanitica.notifications.NotificationEmailService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

/**
 * הרשמה/התחברות: אימייל+סיסמה, סיסמה מגובבת (BCrypt) - לעולם לא נשמרת כטקסט גלוי.
 * טוקן ההתחברות הוא ערך אקראי אטום (לא JWT) שנשמר בטבלת UserSession - כך שאפשר גם לבטל אותו בהתנתקות.
 * הרשמה דורשת אימות אימייל (קישור חד-פעמי שנשלח בדואר) לפני שההתחברות הראשונה מתאפשרת - כדי שאי אפשר
 * יהיה להירשם עם כתובת אימייל של מישהו אחר בלי שהוא ידע. אותו מנגנון טוקן חד-פעמי משמש גם לאיפוס סיסמה.
 */
@Service
public class AuthService {

    private static final Duration SESSION_TTL = Duration.ofDays(30);
    private static final Duration VERIFY_TOKEN_TTL = Duration.ofDays(2);
    private static final Duration RESET_TOKEN_TTL = Duration.ofHours(1);
    private static final String PURPOSE_VERIFY = "VERIFY_EMAIL";
    private static final String PURPOSE_RESET = "RESET_PASSWORD";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final AppUserRepository userRepository;
    private final UserSessionRepository sessionRepository;
    private final AccountTokenRepository tokenRepository;
    private final LoginRateLimiter rateLimiter;
    private final NotificationEmailService emailService;
    private final String baseUrl;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public record AuthResult(String token, String email) {
    }

    public AuthService(AppUserRepository userRepository, UserSessionRepository sessionRepository,
                        AccountTokenRepository tokenRepository, LoginRateLimiter rateLimiter,
                        NotificationEmailService emailService, @Value("${app.base-url}") String baseUrl) {
        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
        this.tokenRepository = tokenRepository;
        this.rateLimiter = rateLimiter;
        this.emailService = emailService;
        this.baseUrl = baseUrl;
    }

    public void register(String email, String password, boolean consent) {
        String normalizedEmail = normalize(email);
        if (normalizedEmail.isBlank() || !normalizedEmail.contains("@")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "כתובת אימייל לא תקינה");
        }
        if (password == null || password.length() < 6) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "הסיסמה חייבת להכיל לפחות 6 תווים");
        }
        if (!consent) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "יש לאשר את מדיניות הפרטיות כדי להירשם");
        }
        if (userRepository.findByEmailIgnoreCase(normalizedEmail).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "כבר קיים משתמש עם האימייל הזה");
        }
        AppUser user = new AppUser(UUID.randomUUID().toString(), normalizedEmail, passwordEncoder.encode(password));
        userRepository.save(user);
        sendVerificationEmail(user);
    }

    public AuthResult login(String email, String password) {
        String normalizedEmail = normalize(email);
        rateLimiter.checkNotBlocked(normalizedEmail);
        Optional<AppUser> user = userRepository.findByEmailIgnoreCase(normalizedEmail);
        if (user.isEmpty() || password == null || !passwordEncoder.matches(password, user.get().getPasswordHash())) {
            rateLimiter.recordFailure(normalizedEmail);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "אימייל או סיסמה שגויים");
        }
        if (!user.get().isEmailVerified()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "יש לאמת את כתובת האימייל לפני ההתחברות - בדקו את תיבת הדואר שלכם (כולל תיקיית ספאם)");
        }
        rateLimiter.recordSuccess(normalizedEmail);
        return new AuthResult(createSession(user.get().getId()), user.get().getEmail());
    }

    public void logout(String token) {
        if (token != null) {
            sessionRepository.deleteById(token);
        }
    }

    /** מחזיר את המשתמש המחובר לפי טוקן, אם קיים וטרם פג - אחרת ריק (לא זורק שגיאה, זה מצב "אנונימי" לגיטימי). */
    public Optional<AppUser> currentUser(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        return sessionRepository.findById(token)
                .filter(s -> s.getExpiresAt().isAfter(Instant.now()))
                .flatMap(s -> userRepository.findById(s.getUserId()));
    }

    /** כמו currentUser, אבל דורש התחברות - לשימוש בנקודות קצה שחייבות משתמש רשום. */
    public AppUser requireUser(String token) {
        return currentUser(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "יש להתחבר כדי להשתמש בפיצ'ר הזה"));
    }

    /**
     * מחיקת חשבון לפי בקשת המשתמש (זכות המחיקה בחוק הגנת הפרטיות, ומובטחת במדיניות הפרטיות).
     * דורש את הסיסמה שוב, כדי שמי שמצא מכשיר פתוח לא יוכל למחוק חשבון של מישהו אחר; ניסיונות שגויים
     * נספרים יחד עם ניסיונות התחברות כושלים. כל המידע של המשתמש (sessions, טוקנים, חישובים שמורים, אזורי עניין)
     * נמחק יחד איתו ע"י ON DELETE CASCADE במסד הנתונים - ראו V1__init.sql.
     */
    public void deleteAccount(String token, String password) {
        AppUser user = requireUser(token);
        rateLimiter.checkNotBlocked(user.getEmail());
        if (password == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
            rateLimiter.recordFailure(user.getEmail());
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "הסיסמה שגויה - החשבון לא נמחק");
        }
        userRepository.delete(user);
    }

    /** לא חושף אם הכתובת קיימת/מאומתת כבר - תמיד "מצליח" מנקודת מבטו של הקורא. */
    public void resendVerification(String email) {
        userRepository.findByEmailIgnoreCase(normalize(email))
                .filter(u -> !u.isEmailVerified())
                .ifPresent(this::sendVerificationEmail);
    }

    /**
     * בדיקה בלבד, בלי לצרוך את הטוקן - כי הקישור מהמייל נפתח לראשונה בבקשת GET, ותוכנות סריקת דואר
     * (Gmail, שערי אבטחה ארגוניים וכו') "מבקרות" אוטומטית בכל קישור שמגיע במייל כדי לבדוק פישינג,
     * עוד לפני שהאדם עצמו פותח את המייל. אם ה-GET היה צורך את הטוקן החד-פעמי, הסריקה האוטומטית הזו
     * הייתה מבטלת אותו לפני שהמשתמש האמיתי מספיק ללחוץ. לכן GET רק בודק, ורק לחיצת אישור מפורשת בעמוד
     * (confirmEmailVerification, שמופעלת ב-POST מה-JS) באמת צורכת את הטוקן.
     */
    public boolean isValidVerificationToken(String token) {
        return tokenRepository.findById(token)
                .filter(t -> PURPOSE_VERIFY.equals(t.getPurpose()))
                .filter(t -> t.getExpiresAt().isAfter(Instant.now()))
                .isPresent();
    }

    public boolean confirmEmailVerification(String token) {
        Optional<AccountToken> accountToken = tokenRepository.findById(token)
                .filter(t -> PURPOSE_VERIFY.equals(t.getPurpose()))
                .filter(t -> t.getExpiresAt().isAfter(Instant.now()));
        if (accountToken.isEmpty()) {
            return false;
        }
        userRepository.findById(accountToken.get().getUserId()).ifPresent(user -> {
            user.setEmailVerified(true);
            userRepository.save(user);
        });
        tokenRepository.deleteById(token);
        return true;
    }

    /** לא חושף אם הכתובת קיימת במערכת - תמיד "מצליח" מנקודת מבטו של הקורא. */
    public void requestPasswordReset(String email) {
        userRepository.findByEmailIgnoreCase(normalize(email)).ifPresent(user -> {
            String token = randomToken();
            tokenRepository.save(new AccountToken(token, user.getId(), PURPOSE_RESET, Instant.now().plus(RESET_TOKEN_TTL)));
            String link = baseUrl + "/reset-password.html?token=" + token;
            emailService.send(user.getEmail(), "איפוס סיסמה",
                    "קיבלנו בקשה לאיפוס הסיסמה שלכם.\n\nלאיפוס, לחצו על הקישור הבא (בתוקף לשעה אחת):\n" + link +
                            "\n\nאם לא ביקשתם זאת, אפשר להתעלם מהמייל הזה - הסיסמה שלכם לא תשתנה.");
        });
    }

    // derived deleteAllByUserId needs a transaction, otherwise the reset fails after saving the new password
    @Transactional
    public AuthResult resetPassword(String token, String newPassword) {
        if (newPassword == null || newPassword.length() < 6) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "הסיסמה חייבת להכיל לפחות 6 תווים");
        }
        AccountToken accountToken = tokenRepository.findById(token)
                .filter(t -> PURPOSE_RESET.equals(t.getPurpose()))
                .filter(t -> t.getExpiresAt().isAfter(Instant.now()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "קישור האיפוס אינו תקין או שפג תוקפו - יש לבקש קישור חדש"));
        AppUser user = userRepository.findById(accountToken.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "המשתמש לא נמצא"));
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        tokenRepository.deleteById(token);
        sessionRepository.deleteAllByUserId(user.getId());
        return new AuthResult(createSession(user.getId()), user.getEmail());
    }

    private void sendVerificationEmail(AppUser user) {
        String token = randomToken();
        tokenRepository.save(new AccountToken(token, user.getId(), PURPOSE_VERIFY, Instant.now().plus(VERIFY_TOKEN_TTL)));
        String link = baseUrl + "/api/auth/verify?token=" + token;
        emailService.send(user.getEmail(), "אימות כתובת אימייל",
                "תודה שנרשמתם לנדלניטיקה!\n\nלאימות כתובת האימייל, לחצו על הקישור הבא:\n" + link +
                        "\n\nהקישור בתוקף ל-48 שעות.");
    }

    private String randomToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String createSession(String userId) {
        String token = randomToken();
        sessionRepository.save(new UserSession(token, userId, Instant.now().plus(SESSION_TTL)));
        return token;
    }

    private static String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }
}
