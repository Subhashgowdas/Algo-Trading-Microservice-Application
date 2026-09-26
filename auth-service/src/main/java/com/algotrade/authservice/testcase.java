package com.algotrade.authservice;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.algotrade.authservice.model.BrokerCredential;
import com.algotrade.authservice.repository.BrokerCredentialRepository;

@Service
public class testcase {
	
	@Autowired
	private BrokerCredentialRepository repo;
	
	public void main() {
		
		List<BrokerCredential> broc = new ArrayList<>();
		
		
		
	}
	

}
