package com.nadlanitica.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

/** אי אפשר לפתוח חשבון שני על אותה תיבה עם +כינוי או נקודות בג'ימייל; ואפשר להתחבר עם כל צורה של הכתובת. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OneAccountPerMailboxTest {

    private final HttpClient client = HttpClient.newHttpClient();

    @Value("${local.server.port}")
    int port;
    @Autowired
    AppUserRepository users;

    @Test
    void secondAccountOnTheSameMailboxIsRejected() throws Exception {
        assertThat(register("mailbox.owner@gmail.com").statusCode()).isEqualTo(200);

        var alias = register("mailboxowner+2@gmail.com");
        assertThat(alias.statusCode()).isEqualTo(409);
        assertThat(alias.body()).contains("כבר קיים חשבון על תיבת המייל הזו");
        assertThat(register("Mailbox.Owner@googlemail.com").statusCode()).isEqualTo(409);

        assertThat(users.findByMailboxKey("mailboxowner@gmail.com")).hasSize(1);
    }

    @Test
    void differentMailboxesStillRegister() throws Exception {
        assertThat(register("first.person@outlook.com").statusCode()).isEqualTo(200);
        assertThat(register("firstperson@outlook.com").statusCode()).isEqualTo(200);
    }

    @Test
    void loginWorksWithAnotherSpellingOfTheSameMailbox() throws Exception {
        AppUser user = new AppUser("mbx-login", "dana.levi@gmail.com", new BCryptPasswordEncoder().encode("secret123"));
        user.setEmailVerified(true);
        users.save(user);

        var res = post("/api/auth/login", "{\"email\":\"danalevi+site@gmail.com\",\"password\":\"secret123\"}");
        assertThat(res.statusCode()).isEqualTo(200);
        assertThat(res.body()).contains("dana.levi@gmail.com");
    }

    private HttpResponse<String> register(String email) throws Exception {
        return post("/api/auth/register", "{\"email\":\"" + email + "\",\"password\":\"secret123\",\"consent\":true}");
    }

    private HttpResponse<String> post(String path, String json) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(json)).build(),
                HttpResponse.BodyHandlers.ofString());
    }
}
