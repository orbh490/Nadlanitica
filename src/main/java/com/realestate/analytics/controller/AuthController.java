package com.realestate.analytics.controller;

import com.realestate.analytics.model.AppUser;
import com.realestate.analytics.service.AuthService;
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

record AuthRequest(String email, String password, Boolean consent) {
}

record MeResponse(boolean loggedIn, String email) {
}

record EmailRequest(String email) {
}

record ResetPasswordRequest(String token, String newPassword) {
}

record TokenRequest(String token) {
}

@RestController
@RequestMapping("/api/auth")
class AuthController {

    private final AuthService authService;

    AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    Map<String, String> register(@RequestBody AuthRequest req) {
        authService.register(req.email(), req.password(), Boolean.TRUE.equals(req.consent()));
        return Map.of("message", "נשלח מייל אימות ל-" + req.email() + " - יש לאמת את הכתובת לפני ההתחברות");
    }

    @PostMapping("/login")
    AuthService.AuthResult login(@RequestBody AuthRequest req) {
        return authService.login(req.email(), req.password());
    }

    @PostMapping("/logout")
    void logout(@RequestHeader(value = "Authorization", required = false) String authorization) {
        authService.logout(bearerToken(authorization));
    }

    @GetMapping("/me")
    MeResponse me(@RequestHeader(value = "Authorization", required = false) String authorization) {
        Optional<AppUser> user = authService.currentUser(bearerToken(authorization));
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
    Map<String, String> verifyConfirm(@RequestBody TokenRequest req) {
        if (!authService.confirmEmailVerification(req.token())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "הקישור אינו תקין או שפג תוקפו - יש לבקש קישור אימות חדש");
        }
        return Map.of("message", "האימייל אומת בהצלחה! אפשר להתחבר עכשיו.");
    }

    @PostMapping("/resend-verification")
    Map<String, String> resendVerification(@RequestBody EmailRequest req) {
        authService.resendVerification(req.email());
        return Map.of("message", "אם הכתובת רשומה ועדיין לא אומתה, נשלח אליה מייל אימות חדש");
    }

    @PostMapping("/forgot-password")
    Map<String, String> forgotPassword(@RequestBody EmailRequest req) {
        authService.requestPasswordReset(req.email());
        return Map.of("message", "אם הכתובת רשומה במערכת, נשלח אליה קישור לאיפוס סיסמה");
    }

    @PostMapping("/reset-password")
    AuthService.AuthResult resetPassword(@RequestBody ResetPasswordRequest req) {
        return authService.resetPassword(req.token(), req.newPassword());
    }

    static String bearerToken(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return null;
        }
        return authorizationHeader.substring("Bearer ".length()).trim();
    }
}
