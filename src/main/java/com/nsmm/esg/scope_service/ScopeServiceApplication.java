package com.nsmm.esg.scope_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
@EnableJpaAuditing // JPA Auditing 활성화 (생성일/수정일 자동 설정)
public class ScopeServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(ScopeServiceApplication.class, args);
	}

}
