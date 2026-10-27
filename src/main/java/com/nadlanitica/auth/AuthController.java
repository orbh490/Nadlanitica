package com.nadlanitica.auth;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.Map;
import java.util.Optional;

// BCrypt מתעלם מכל מה שאחרי 72 בייטים (ובגרסאות החדשות זורק חריגה) - לכן התקרה על אורך הסיסמה.
record AuthRequest(
        @Size(max = 254, message = "כתובת האימייל ארוכה מדי") String email,
        @Size(max = 72, message = "הסיסמה ארוכה מדי (עד 72 תווים)") String password,
        Boolean consent) {
}

record MeResponse(boolean loggedIn, String email) {
}

record EmailRequest(
        @NotBlank(message = "יש להזין כתובת אימייל")
        @Size(max = 254, message = "כתובת האימייל ארוכה מדי") String email) {
}

record ResetPasswordRequest(
        @NotBlank(message = "הקישור אינו תקין - יש לבקש קישור חדש") String token,
        @Size(max = 72, message = "הסיסמה ארוכה מדי (עד 72 תווים)") String newPassword) {
}

record DeleteAccountRequest(
        @NotBlank(message = "יש להזין את הסיסמה כדי למחוק את החשבון")
        @Size(max = 72, message = "הסיסמה ארוכה מדי (עד 72 תווים)") String password) {
}

record TokenRequest(@NotBlank(message = "הקישור אינו תקין - יש לבקש קישור אימות חדש") String token) {
}

@RestController
@RequestMapping("/api/auth")
class AuthController {

    private final AuthService authService;

    AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    Map<String, String> register(@Valid @RequestBody AuthRequest req) {
        authService.register(req.email(), req.password(), Boolean.TRUE.equals(req.consent()));
        return Map.of("message", "נשלח מייל אימות ל-" + req.email() + " - יש לאמת את הכתובת לפני ההתחברות");
    }

    @PostMapping("/login")
    AuthService.AuthResult login(@Valid @RequestBody AuthRequest req) {
        return authService.login(req.email(), req.password());
    }

    @PostMapping("/logout")
    void logout(@RequestHeader(value = "Authorization", required = false) String authorization) {
        authService.logout(BearerToken.from(authorization));
    }

    @GetMapping("/me")
    MeResponse me(@RequestHeader(value = "Authorization", required = false) String authorization) {
        Optional<AppUser> user = authService.currentUser(BearerToken.from(authorization));
        return user.map(u -> new MeResponse(true, u.getEmail())).orElse(new MeResponse(false, null));
    }

    @GetMapping("/verify")
    ResponseEntity<Void> verify(@RequestParam String token) {
        boolean valid = authService.isValidVerificationToken(token);
        String location = valid
                ? "/email-verified.html?token=" + token
                : "/email-verified.html?error=1";
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(location)).build();
    }

    @PostMapping("/verify-confirm")
    Map<String, String> verifyConfirm(@Valid @RequestBody TokenRequest req) {
        if (!authService.confirmEmailVerification(req.token())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "הקישור אינו תקין או שפג תוקפו - יש לבקש קישור אימות חדש");
        }
        return Map.of("message", "האימייל אומת בהצלחה! אפשר להתחבר עכשיו.");
    }

    @PostMapping("/resend-verification")
    Map<String, String> resendVerification(@Valid @RequestBody EmailRequest req) {
        authService.resendVerification(req.email());
        return Map.of("message", "אם הכתובת רשומה ועדיין לא אומתה, נשלח אליה מייל אימות חדש");
    }

    @PostMapping("/forgot-password")
    Map<String, String> forgotPassword(@Valid @RequestBody EmailRequest req) {
        authService.requestPasswordReset(req.email());
        return Map.of("message", "אם הכתובת רשומה במערכת, נשלח אליה קישור לאיפוס סיסמה");
    }

    @PostMapping("/reset-password")
    AuthService.AuthResult resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
        return authService.resetPassword(req.token(), req.newPassword());
    }

    @PostMapping("/delete-account")
    Map<String, String> deleteAccount(@Valid @RequestBody DeleteAccountRequest req,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        authService.deleteAccount(BearerToken.from(authorization), req.password());
        return Map.of("message", "החשבון וכל המידע השמור בו נמחקו");
    }
}
