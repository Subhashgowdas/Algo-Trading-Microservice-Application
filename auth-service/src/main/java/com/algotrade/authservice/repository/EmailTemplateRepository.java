package com.algotrade.authservice.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.algotrade.authservice.model.EmailTemplate;
import com.algotrade.authservice.model.EntityStatus;

public interface EmailTemplateRepository extends JpaRepository<EmailTemplate, Long> {
	 Optional<EmailTemplate> findByCodeAndStatus(String code, EntityStatus status);
}