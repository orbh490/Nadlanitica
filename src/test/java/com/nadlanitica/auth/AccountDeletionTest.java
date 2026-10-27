package com.nadlanitica.auth;

import com.nadlanitica.account.InterestArea;
import com.nadlanitica.account.InterestAreaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AccountDeletionTest {

    private final HttpClient client = HttpClient.newHttpClient();

    @Value("${local.server.port}")
    int port;
    @Autowired
    AppUserRepository users;
    @Autowired
    UserSessionRepository sessions;
    @Autowired
    InterestAreaRepository interestAreas;

    @Test
    void requiresLogin() throws Exception {
        assertThat(deleteAccount(null, "secret123").statusCode()).isEqualTo(401);
    }

    @Test
    void wrongPasswordKeepsTheAccount() throws Exception {
        createLoggedInUser("del-wrong", "secret123");
        var res = deleteAccount("del-wrong-token", "not-my-password");
        assertThat(res.statusCode()).isEqualTo(403);
        assertThat(res.body()).contains("הסיסמה שגויה");
        assertThat(users.existsById("del-wrong")).isTrue();
    }

    @Test
    void correctPasswordDeletesTheUserAndAllTheirData() throws Exception {
        createLoggedInUser("del-ok", "secret123");
        interestAreas.save(new InterestArea("del-ok-area", "del-ok", "תל אביב"));

        var res = deleteAccount("del-ok-token", "secret123");

        assertThat(res.statusCode()).isEqualTo(200);
        assertThat(users.existsById("del-ok")).isFalse();
        assertThat(sessions.existsById("del-ok-token")).isFalse();
        assertThat(interestAreas.existsById("del-ok-area")).isFalse();
    }

    private void createLoggedInUser(String id, String password) {
        AppUser user = new AppUser(id, id + "@example.test", new BCryptPasswordEncoder().encode(password));
        user.setEmailVerified(true);
        users.save(user);
        sessions.save(new UserSession(id + "-token", id, Instant.now().plus(Duration.ofDays(1))));
    }

    private HttpResponse<String> deleteAccount(String token, String password) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/auth/delete-account"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"password\":\"" + password + "\"}"));
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }
}
