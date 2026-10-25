package com.monikoshop.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Sends e-mails through SMTP. If SPRING_MAIL_HOST is not configured, nothing is sent
 * (a warning is logged) and the app keeps working.
 */
@Service
@RequiredArgsConstructor
public class MailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    /** Sender address; defaults to the SMTP username. */
    @Value("${MAIL_FROM:}")
    private String mailFrom;

    @Value("${spring.mail.username:}")
    private String smtpUsername;

    /** Public address of the website, used to build the reset link. */
    @Value("${APP_BASE_URL:https://moniko.ir}")
    private String appBaseUrl;

    /** Sends in a background thread so the response time does not reveal whether the e-mail exists. */
    public void sendPasswordResetAsync(String to, String fullName, String token) {
        JavaMailSender sender = mailSenderProvider.getIfAvailable();
        if (sender == null) {
            log.warn("Mail is not configured (SPRING_MAIL_HOST is empty); password reset e-mail was not sent.");
            return;
        }

        String base = appBaseUrl == null ? "" : appBaseUrl.trim().replaceAll("/+$", "");
        String link = base + "/reset-password?token=" + token;
        String from = (mailFrom != null && !mailFrom.isBlank()) ? mailFrom.trim() : smtpUsername;
        String name = (fullName == null || fullName.isBlank()) ? "کاربر گرامی" : fullName;

        CompletableFuture.runAsync(() -> {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                if (from != null && !from.isBlank()) {
                    message.setFrom(from);
                }
                message.setTo(to);
                message.setSubject("بازیابی رمز عبور مونیکوشاپ");
                message.setText(name + " عزیز،\n\n"
                        + "درخواست بازیابی رمز عبور برای حساب شما ثبت شد. برای تعیین رمز جدید روی لینک زیر کلیک کنید:\n\n"
                        + link + "\n\n"
                        + "این لینک ۳۰ دقیقه معتبر است و فقط یک‌بار قابل استفاده است.\n"
                        + "اگر شما این درخواست را نداده‌اید، این ایمیل را نادیده بگیرید؛ رمز عبور شما تغییری نکرده است.\n");
                sender.send(message);
            } catch (Exception e) {
                log.error("Failed to send password reset e-mail: {}", e.getMessage());
            }
        });
    }
}
