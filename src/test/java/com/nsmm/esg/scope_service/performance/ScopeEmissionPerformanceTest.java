package com.nsmm.esg.scope_service.performance;

import com.nsmm.esg.scope_service.repository.ScopeEmissionRepository;
import com.nsmm.esg.scope_service.service.ScopeEmissionService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;

/**
 * 특수 집계 배출량 조회 성능 테스트
 * 
 * 최적화 전후 성능 비교:
 * - 기존: 50여개 개별 쿼리 실행
 * - 최적화 후: 1개 통합 쿼리 실행
 * - 캐시 적용으로 반복 조회 시 성능 향상
 */
@Slf4j
@SpringBootTest
@ActiveProfiles("test")
public class ScopeEmissionPerformanceTest {

    @Autowired
    private ScopeEmissionRepository scopeEmissionRepository;

    @Autowired
    private ScopeEmissionService scopeEmissionService;

    /**
     * 특수 집계 조회 성능 테스트 - 통합 쿼리 vs 개별 쿼리
     */
    @Test
    void testSpecialAggregationPerformance() {
        Long headquartersId = 1L;
        Integer year = 2024;
        Integer month = 1;

        log.info("=== 특수 집계 성능 테스트 시작 ===");

        // 통합 쿼리 성능 측정
        long startTime = System.currentTimeMillis();
        Map<String, Object> unifiedResults = scopeEmissionService
            .getSpecialAggregationSummaryForHeadquarters(headquartersId, year, month);
        long unifiedQueryTime = System.currentTimeMillis() - startTime;

        log.info("통합 쿼리 결과: {} 개 항목, 소요시간: {}ms", 
                 unifiedResults.size(), unifiedQueryTime);

        // 개별 쿼리 성능 측정 (기존 방식 시뮬레이션)
        startTime = System.currentTimeMillis();
        simulateIndividualQueries(headquartersId, year, month);
        long individualQueryTime = System.currentTimeMillis() - startTime;

        log.info("개별 쿼리 시뮬레이션 소요시간: {}ms", individualQueryTime);

        // 성능 비교 결과 출력
        double improvementRatio = (double) individualQueryTime / unifiedQueryTime;
        log.info("=== 성능 최적화 결과 ===");
        log.info("통합 쿼리: {}ms", unifiedQueryTime);
        log.info("개별 쿼리: {}ms", individualQueryTime);
        log.info("성능 향상 배수: {:.2f}x", improvementRatio);
        log.info("시간 단축률: {:.1f}%", (1 - 1/improvementRatio) * 100);
    }

    /**
     * 캐시 성능 테스트 - 첫 번째 조회 vs 캐시된 조회
     */
    @Test
    void testCachePerformance() {
        Long headquartersId = 1L;
        Integer year = 2024;
        Integer month = 1;

        log.info("=== 캐시 성능 테스트 시작 ===");

        // 첫 번째 조회 (캐시 없음)
        long startTime = System.currentTimeMillis();
        Map<String, Object> firstCall = scopeEmissionService
            .getSpecialAggregationSummaryForHeadquarters(headquartersId, year, month);
        long firstCallTime = System.currentTimeMillis() - startTime;

        // 두 번째 조회 (캐시됨)
        startTime = System.currentTimeMillis();
        Map<String, Object> secondCall = scopeEmissionService
            .getSpecialAggregationSummaryForHeadquarters(headquartersId, year, month);
        long secondCallTime = System.currentTimeMillis() - startTime;

        // 세 번째 조회 (캐시됨)
        startTime = System.currentTimeMillis();
        Map<String, Object> thirdCall = scopeEmissionService
            .getSpecialAggregationSummaryForHeadquarters(headquartersId, year, month);
        long thirdCallTime = System.currentTimeMillis() - startTime;

        log.info("=== 캐시 성능 결과 ===");
        log.info("첫 번째 조회 (캐시 없음): {}ms", firstCallTime);
        log.info("두 번째 조회 (캐시됨): {}ms", secondCallTime);
        log.info("세 번째 조회 (캐시됨): {}ms", thirdCallTime);
        
        double cacheImprovement = (double) firstCallTime / secondCallTime;
        log.info("캐시 성능 향상: {:.2f}x", cacheImprovement);
        log.info("캐시 시간 단축률: {:.1f}%", (1 - 1/cacheImprovement) * 100);
    }

    /**
     * 카테고리별 집계 성능 테스트
     */
    @Test
    void testCategoryWisePerformance() {
        Long headquartersId = 1L;
        Long partnerId = null; // 본사 데이터
        Integer year = 2024;
        Integer month = null; // 전체 월

        log.info("=== 카테고리별 집계 성능 테스트 시작 ===");

        long startTime = System.currentTimeMillis();
        List<Map<String, Object>> categoryResults = scopeEmissionService
            .getCategoryWiseEmissions(headquartersId, partnerId, year, month);
        long queryTime = System.currentTimeMillis() - startTime;

        log.info("카테고리별 집계 결과: {} 개 항목, 소요시간: {}ms", 
                 categoryResults.size(), queryTime);

        // Scope3 전체 카테고리 조회 성능 테스트
        startTime = System.currentTimeMillis();
        List<Map<String, Object>> scope3Results = scopeEmissionService
            .getScope3AllCategoriesEmissions(headquartersId, partnerId, year, month);
        long scope3QueryTime = System.currentTimeMillis() - startTime;

        log.info("Scope3 카테고리별 집계 결과: {} 개 항목, 소요시간: {}ms", 
                 scope3Results.size(), scope3QueryTime);
    }

    /**
     * 개별 쿼리 방식 시뮬레이션 (기존 방식)
     * 실제로는 이렇게 많은 개별 쿼리가 실행됨
     */
    private void simulateIndividualQueries(Long headquartersId, Integer year, Integer month) {
        // 시뮬레이션: 기존 방식의 개별 쿼리들 (실제 쿼리 수행하지 않고 시간만 측정)
        
        // Scope1 이동연소 그룹 쿼리들 (카테고리 4,5,6)
        scopeEmissionRepository.sumScope1MobileCombustionEmissionsByYearAndMonthForHeadquarters(
            headquartersId, year, month);
        
        // Scope1 폐수처리 쿼리
        scopeEmissionRepository.sumScope1WasteWaterTreatmentEmissionsByYearAndMonthForHeadquarters(
            headquartersId, year, month);
        
        // Scope1 공장설비 쿼리
        scopeEmissionRepository.sumScope1FactoryEmissionsByYearAndMonthForHeadquarters(
            headquartersId, year, month);
        
        // Scope2 공장설비 쿼리
        scopeEmissionRepository.sumScope2FactoryEmissionsByYearAndMonthForHeadquarters(
            headquartersId, year, month);
        
        // Scope1 전체 쿼리
        scopeEmissionRepository.sumScope1TotalEmissionsByYearAndMonthForHeadquarters(
            headquartersId, year, month);
        
        // Scope2 전체 쿼리
        scopeEmissionRepository.sumScope2TotalEmissionsByYearAndMonthForHeadquarters(
            headquartersId, year, month);
        
        // Scope3 각 카테고리별 쿼리 (1~15번)
        for (int categoryNumber = 1; categoryNumber <= 15; categoryNumber++) {
            scopeEmissionRepository.sumScope3CategoryEmissionsByYearAndMonthForHeadquarters(
                headquartersId, categoryNumber, year, month);
        }
        
        log.info("개별 쿼리 시뮬레이션 완료: 총 21개 쿼리 실행");
    }
}