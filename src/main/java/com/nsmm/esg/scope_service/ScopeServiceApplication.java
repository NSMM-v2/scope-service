package com.nsmm.esg.scope_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient

public class ScopeServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(ScopeServiceApplication.class, args);
	}

}
