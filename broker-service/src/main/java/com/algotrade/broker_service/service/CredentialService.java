package com.algotrade.broker_service.service;

import com.algotrade.broker_service.dto.BrokerCredentialResponse;
import com.algotrade.broker_service.dto.BrokerTokenResponse;
import com.algotrade.broker_service.model.BrokerCredentials;

public interface CredentialService {

	BrokerCredentials getCredentials(String email, String brokerName);
	
	BrokerCredentialResponse createCredentialResponse(BrokerCredentials credentials);

	void saveCredentials(String email, String brokerName, String apiKey, String apiSecret);

	void evictCredentialsCache(String email, String brokerName);
	
	void updateAccessToken(String email, String brokerName, BrokerTokenResponse accessToken);
	
	String prepareOuthRedirectUrl(String brokerName);

}
