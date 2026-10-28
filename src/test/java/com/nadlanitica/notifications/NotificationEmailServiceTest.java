package com.nadlanitica.notifications;

import jakarta.mail.BodyPart;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.ITemplateEngine;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** בונה מייל אמיתי (עם מנוע התבניות של האפליקציה) ותופס אותו לפני השליחה, במקום לשלוח. */
@SpringBootTest
class NotificationEmailServiceTest {

    @Autowired
    ITemplateEngine templateEngine;

    @Test
    void customerEmailHasHtmlAndTextVersionsFromTheDomainWithReplyToContact() throws Exception {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));
        NotificationEmailService service = new NotificationEmailService(mailSender, templateEngine,
                "alerts@example.test", "noreply@example.test", "נדלניטיקה", "contact@example.test",
                "https://nadlanitica.example");

        // קריאה ישירה לאובייקט (לא דרך Spring) רצה מיד ולא ברקע, אז אפשר לבדוק את התוצאה מיד
        service.send("user@example.test", "אימות כתובת אימייל", new NotificationEmailService.EmailContent(
                "ברוכים הבאים לנדלניטיקה!", List.of("תודה שנרשמתם."), "אימות כתובת האימייל",
                "https://nadlanitica.example/api/auth/verify?token=abc", "הקישור בתוקף ל-48 שעות."));

        ArgumentCaptor<MimeMessage> sent = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(sent.capture());
        MimeMessage message = sent.getValue();
        message.saveChanges();

        InternetAddress from = (InternetAddress) message.getFrom()[0];
        assertThat(from.getAddress()).isEqualTo("noreply@example.test");
        assertThat(from.getPersonal()).isEqualTo("נדלניטיקה");
        assertThat(((InternetAddress) message.getReplyTo()[0]).getAddress()).isEqualTo("contact@example.test");
        assertThat(message.getSubject()).isEqualTo("[נדלניטיקה] אימות כתובת אימייל");

        Map<String, String> parts = new HashMap<>();
        collectTextParts(message, parts);
        assertThat(parts.get("text/plain"))
                .contains("ברוכים הבאים לנדלניטיקה!")
                .contains("https://nadlanitica.example/api/auth/verify?token=abc");
        assertThat(parts.get("text/html"))
                .contains("dir=\"rtl\"")
                .contains("ברוכים הבאים לנדלניטיקה!")
                .contains("href=\"https://nadlanitica.example/api/auth/verify?token=abc\"")
                .contains("https://nadlanitica.example/img/icon-192.png");
    }

    private static void collectTextParts(Part part, Map<String, String> out) throws Exception {
        if (part.isMimeType("text/plain") || part.isMimeType("text/html")) {
            out.put(part.isMimeType("text/html") ? "text/html" : "text/plain", (String) part.getContent());
        } else if (part.getContent() instanceof Multipart multipart) {
            for (int i = 0; i < multipart.getCount(); i++) {
                BodyPart child = multipart.getBodyPart(i);
                collectTextParts(child, out);
            }
        }
    }
}
