package com.algotrade.authservice.controller;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.algotrade.authservice.model.BrokerCredential;
import com.algotrade.authservice.model.EntityStatus;
import com.algotrade.authservice.model.User;
import com.algotrade.authservice.repository.BrokerCredentialRepository;
import com.algotrade.authservice.repository.UserRepository;

@RestController
@RequestMapping("/api/internal")
public class InternalController {

	private final BrokerCredentialRepository brokerCredentialRepository;
	private final UserRepository userRepository;

	public InternalController(BrokerCredentialRepository brokerCredentialRepository
			,UserRepository userRepository) {
		this.brokerCredentialRepository = brokerCredentialRepository;
		this.userRepository = userRepository;
	}

	@GetMapping("/broker-credentials/{brokerName}")
	public ResponseEntity<Map<String, Map<String, Object>>> getBrokerCredentials(
			@PathVariable String brokerName,
			@RequestHeader("X-User-Email") String email) {

		// Look for an existing credential record
		BrokerCredential credential = brokerCredentialRepository
				.findByUserEmailAndBrokerNameAndStatus(email, brokerName, EntityStatus.ACTV)
				.orElse(null);

		if(credential != null) {
			return ResponseEntity.ok(
					Map.of(brokerName, credential.getBrokerApiConfig()));
		}
		return ResponseEntity.status(HttpStatus.NOT_FOUND).build(); 
	}

	/**
	 * Create or update broker credentials for a user.
	 *
	 * Headers required:
	 *   Authorization: Bearer <service-token>
	 *   X-User-Email: user@example.com
	 */
	@PostMapping("/broker-credentials/{brokerName}")
	public ResponseEntity<Map<String, String>> saveBrokerCredentials(
			@PathVariable String brokerName,
			@RequestHeader("X-User-Email") String email,
			@RequestBody Map<String, String> request) {

		// Find the user by email
		User user = userRepository.findByEmail(email)
				.orElseThrow(() -> new RuntimeException("User not found for email: " + email));

		// Look for an existing credential record
		BrokerCredential credential = brokerCredentialRepository
				.findByUserIdAndBrokerNameAndStatus(user.getId(), brokerName, EntityStatus.ACTV)
				.orElse(null);

		if (credential != null) {
			// Update existing – replace the whole JSON config
			credential.setBrokerApiConfig(new HashMap<>(request));
			credential.setUpdatedAt(LocalDateTime.now());
			brokerCredentialRepository.save(credential);
			return ResponseEntity.ok(Map.of("message", "Broker credentials updated"));
		} else {
			// Create new
			BrokerCredential newCred = BrokerCredential.builder()
					.user(user)
					.brokerName(brokerName)
					.brokerApiConfig(new HashMap<>(request))
					.build();
			brokerCredentialRepository.save(newCred);
			return ResponseEntity.status(HttpStatus.CREATED)
					.body(Map.of("message", "Broker credentials created"));
		}
	}

}
