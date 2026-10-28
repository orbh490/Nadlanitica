package com.nadlanitica.notifications;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.Context;

import java.io.UnsupportedEncodingException;
import java.util.List;

/**
 * שליחת מיילים. ללקוחות: מייל מעוצב (templates/email/standard.html) יחד עם גרסת טקסט פשוט באותה הודעה,
 * כך שכל תוכנת דואר מציגה משהו קריא. ההודעות יוצאות מ-app.mail.from (כתובת על הדומיין, מאומתת אצל שירות
 * השליחה) ותשובות מגיעות ל-app.contact-email.
 */
@Service
public class NotificationEmailService {

    private static final Logger log = LoggerFactory.getLogger(NotificationEmailService.class);

    /** תוכן מייל ללקוח: כותרת, פסקאות, כפתור אופציונלי (טקסט + קישור) והערה קטנה בסוף. */
    public record EmailContent(String heading, List<String> paragraphs, String buttonText, String buttonUrl,
            String footnote) {
    }

    private final JavaMailSender mailSender;
    private final ITemplateEngine templateEngine;
    private final String alertsTo;
    private final String from;
    private final String fromName;
    private final String replyTo;
    private final String baseUrl;

    public NotificationEmailService(JavaMailSender mailSender, ITemplateEngine templateEngine,
            @Value("${app.notifications.to}") String alertsTo, @Value("${app.mail.from:}") String from,
            @Value("${app.mail.from-name:נדלניטיקה}") String fromName, @Value("${app.contact-email}") String replyTo,
            @Value("${app.base-url}") String baseUrl) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.alertsTo = alertsTo;
        this.from = from;
        this.fromName = fromName;
        this.replyTo = replyTo;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    /** התראות תפעוליות למפתח בלבד - טקסט פשוט, לכתובת מ-app.notifications.to. */
    @Async
    public void sendAlert(String subject, String body) {
        deliver(alertsTo, subject, body, null);
    }

    /**
     * מיילים ללקוחות עצמם (אימות אימייל, איפוס סיסמה וכו') - לכתובת שהמשתמש הזין בהרשמה.
     * רץ ברקע (@Async): הרשמה ואיפוס סיסמה לא מחכים לשרת ה-SMTP, שיכול לקחת כמה שניות.
     */
    @Async
    public void send(String to, String subject, EmailContent content) {
        deliver(to, subject, plainText(content), html(content));
    }

    String html(EmailContent content) {
        Context context = new Context();
        context.setVariable("heading", content.heading());
        context.setVariable("paragraphs", content.paragraphs());
        context.setVariable("buttonText", content.buttonText());
        context.setVariable("buttonUrl", content.buttonUrl());
        context.setVariable("footnote", content.footnote());
        context.setVariable("baseUrl", baseUrl);
        return templateEngine.process("email/standard", context);
    }

    static String plainText(EmailContent content) {
        StringBuilder text = new StringBuilder(content.heading()).append("\n\n");
        content.paragraphs().forEach(p -> text.append(p).append("\n\n"));
        if (content.buttonUrl() != null) {
            text.append(content.buttonText()).append(":\n").append(content.buttonUrl()).append("\n\n");
        }
        if (content.footnote() != null) {
            text.append(content.footnote()).append("\n");
        }
        return text.toString();
    }

    private void deliver(String to, String subject, String text, String html) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, html != null, "UTF-8");
            if (!from.isBlank()) {
                helper.setFrom(from, fromName);
            }
            if (!replyTo.isBlank()) {
                helper.setReplyTo(replyTo);
            }
            helper.setTo(to);
            helper.setSubject("[נדלניטיקה] " + subject);
            if (html != null) {
                helper.setText(text, html);
            } else {
                helper.setText(text);
            }
            mailSender.send(message);
            log.info("מייל נשלח בהצלחה אל {}: {}", to, subject);
        } catch (MailException | MessagingException | UnsupportedEncodingException e) {
            log.warn("שליחת מייל אל {} נכשלה (בדוק את הגדרות MAIL_*): {}", to, e.getMessage());
        }
    }
}
