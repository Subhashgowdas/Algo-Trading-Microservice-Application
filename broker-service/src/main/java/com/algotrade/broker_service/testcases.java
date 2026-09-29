package com.algotrade.broker_service;

import java.util.ArrayList;
import java.util.List;

import com.algotrade.broker_service.model.BrokerCredentials;

import io.jsonwebtoken.lang.Arrays;

// this class for test Environment variable

//@Component
//public class EnvDebugRunner implements CommandLineRunner {
//
//    @Override
//    public void run(String... args) {
//        System.out.println("******** RUNNER EXECUTED ********");
//        
//        System.out.println("BROKER_SERVICE_TOKEN ="  + System.getenv("BROKER_SERVICE_TOKEN"));
//        
//        System.out.println("ENCRYPTION_SECRET_KEY ="  + System.getenv("ENCRYPTION_SECRET_KEY"));
//    }
//}

public class testcases{
	
//	private BrokerCredentialRepository repo;
	
	public static void main(String[] args) {
		int[] val = {10,20,30,66,50,66};
		
		int findele = 66;
		
		for(int i = 0 ;i<val.length;i++) {
			if(val[i] == findele) {
				System.out.println(i);
				break;
			}
		}
		
	}
}


