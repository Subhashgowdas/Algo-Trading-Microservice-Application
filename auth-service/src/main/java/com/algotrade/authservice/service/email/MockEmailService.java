package com.algotrade.authservice.service.email;

import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import com.algotrade.common.email.EmailService;

@Service
@ConditionalOnProperty(name = "app.email.provider", havingValue = "mock", matchIfMissing = true)
public class MockEmailService implements EmailService{
	
	private static final org.slf4j.Logger log = LoggerFactory.getLogger(MockEmailService.class);

	@Override
	public void sendEmail(String to, String subject, String body) {
		log.info("--------MOCK EMAIL--------");
		log.info("To : {}",to);
		log.info("Subject : {}",subject);
		log.info("Body : {}",body);
	}
}
 