package com.nadlanitica.notifications;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class NotificationEmailService {

    private static final Logger log = LoggerFactory.getLogger(NotificationEmailService.class);

    private final JavaMailSender mailSender;
    private final String toAddress;

    public NotificationEmailService(JavaMailSender mailSender, @Value("${app.notifications.to}") String toAddress) {
        this.mailSender = mailSender;
        this.toAddress = toAddress;
    }

    /** התראות תפעוליות למפתח בלבד (כתובת קבועה מ-app.notifications.to). */
    public void sendAlert(String subject, String body) {
        send(toAddress, subject, body);
    }

    /** מיילים ללקוחות עצמם (אימות אימייל, איפוס סיסמה וכו') - לכתובת שהמשתמש הזין בהרשמה. */
    public void send(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject("[נדלניטיקה] " + subject);
            message.setText(body);
            mailSender.send(message);
            log.info("מייל נשלח בהצלחה אל {}: {}", to, subject);
        } catch (MailException e) {
            log.warn("שליחת מייל אל {} נכשלה (בדוק MAIL_USERNAME/MAIL_PASSWORD): {}", to, e.getMessage());
        }
    }
}
