package com.nadlanitica.auth;

/** מחלץ את טוקן ההתחברות מכותרת Authorization: Bearer <token> - משותף לכל הבקרים שדורשים משתמש מחובר. */
public final class BearerToken {

    private BearerToken() {
    }

    public static String from(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return null;
        }
        return authorizationHeader.substring("Bearer ".length()).trim();
    }
}
