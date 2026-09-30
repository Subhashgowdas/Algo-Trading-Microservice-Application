package com.algotrade.broker_service.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.algotrade.broker_service.model.BrokerProfile;
import com.algotrade.broker_service.model.OrderRequest;
import com.algotrade.broker_service.model.OrderResponse;
import com.algotrade.broker_service.model.Position;
import com.algotrade.broker_service.serviceImpl.BrokerServiceImpl;

@RestController
@RequestMapping("/api/broker")
public class OrderController {

	private final BrokerServiceImpl brokerService;

	public OrderController(BrokerServiceImpl brokerService) {
		this.brokerService = brokerService;
	}

	@GetMapping("/{brokerName}/positions")
	public ResponseEntity<List<Position>> getPositions(@PathVariable String brokerName) {
		return ResponseEntity.ok(brokerService.getPositions(brokerName));
	}

	@GetMapping("/{brokerName}/profile")
	public ResponseEntity<BrokerProfile> getProfile(@PathVariable String brokerName) {
		return ResponseEntity.ok(brokerService.getProfile(brokerName));
	}

	@PostMapping("/{brokerName}/order")
	public ResponseEntity<OrderResponse> placeOrder(
			@PathVariable String brokerName,
			@RequestBody OrderRequest request) {

		// The BrokerService automatically extracts the user from the SecurityContext
		OrderResponse response = brokerService.placeOrder(brokerName, request);
		return ResponseEntity.ok(response);
	}

}
