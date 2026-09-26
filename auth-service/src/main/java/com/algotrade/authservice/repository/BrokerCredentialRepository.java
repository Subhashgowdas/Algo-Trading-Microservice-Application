package com.algotrade.authservice.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.algotrade.authservice.model.BrokerCredential;
import com.algotrade.authservice.model.EntityStatus;

@Repository
public interface BrokerCredentialRepository extends JpaRepository<BrokerCredential, Long> {
	
	Optional<BrokerCredential> findByUserIdAndBrokerNameAndStatus(Long UserId,String brokerName, EntityStatus status);
	
	Optional<BrokerCredential> findByUserEmailAndBrokerNameAndStatus(String emailId,String brokerName, EntityStatus status);

	boolean existsByUserIdAndBrokerNameAndStatus(Long UserId,String brokerName, EntityStatus status);

}
