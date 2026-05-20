package com.murasame.service;

import jakarta.annotation.Resource;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class MailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    @Resource
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String from;

    public boolean sendVerificationCode(String toEmail, String code) {
        try {
            if (from == null || from.isBlank()) {
                log.error("Mail from address is not configured (spring.mail.username)");
                return false;
            }
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(from.strip());
            helper.setTo(toEmail.strip());
            helper.setSubject("MyBlog - 邮箱验证码");

            // 内联HTML邮件模板：深色背景 + 青色高亮，展示6位验证码及5分钟有效期提示
            String html = "<div style=\"max-width:480px;margin:0 auto;padding:24px;font-family:Arial,sans-serif;background:#1a1e2b;color:#fff;border-radius:12px;\">"
                    + "<h2 style=\"color:#87CEEB;\">MyBlog - 邮箱验证</h2>"
                    + "<p>亲爱的客官，您的验证码是：</p>"
                    + "<div style=\"font-size:32px;font-weight:bold;letter-spacing:8px;color:#87CEEB;padding:16px;background:rgba(135,206,235,0.1);border-radius:8px;text-align:center;\">"
                    + code + "</div>"
                    + "<p style=\"color:rgba(255,255,255,0.5);font-size:13px;\">验证码 5 分钟内有效。如非本人操作请忽略。</p>"
                    + "</div>";

            helper.setText(html, true);
            mailSender.send(message);
            log.info("Verification code sent to {}", toEmail);
            return true;
        } catch (MailException | MessagingException e) {
            log.error("Failed to send verification code to {}: {}", toEmail, e.getMessage());
            return false;
        }
    }
}
