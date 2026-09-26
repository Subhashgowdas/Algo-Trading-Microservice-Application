package com.algotrade.authservice.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class CorsConfigTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void shouldAllowCorsPreflight() throws Exception {
		mockMvc.perform(options("/api/auth/login")
				.header("Origin", "http://localhost:3000")
				.header("Access-Control-Request-Method", "POST")
				.header("Access-Control-Request-Headers", "Authorization, Content-Type"))
		.andExpect(status().isOk())
		.andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"))
		.andExpect(header().string("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS"))
		.andExpect(header().string("Access-Control-Allow-Headers", "Authorization, Content-Type"))
		.andExpect(header().string("Access-Control-Allow-Credentials", "true"));
	}
}