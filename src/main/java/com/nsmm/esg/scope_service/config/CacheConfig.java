package com.nsmm.esg.scope_service.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 캐시 설정 클래스
 * 
 * 특수 집계 배출량 조회 성능 최적화를 위한 캐시 설정
 * - 월별 확정 데이터: 장기 캐시 (1시간)
 * - 실시간 데이터: 단기 캐시 (5분)
 */
@Configuration
@EnableCaching
public class CacheConfig {

    // CacheManager 빈 등록
    @Bean
    public CacheManager cacheManager() {
        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager();
        
        // 캐시 이름들 사전 등록
        cacheManager.setCacheNames(java.util.Arrays.asList(
            "specialAggregationCache",      // 특수 집계 결과 캐시
            "categoryWiseCache",           // 카테고리별 집계 캐시  
            "scope3CategoriesCache",       // Scope3 카테고리 집계 캐시
            "monthlyEmissionCache",        // 월별 배출량 캐시
            "partnerEmissionCache"         // 협력사별 배출량 캐시
        ));
        
        return cacheManager;
    }
}