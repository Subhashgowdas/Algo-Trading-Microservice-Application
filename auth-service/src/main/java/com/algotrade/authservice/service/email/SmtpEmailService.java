package com.algotrade.authservice.service.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.algotrade.common.email.EmailService;

import jakarta.mail.internet.MimeMessage;

@Service
@ConditionalOnProperty(name = "app.email.provider", havingValue = "smtp")
public class SmtpEmailService implements EmailService {

	private static final Logger log = LoggerFactory.getLogger(SmtpEmailService.class);
	private final JavaMailSender mailSender;

	public SmtpEmailService(JavaMailSender mailSender) {
		this.mailSender = mailSender;
	}

	@Override
	public void sendEmail(String to, String subject, String body) {
		try {
			MimeMessage message = mailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
			helper.setTo(to);
			helper.setSubject(subject);
			helper.setText(body, true); // HTML content
			mailSender.send(message);
			log.info("Email sent to {} with subject '{}'", to, subject);
		} catch (Exception e) {
			log.error("Failed to send email to {}", to, e);
			throw new RuntimeException("Email sending failed", e);
		}
	}
}