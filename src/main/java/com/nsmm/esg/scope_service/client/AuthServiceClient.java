package com.nsmm.esg.scope_service.client;

import com.nsmm.esg.scope_service.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Auth-Service Feign Client
 * 
 * UUID를 비즈니스 ID로 변환하기 위한 Auth-Service 연동
 */
@FeignClient(name = "auth-service", url = "${auth-service.url:http://localhost:8081}")
public interface AuthServiceClient {

    /**
     * UUID를 비즈니스 ID로 변환 (내부 서비스 전용)
     * 
     * @param uuid 협력사 UUID
     * @return 비즈니스 ID (L1-001, L2-001 등)
     */
    @GetMapping("/api/v1/auth/partners/internal/uuid-to-business-id/{uuid}")
    ApiResponse<String> getBusinessIdByUuid(@PathVariable("uuid") String uuid);
}