package com.algotrade.authservice.service.email;

import java.util.Map;

import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.StringTemplateResolver;

import com.algotrade.authservice.model.EmailTemplate;
import com.algotrade.authservice.model.EntityStatus;
import com.algotrade.authservice.repository.EmailTemplateRepository;

@Service
public class EmailTemplateService {

	private final EmailTemplateRepository repository;
	private final SpringTemplateEngine templateEngine;

	public EmailTemplateService(EmailTemplateRepository repository) {
		this.repository = repository;
		// Setup Thymeleaf to handle in‑memory templates
		StringTemplateResolver resolver = new StringTemplateResolver();
		resolver.setCacheable(false); // templates are in DB, no caching for dev
		this.templateEngine = new SpringTemplateEngine();
		this.templateEngine.setTemplateResolver(resolver);
	}

	/**
	 * Renders an email template by code.
	 * @param code  template code (REG_OTP, RESET_PWD, …)
	 * @param model map of variables to insert into the template
	 * @return RenderedEmail containing subject and HTML body
	 */
	public RenderedEmail render(String code, Map<String, Object> model) {
		EmailTemplate template = repository.findByCodeAndStatus(code, EntityStatus.ACTV)
				.orElseThrow(() -> new IllegalArgumentException("Email template not found: " + code));

		Context context = new Context();
		model.forEach(context::setVariable);

		String renderedBody = templateEngine.process(template.getBody(), context);
		// Also render the subject (may contain Thymeleaf too)
		String renderedSubject = templateEngine.process(template.getSubject(), context);

		return new RenderedEmail(renderedSubject, renderedBody);
	}

	public record RenderedEmail(String subject, String body) {}
}