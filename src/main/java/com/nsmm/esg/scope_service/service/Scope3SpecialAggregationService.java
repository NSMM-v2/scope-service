package com.nsmm.esg.scope_service.service;

import com.nsmm.esg.scope_service.dto.response.Scope3SpecialAggregationResponse;
import com.nsmm.esg.scope_service.repository.ScopeEmissionRepository;
import com.nsmm.esg.scope_service.service.ScopeEmissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Scope 3 특수 집계 서비스
 * 
 * 특수 집계 규칙:
 * - Cat.1: (Scope1 전체 - 이동연소 - 공장설비 - 폐수처리) + (Scope2 - 공장설비) + Scope3 Cat.1
 * - Cat.2: Scope1 공장설비 + Scope2 공장설비 + Scope3 Cat.2
 * - Cat.4: Scope1 이동연소 + Scope3 Cat.4
 * - Cat.5: Scope1 폐수처리 + Scope3 Cat.5
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class Scope3SpecialAggregationService {

    private final ScopeEmissionRepository scopeEmissionRepository;
    private final ScopeEmissionService scopeEmissionService;

    /**
     * 특수 집계 실행 - 로그인된 사용자 기준 (최적화된 통합 쿼리 방식)
     */
    @Transactional(readOnly = true)
    public Scope3SpecialAggregationResponse getSpecialAggregation(
            Integer year,
            Integer month,
            Long headquartersId,
            String userType,
            Long partnerId,
            String treePath) {

        long startTime = System.currentTimeMillis();

        // 통합 쿼리를 사용한 최적화된 집계 실행
        Scope3SpecialAggregationResponse.Category1Detail category1Detail = calculateCategory1Optimized(
                year, month, headquartersId, userType, partnerId, treePath);
        Scope3SpecialAggregationResponse.Category2Detail category2Detail = calculateCategory2Optimized(
                year, month, headquartersId, userType, partnerId, treePath);
        Scope3SpecialAggregationResponse.Category4Detail category4Detail = calculateCategory4Optimized(
                year, month, headquartersId, userType, partnerId, treePath);
        Scope3SpecialAggregationResponse.Category5Detail category5Detail = calculateCategory5Optimized(
                year, month, headquartersId, userType, partnerId, treePath);

        // 응답 생성
        Scope3SpecialAggregationResponse response = Scope3SpecialAggregationResponse.builder()
                .reportingYear(year)
                .reportingMonth(month)
                .userType(userType)
                .organizationId("HEADQUARTERS".equals(userType) ? headquartersId : partnerId)
                .category1TotalEmission(category1Detail.getFinalTotal())
                .category1Detail(category1Detail)
                .category2TotalEmission(category2Detail.getFinalTotal())
                .category2Detail(category2Detail)
                .category4TotalEmission(category4Detail.getFinalTotal())
                .category4Detail(category4Detail)
                .category5TotalEmission(category5Detail.getFinalTotal())
                .category5Detail(category5Detail)
                .build();
                
        long totalDuration = System.currentTimeMillis() - startTime;
        BigDecimal totalEmission = category1Detail.getFinalTotal()
                .add(category2Detail.getFinalTotal())
                .add(category4Detail.getFinalTotal())
                .add(category5Detail.getFinalTotal());
        log.debug("Scope3 특수집계 완료 - {}년 {}월: {} tCO2eq ({}ms)", 
                year, month, totalEmission, totalDuration);
                
        return response;
    }

    /**
     * 연간 특수 집계 실행 - 최적화된 통합 쿼리 방식 (12개월 통합 조회)
     * 기존의 12번 반복 호출을 통합하여 성능 대폭 향상
     */
    @Transactional(readOnly = true)
    public Scope3SpecialAggregationResponse getYearlySpecialAggregation(
            Integer year,
            Long headquartersId,
            String userType,
            Long partnerId,
            String treePath) {

        long startTime = System.currentTimeMillis();
        log.info("Scope3 연간 특수집계 시작 - {}년", year);

        try {
            // 연간 카테고리별 집계 실행 (통합 쿼리 사용)
            Scope3SpecialAggregationResponse.Category1Detail yearlyCategory1Detail = 
                calculateYearlyCategory1Optimized(year, headquartersId, userType, partnerId, treePath);
            Scope3SpecialAggregationResponse.Category2Detail yearlyCategory2Detail = 
                calculateYearlyCategory2Optimized(year, headquartersId, userType, partnerId, treePath);
            Scope3SpecialAggregationResponse.Category4Detail yearlyCategory4Detail = 
                calculateYearlyCategory4Optimized(year, headquartersId, userType, partnerId, treePath);
            Scope3SpecialAggregationResponse.Category5Detail yearlyCategory5Detail = 
                calculateYearlyCategory5Optimized(year, headquartersId, userType, partnerId, treePath);

            // 응답 생성 (12월로 설정하여 연별임을 표시)
            Long organizationId = "HEADQUARTERS".equals(userType) ? headquartersId : partnerId;
            
            Scope3SpecialAggregationResponse response = Scope3SpecialAggregationResponse.builder()
                    .reportingYear(year)
                    .reportingMonth(12) // 연별 조회임을 나타내는 더미 값
                    .userType(userType)
                    .organizationId(organizationId)
                    .category1TotalEmission(yearlyCategory1Detail.getFinalTotal())
                    .category1Detail(yearlyCategory1Detail)
                    .category2TotalEmission(yearlyCategory2Detail.getFinalTotal())
                    .category2Detail(yearlyCategory2Detail)
                    .category4TotalEmission(yearlyCategory4Detail.getFinalTotal())
                    .category4Detail(yearlyCategory4Detail)
                    .category5TotalEmission(yearlyCategory5Detail.getFinalTotal())
                    .category5Detail(yearlyCategory5Detail)
                    .build();
                    
            long totalDuration = System.currentTimeMillis() - startTime;
            BigDecimal totalEmission = yearlyCategory1Detail.getFinalTotal()
                    .add(yearlyCategory2Detail.getFinalTotal())
                    .add(yearlyCategory4Detail.getFinalTotal())
                    .add(yearlyCategory5Detail.getFinalTotal());
            
            log.info("Scope3 연별 특수집계 완료 - {}년: {} tCO2eq ({}ms)", 
                    year, totalEmission, totalDuration);
                    
            return response;

        } catch (Exception e) {
            log.error("Scope3 연간 특수집계 중 오류 발생 - {}년: {}", year, e.getMessage(), e);
            throw new RuntimeException("연간 특수집계 처리 중 오류가 발생했습니다", e);
        }
    }

    /**
     * 연간 Cat.1 집계 - 최적화된 통합 쿼리 방식 (12개월 통합)
     */
    private Scope3SpecialAggregationResponse.Category1Detail calculateYearlyCategory1Optimized(
            Integer year, Long headquartersId, String userType, Long partnerId, String treePath) {

        boolean isHeadquarters = "HEADQUARTERS".equals(userType);
        
        BigDecimal totalScope1Total = BigDecimal.ZERO;
        BigDecimal totalScope1MobileCombustion = BigDecimal.ZERO;
        BigDecimal totalScope1Factory = BigDecimal.ZERO;
        BigDecimal totalScope1WasteWater = BigDecimal.ZERO;
        BigDecimal totalScope2Total = BigDecimal.ZERO;
        BigDecimal totalScope2Factory = BigDecimal.ZERO;
        BigDecimal totalScope3Category1 = BigDecimal.ZERO;
        
        // 1월부터 12월까지 통합 쿼리로 조회하여 합산
        for (int month = 1; month <= 12; month++) {
            Map<String, Object> aggregationData = isHeadquarters
                    ? scopeEmissionService.getSpecialAggregationSummaryForHeadquarters(headquartersId, year, month)
                    : scopeEmissionService.getSpecialAggregationSummaryForPartner(headquartersId, partnerId, year, month);

            totalScope1Total = totalScope1Total.add(extractTotalEmission(aggregationData, "SCOPE1_TOTAL"));
            totalScope1MobileCombustion = totalScope1MobileCombustion.add(extractTotalEmission(aggregationData, "SCOPE1_MOBILE"));
            totalScope1Factory = totalScope1Factory.add(extractTotalEmission(aggregationData, "SCOPE1_FACTORY"));
            totalScope1WasteWater = totalScope1WasteWater.add(extractTotalEmission(aggregationData, "SCOPE1_WASTEWATER"));
            totalScope2Total = totalScope2Total.add(extractTotalEmission(aggregationData, "SCOPE2_TOTAL"));
            totalScope2Factory = totalScope2Factory.add(extractTotalEmission(aggregationData, "SCOPE2_FACTORY"));
            
            // Scope3 Category 1은 별도 조회
            BigDecimal monthlyScope3Cat1 = isHeadquarters
                    ? scopeEmissionRepository.sumScope3CategoryEmissionsByYearAndMonthForHeadquarters(headquartersId, 1, year, month)
                    : scopeEmissionRepository.sumScope3CategoryEmissionsByYearAndMonthForPartner(headquartersId, partnerId, 1, year, month);
            totalScope3Category1 = totalScope3Category1.add(monthlyScope3Cat1);
        }

        // 하위 조직들의 연간 Cat.1 finalTotal 합계 계산
        BigDecimal childOrganizationsCat1Total = calculateYearlyChildOrganizationsCat1Total(
                year, headquartersId, userType, partnerId, treePath);

        // 계산 수행
        BigDecimal scope1Remaining = totalScope1Total
                .subtract(totalScope1MobileCombustion)
                .subtract(totalScope1Factory)
                .subtract(totalScope1WasteWater);

        BigDecimal scope2Remaining = totalScope2Total.subtract(totalScope2Factory);

        // finalTotal 계산
        BigDecimal finalTotal;
        if (isHeadquarters) {
            finalTotal = scope1Remaining
                    .add(scope2Remaining)
                    .add(totalScope3Category1)
                    .add(childOrganizationsCat1Total);
        } else {
            finalTotal = childOrganizationsCat1Total;
        }

        return Scope3SpecialAggregationResponse.Category1Detail.builder()
                .scope1Total(totalScope1Total)
                .scope1MobileCombustion(totalScope1MobileCombustion)
                .scope1Factory(totalScope1Factory)
                .scope1WasteWater(totalScope1WasteWater)
                .scope1Remaining(scope1Remaining)
                .scope2Total(totalScope2Total)
                .scope2Factory(totalScope2Factory)
                .scope2Remaining(scope2Remaining)
                .scope3Category1(isHeadquarters ? totalScope3Category1.add(childOrganizationsCat1Total) : totalScope3Category1)
                .finalTotal(finalTotal)
                .build();
    }

    /**
     * 연간 Cat.2 집계 - 최적화된 통합 쿼리 방식
     */
    private Scope3SpecialAggregationResponse.Category2Detail calculateYearlyCategory2Optimized(
            Integer year, Long headquartersId, String userType, Long partnerId, String treePath) {

        boolean isHeadquarters = "HEADQUARTERS".equals(userType);
        
        BigDecimal totalScope1Factory = BigDecimal.ZERO;
        BigDecimal totalScope2Factory = BigDecimal.ZERO;
        BigDecimal totalScope3Category2 = BigDecimal.ZERO;
        
        // 1월부터 12월까지 통합 쿼리로 조회하여 합산
        for (int month = 1; month <= 12; month++) {
            Map<String, Object> aggregationData = isHeadquarters
                    ? scopeEmissionService.getSpecialAggregationSummaryForHeadquarters(headquartersId, year, month)
                    : scopeEmissionService.getSpecialAggregationSummaryForPartner(headquartersId, partnerId, year, month);

            totalScope1Factory = totalScope1Factory.add(extractTotalEmission(aggregationData, "SCOPE1_FACTORY"));
            totalScope2Factory = totalScope2Factory.add(extractTotalEmission(aggregationData, "SCOPE2_FACTORY"));
            
            // Scope3 Category 2는 별도 조회
            BigDecimal monthlyScope3Cat2 = isHeadquarters
                    ? scopeEmissionRepository.sumScope3CategoryEmissionsByYearAndMonthForHeadquarters(headquartersId, 2, year, month)
                    : scopeEmissionRepository.sumScope3CategoryEmissionsByYearAndMonthForPartner(headquartersId, partnerId, 2, year, month);
            totalScope3Category2 = totalScope3Category2.add(monthlyScope3Cat2);
        }

        // 하위 조직들의 연간 Cat.2 finalTotal 합계 계산
        BigDecimal childOrganizationsCat2Total = calculateYearlyChildOrganizationsCat2Total(
                year, headquartersId, userType, partnerId, treePath);

        // finalTotal 계산
        BigDecimal finalTotal;
        if (isHeadquarters) {
            finalTotal = totalScope1Factory.add(totalScope2Factory).add(totalScope3Category2).add(childOrganizationsCat2Total);
        } else {
            finalTotal = childOrganizationsCat2Total;
        }

        return Scope3SpecialAggregationResponse.Category2Detail.builder()
                .scope1Factory(totalScope1Factory)
                .scope2Factory(totalScope2Factory)
                .scope3Category2(isHeadquarters ? totalScope3Category2.add(childOrganizationsCat2Total) : totalScope3Category2)
                .finalTotal(finalTotal)
                .build();
    }

    /**
     * 연간 Cat.4 집계 - 최적화된 통합 쿼리 방식
     */
    private Scope3SpecialAggregationResponse.Category4Detail calculateYearlyCategory4Optimized(
            Integer year, Long headquartersId, String userType, Long partnerId, String treePath) {

        boolean isHeadquarters = "HEADQUARTERS".equals(userType);
        
        BigDecimal totalScope1MobileCombustion = BigDecimal.ZERO;
        BigDecimal totalScope3Category4 = BigDecimal.ZERO;
        
        // 1월부터 12월까지 통합 쿼리로 조회하여 합산
        for (int month = 1; month <= 12; month++) {
            Map<String, Object> aggregationData = isHeadquarters
                    ? scopeEmissionService.getSpecialAggregationSummaryForHeadquarters(headquartersId, year, month)
                    : scopeEmissionService.getSpecialAggregationSummaryForPartner(headquartersId, partnerId, year, month);

            totalScope1MobileCombustion = totalScope1MobileCombustion.add(extractTotalEmission(aggregationData, "SCOPE1_MOBILE"));
            
            // Scope3 Category 4는 별도 조회
            BigDecimal monthlyScope3Cat4 = isHeadquarters
                    ? scopeEmissionRepository.sumScope3CategoryEmissionsByYearAndMonthForHeadquarters(headquartersId, 4, year, month)
                    : scopeEmissionRepository.sumScope3CategoryEmissionsByYearAndMonthForPartner(headquartersId, partnerId, 4, year, month);
            totalScope3Category4 = totalScope3Category4.add(monthlyScope3Cat4);
        }

        // 하위 조직들의 연간 Cat.4 finalTotal 합계 계산
        BigDecimal childOrganizationsCat4Total = calculateYearlyChildOrganizationsCat4Total(
                year, headquartersId, userType, partnerId, treePath);

        // finalTotal 계산
        BigDecimal finalTotal;
        if (isHeadquarters) {
            finalTotal = totalScope1MobileCombustion.add(totalScope3Category4).add(childOrganizationsCat4Total);
        } else {
            finalTotal = childOrganizationsCat4Total;
        }

        return Scope3SpecialAggregationResponse.Category4Detail.builder()
                .scope1MobileCombustion(totalScope1MobileCombustion)
                .scope3Category4(isHeadquarters ? totalScope3Category4.add(childOrganizationsCat4Total) : totalScope3Category4)
                .finalTotal(finalTotal)
                .build();
    }

    /**
     * 연간 Cat.5 집계 - 최적화된 통합 쿼리 방식
     */
    private Scope3SpecialAggregationResponse.Category5Detail calculateYearlyCategory5Optimized(
            Integer year, Long headquartersId, String userType, Long partnerId, String treePath) {

        boolean isHeadquarters = "HEADQUARTERS".equals(userType);
        
        BigDecimal totalScope1WasteWater = BigDecimal.ZERO;
        BigDecimal totalScope3Category5 = BigDecimal.ZERO;
        
        // 1월부터 12월까지 통합 쿼리로 조회하여 합산
        for (int month = 1; month <= 12; month++) {
            Map<String, Object> aggregationData = isHeadquarters
                    ? scopeEmissionService.getSpecialAggregationSummaryForHeadquarters(headquartersId, year, month)
                    : scopeEmissionService.getSpecialAggregationSummaryForPartner(headquartersId, partnerId, year, month);

            totalScope1WasteWater = totalScope1WasteWater.add(extractTotalEmission(aggregationData, "SCOPE1_WASTEWATER"));
            
            // Scope3 Category 5는 별도 조회
            BigDecimal monthlyScope3Cat5 = isHeadquarters
                    ? scopeEmissionRepository.sumScope3CategoryEmissionsByYearAndMonthForHeadquarters(headquartersId, 5, year, month)
                    : scopeEmissionRepository.sumScope3CategoryEmissionsByYearAndMonthForPartner(headquartersId, partnerId, 5, year, month);
            totalScope3Category5 = totalScope3Category5.add(monthlyScope3Cat5);
        }

        // 하위 조직들의 연간 Cat.5 finalTotal 합계 계산
        BigDecimal childOrganizationsCat5Total = calculateYearlyChildOrganizationsCat5Total(
                year, headquartersId, userType, partnerId, treePath);

        // finalTotal 계산
        BigDecimal finalTotal;
        if (isHeadquarters) {
            finalTotal = totalScope1WasteWater.add(totalScope3Category5).add(childOrganizationsCat5Total);
        } else {
            finalTotal = childOrganizationsCat5Total;
        }

        return Scope3SpecialAggregationResponse.Category5Detail.builder()
                .scope1WasteWater(totalScope1WasteWater)
                .scope3Category5(isHeadquarters ? totalScope3Category5.add(childOrganizationsCat5Total) : totalScope3Category5)
                .finalTotal(finalTotal)
                .build();
    }

    // ========================================================================
    // 연간 하위 조직의 특수 집계 finalTotal 계산 헬퍼 메서드들
    // ========================================================================

    /**
     * 하위 조직들의 연간 Cat.1 finalTotal 합계 계산
     */
    private BigDecimal calculateYearlyChildOrganizationsCat1Total(
            Integer year, Long headquartersId, String userType, Long partnerId, String treePath) {
        
        BigDecimal yearlyTotal = BigDecimal.ZERO;
        
        // 1월부터 12월까지 각 월의 하위 조직 Cat.1 합계 누적
        for (int month = 1; month <= 12; month++) {
            BigDecimal monthlyTotal = calculateChildOrganizationsCat1Total(
                    year, month, headquartersId, userType, partnerId, treePath);
            yearlyTotal = yearlyTotal.add(monthlyTotal);
        }
        
        return yearlyTotal;
    }

    /**
     * 하위 조직들의 연간 Cat.2 finalTotal 합계 계산
     */
    private BigDecimal calculateYearlyChildOrganizationsCat2Total(
            Integer year, Long headquartersId, String userType, Long partnerId, String treePath) {
        
        BigDecimal yearlyTotal = BigDecimal.ZERO;
        
        for (int month = 1; month <= 12; month++) {
            BigDecimal monthlyTotal = calculateChildOrganizationsCat2Total(
                    year, month, headquartersId, userType, partnerId, treePath);
            yearlyTotal = yearlyTotal.add(monthlyTotal);
        }
        
        return yearlyTotal;
    }

    /**
     * 하위 조직들의 연간 Cat.4 finalTotal 합계 계산
     */
    private BigDecimal calculateYearlyChildOrganizationsCat4Total(
            Integer year, Long headquartersId, String userType, Long partnerId, String treePath) {
        
        BigDecimal yearlyTotal = BigDecimal.ZERO;
        
        for (int month = 1; month <= 12; month++) {
            BigDecimal monthlyTotal = calculateChildOrganizationsCat4Total(
                    year, month, headquartersId, userType, partnerId, treePath);
            yearlyTotal = yearlyTotal.add(monthlyTotal);
        }
        
        return yearlyTotal;
    }

    /**
     * 하위 조직들의 연간 Cat.5 finalTotal 합계 계산
     */
    private BigDecimal calculateYearlyChildOrganizationsCat5Total(
            Integer year, Long headquartersId, String userType, Long partnerId, String treePath) {
        
        BigDecimal yearlyTotal = BigDecimal.ZERO;
        
        for (int month = 1; month <= 12; month++) {
            BigDecimal monthlyTotal = calculateChildOrganizationsCat5Total(
                    year, month, headquartersId, userType, partnerId, treePath);
            yearlyTotal = yearlyTotal.add(monthlyTotal);
        }
        
        return yearlyTotal;
    }

    // ========================================================================
    // 최적화된 특수 집계 메서드들 (통합 쿼리 사용)
    // ========================================================================

    /**
     * Cat.1 집계 - 최적화된 통합 쿼리 방식
     */
    private Scope3SpecialAggregationResponse.Category1Detail calculateCategory1Optimized(
            Integer year, Integer month, Long headquartersId, String userType, Long partnerId, String treePath) {

        boolean isHeadquarters = "HEADQUARTERS".equals(userType);
        
        // 통합 쿼리를 사용하여 모든 특수 집계 데이터를 한 번에 조회
        Map<String, Object> aggregationData = isHeadquarters
                ? scopeEmissionService.getSpecialAggregationSummaryForHeadquarters(headquartersId, year, month)
                : scopeEmissionService.getSpecialAggregationSummaryForPartner(headquartersId, partnerId, year, month);

        // 통합 쿼리 결과에서 필요한 데이터 추출
        BigDecimal scope1Total = extractTotalEmission(aggregationData, "SCOPE1_TOTAL");
        BigDecimal scope1MobileCombustion = extractTotalEmission(aggregationData, "SCOPE1_MOBILE");
        BigDecimal scope1Factory = extractTotalEmission(aggregationData, "SCOPE1_FACTORY");
        BigDecimal scope1WasteWater = extractTotalEmission(aggregationData, "SCOPE1_WASTEWATER");
        BigDecimal scope2Total = extractTotalEmission(aggregationData, "SCOPE2_TOTAL");
        BigDecimal scope2Factory = extractTotalEmission(aggregationData, "SCOPE2_FACTORY");
        
        // Scope3 Category 1은 별도 조회 (통합 쿼리에 포함되지 않음)
        BigDecimal scope3Category1 = isHeadquarters
                ? scopeEmissionRepository.sumScope3CategoryEmissionsByYearAndMonthForHeadquarters(headquartersId, 1, year, month)
                : scopeEmissionRepository.sumScope3CategoryEmissionsByYearAndMonthForPartner(headquartersId, partnerId, 1, year, month);

        // 하위 조직들의 Cat.1 finalTotal 합계 계산
        BigDecimal childOrganizationsCat1Total = calculateChildOrganizationsCat1Total(
                year, month, headquartersId, userType, partnerId, treePath);

        // 계산 수행
        BigDecimal scope1Remaining = scope1Total
                .subtract(scope1MobileCombustion)
                .subtract(scope1Factory)
                .subtract(scope1WasteWater);

        BigDecimal scope2Remaining = scope2Total.subtract(scope2Factory);

        // finalTotal 계산
        BigDecimal finalTotal;
        if (isHeadquarters) {
            finalTotal = scope1Remaining
                    .add(scope2Remaining)
                    .add(scope3Category1)
                    .add(childOrganizationsCat1Total);
        } else {
            finalTotal = childOrganizationsCat1Total;
        }

        return Scope3SpecialAggregationResponse.Category1Detail.builder()
                .scope1Total(scope1Total)
                .scope1MobileCombustion(scope1MobileCombustion)
                .scope1Factory(scope1Factory)
                .scope1WasteWater(scope1WasteWater)
                .scope1Remaining(scope1Remaining)
                .scope2Total(scope2Total)
                .scope2Factory(scope2Factory)
                .scope2Remaining(scope2Remaining)
                .scope3Category1(isHeadquarters ? scope3Category1.add(childOrganizationsCat1Total) : scope3Category1)
                .finalTotal(finalTotal)
                .build();
    }

    /**
     * Cat.2 집계 - 최적화된 통합 쿼리 방식
     */
    private Scope3SpecialAggregationResponse.Category2Detail calculateCategory2Optimized(
            Integer year, Integer month, Long headquartersId, String userType, Long partnerId, String treePath) {

        boolean isHeadquarters = "HEADQUARTERS".equals(userType);
        
        // 통합 쿼리를 사용하여 모든 특수 집계 데이터를 한 번에 조회
        Map<String, Object> aggregationData = isHeadquarters
                ? scopeEmissionService.getSpecialAggregationSummaryForHeadquarters(headquartersId, year, month)
                : scopeEmissionService.getSpecialAggregationSummaryForPartner(headquartersId, partnerId, year, month);

        // 통합 쿼리 결과에서 필요한 데이터 추출
        BigDecimal scope1Factory = extractTotalEmission(aggregationData, "SCOPE1_FACTORY");
        BigDecimal scope2Factory = extractTotalEmission(aggregationData, "SCOPE2_FACTORY");
        
        // Scope3 Category 2는 별도 조회
        BigDecimal scope3Category2 = isHeadquarters
                ? scopeEmissionRepository.sumScope3CategoryEmissionsByYearAndMonthForHeadquarters(headquartersId, 2, year, month)
                : scopeEmissionRepository.sumScope3CategoryEmissionsByYearAndMonthForPartner(headquartersId, partnerId, 2, year, month);

        // 하위 조직들의 Cat.2 finalTotal 합계 계산
        BigDecimal childOrganizationsCat2Total = calculateChildOrganizationsCat2Total(
                year, month, headquartersId, userType, partnerId, treePath);

        // finalTotal 계산
        BigDecimal finalTotal;
        if (isHeadquarters) {
            finalTotal = scope1Factory.add(scope2Factory).add(scope3Category2).add(childOrganizationsCat2Total);
        } else {
            finalTotal = childOrganizationsCat2Total;
        }

        return Scope3SpecialAggregationResponse.Category2Detail.builder()
                .scope1Factory(scope1Factory)
                .scope2Factory(scope2Factory)
                .scope3Category2(isHeadquarters ? scope3Category2.add(childOrganizationsCat2Total) : scope3Category2)
                .finalTotal(finalTotal)
                .build();
    }

    /**
     * Cat.4 집계 - 최적화된 통합 쿼리 방식
     */
    private Scope3SpecialAggregationResponse.Category4Detail calculateCategory4Optimized(
            Integer year, Integer month, Long headquartersId, String userType, Long partnerId, String treePath) {

        boolean isHeadquarters = "HEADQUARTERS".equals(userType);
        
        // 통합 쿼리를 사용하여 모든 특수 집계 데이터를 한 번에 조회
        Map<String, Object> aggregationData = isHeadquarters
                ? scopeEmissionService.getSpecialAggregationSummaryForHeadquarters(headquartersId, year, month)
                : scopeEmissionService.getSpecialAggregationSummaryForPartner(headquartersId, partnerId, year, month);

        // 통합 쿼리 결과에서 필요한 데이터 추출
        BigDecimal scope1MobileCombustion = extractTotalEmission(aggregationData, "SCOPE1_MOBILE");
        
        // Scope3 Category 4는 별도 조회
        BigDecimal scope3Category4 = isHeadquarters
                ? scopeEmissionRepository.sumScope3CategoryEmissionsByYearAndMonthForHeadquarters(headquartersId, 4, year, month)
                : scopeEmissionRepository.sumScope3CategoryEmissionsByYearAndMonthForPartner(headquartersId, partnerId, 4, year, month);

        // 하위 조직들의 Cat.4 finalTotal 합계 계산
        BigDecimal childOrganizationsCat4Total = calculateChildOrganizationsCat4Total(
                year, month, headquartersId, userType, partnerId, treePath);

        // finalTotal 계산
        BigDecimal finalTotal;
        if (isHeadquarters) {
            finalTotal = scope1MobileCombustion.add(scope3Category4).add(childOrganizationsCat4Total);
        } else {
            finalTotal = childOrganizationsCat4Total;
        }

        return Scope3SpecialAggregationResponse.Category4Detail.builder()
                .scope1MobileCombustion(scope1MobileCombustion)
                .scope3Category4(isHeadquarters ? scope3Category4.add(childOrganizationsCat4Total) : scope3Category4)
                .finalTotal(finalTotal)
                .build();
    }

    /**
     * Cat.5 집계 - 최적화된 통합 쿼리 방식
     */
    private Scope3SpecialAggregationResponse.Category5Detail calculateCategory5Optimized(
            Integer year, Integer month, Long headquartersId, String userType, Long partnerId, String treePath) {

        boolean isHeadquarters = "HEADQUARTERS".equals(userType);
        
        // 통합 쿼리를 사용하여 모든 특수 집계 데이터를 한 번에 조회
        Map<String, Object> aggregationData = isHeadquarters
                ? scopeEmissionService.getSpecialAggregationSummaryForHeadquarters(headquartersId, year, month)
                : scopeEmissionService.getSpecialAggregationSummaryForPartner(headquartersId, partnerId, year, month);

        // 통합 쿼리 결과에서 필요한 데이터 추출
        BigDecimal scope1WasteWater = extractTotalEmission(aggregationData, "SCOPE1_WASTEWATER");
        
        // Scope3 Category 5는 별도 조회
        BigDecimal scope3Category5 = isHeadquarters
                ? scopeEmissionRepository.sumScope3CategoryEmissionsByYearAndMonthForHeadquarters(headquartersId, 5, year, month)
                : scopeEmissionRepository.sumScope3CategoryEmissionsByYearAndMonthForPartner(headquartersId, partnerId, 5, year, month);

        // 하위 조직들의 Cat.5 finalTotal 합계 계산
        BigDecimal childOrganizationsCat5Total = calculateChildOrganizationsCat5Total(
                year, month, headquartersId, userType, partnerId, treePath);

        // finalTotal 계산
        BigDecimal finalTotal;
        if (isHeadquarters) {
            finalTotal = scope1WasteWater.add(scope3Category5).add(childOrganizationsCat5Total);
        } else {
            finalTotal = childOrganizationsCat5Total;
        }

        return Scope3SpecialAggregationResponse.Category5Detail.builder()
                .scope1WasteWater(scope1WasteWater)
                .scope3Category5(isHeadquarters ? scope3Category5.add(childOrganizationsCat5Total) : scope3Category5)
                .finalTotal(finalTotal)
                .build();
    }

    /**
     * 통합 쿼리 결과에서 배출량 데이터 추출
     */
    @SuppressWarnings("unchecked")
    private BigDecimal extractTotalEmission(Map<String, Object> aggregationData, String key) {
        Map<String, Object> typeData = (Map<String, Object>) aggregationData.get(key);
        if (typeData != null && typeData.get("totalEmission") != null) {
            return (BigDecimal) typeData.get("totalEmission");
        }
        return BigDecimal.ZERO;
    }

    /**
     * Cat.1 집계: (Scope1 전체 - 이동연소 - 공장설비 - 폐수처리) + (Scope2 - 공장설비) + Scope3 Cat.1 + 하위 조직 Cat.1 finalTotal
     */
    private Scope3SpecialAggregationResponse.Category1Detail calculateCategory1(
            Integer year, Integer month, Long headquartersId, String userType, Long partnerId, String treePath) {

        boolean isHeadquarters = "HEADQUARTERS".equals(userType);

        // 본인 조직의 데이터 수집
        BigDecimal scope1Total, scope1MobileCombustion, scope1Factory, scope1WasteWater;
        BigDecimal scope2Total, scope2Factory, scope3Category1;

        if (isHeadquarters) {
            // 본사는 본사 직접 입력 데이터만
            scope1Total = scopeEmissionRepository.sumScope1TotalEmissionsByYearAndMonthForHeadquarters(headquartersId, year, month);
            scope1MobileCombustion = scopeEmissionRepository.sumScope1MobileCombustionEmissionsByYearAndMonthForHeadquarters(headquartersId, year, month);
            scope1Factory = scopeEmissionRepository.sumScope1FactoryEmissionsByYearAndMonthForHeadquarters(headquartersId, year, month);
            scope1WasteWater = scopeEmissionRepository.sumScope1WasteWaterTreatmentEmissionsByYearAndMonthForHeadquarters(headquartersId, year, month);
            scope2Total = scopeEmissionRepository.sumScope2TotalEmissionsByYearAndMonthForHeadquarters(headquartersId, year, month);
            scope2Factory = scopeEmissionRepository.sumScope2FactoryEmissionsByYearAndMonthForHeadquarters(headquartersId, year, month);
            scope3Category1 = scopeEmissionRepository.sumScope3CategoryEmissionsByYearAndMonthForHeadquarters(headquartersId, 1, year, month);
        } else {
            // 협력사는 본인 데이터만
            scope1Total = scopeEmissionRepository.sumScope1TotalEmissionsByYearAndMonthForPartner(headquartersId, partnerId, year, month);
            scope1MobileCombustion = scopeEmissionRepository.sumScope1MobileCombustionEmissionsByYearAndMonthForPartner(headquartersId, partnerId, year, month);
            scope1Factory = scopeEmissionRepository.sumScope1FactoryEmissionsByYearAndMonthForPartner(headquartersId, partnerId, year, month);
            scope1WasteWater = scopeEmissionRepository.sumScope1WasteWaterTreatmentEmissionsByYearAndMonthForPartner(headquartersId, partnerId, year, month);
            scope2Total = scopeEmissionRepository.sumScope2TotalEmissionsByYearAndMonthForPartner(headquartersId, partnerId, year, month);
            scope2Factory = scopeEmissionRepository.sumScope2FactoryEmissionsByYearAndMonthForPartner(headquartersId, partnerId, year, month);
            scope3Category1 = scopeEmissionRepository.sumScope3CategoryEmissionsByYearAndMonthForPartner(headquartersId, partnerId, 1, year, month);
        }

        // 하위 조직들의 Cat.1 finalTotal 합계 계산
        BigDecimal childOrganizationsCat1Total = calculateChildOrganizationsCat1Total(
                year, month, headquartersId, userType, partnerId, treePath);

        // 계산 수행
        BigDecimal scope1Remaining = scope1Total
                .subtract(scope1MobileCombustion)
                .subtract(scope1Factory)
                .subtract(scope1WasteWater);

        BigDecimal scope2Remaining = scope2Total.subtract(scope2Factory);

        // finalTotal 계산 - 협력사는 하위 조직 데이터만, 본사는 본인 + 하위 조직
        BigDecimal finalTotal;
        if (isHeadquarters) {
            // 본사: 본인 계산 결과 + 하위 조직들의 Cat.1 finalTotal
            finalTotal = scope1Remaining
                    .add(scope2Remaining)
                    .add(scope3Category1)
                    .add(childOrganizationsCat1Total);
        } else {
            // 협력사: 하위 조직들의 Cat.1 finalTotal만 (본인 데이터는 업스트림용)
            finalTotal = childOrganizationsCat1Total;
        }

        return Scope3SpecialAggregationResponse.Category1Detail.builder()
                .scope1Total(scope1Total)
                .scope1MobileCombustion(scope1MobileCombustion)
                .scope1Factory(scope1Factory)
                .scope1WasteWater(scope1WasteWater)
                .scope1Remaining(scope1Remaining)
                .scope2Total(scope2Total)
                .scope2Factory(scope2Factory)
                .scope2Remaining(scope2Remaining)
                .scope3Category1(isHeadquarters ? scope3Category1.add(childOrganizationsCat1Total) : scope3Category1) // 협력사는 본인 데이터만
                .finalTotal(finalTotal)
                .build();
    }

    /**
     * Cat.2 집계: Scope1 공장설비 + Scope2 공장설비 + Scope3 Cat.2 + 하위 조직 Cat.2 finalTotal
     */
    private Scope3SpecialAggregationResponse.Category2Detail calculateCategory2(
            Integer year, Integer month, Long headquartersId, String userType, Long partnerId, String treePath) {

        boolean isHeadquarters = "HEADQUARTERS".equals(userType);

        BigDecimal scope1Factory = isHeadquarters
                ? scopeEmissionRepository.sumScope1FactoryEmissionsByYearAndMonthForHeadquarters(headquartersId, year, month)
                : scopeEmissionRepository.sumScope1FactoryEmissionsByYearAndMonthForPartner(headquartersId, partnerId, year, month);

        BigDecimal scope2Factory = isHeadquarters
                ? scopeEmissionRepository.sumScope2FactoryEmissionsByYearAndMonthForHeadquarters(headquartersId, year, month)
                : scopeEmissionRepository.sumScope2FactoryEmissionsByYearAndMonthForPartner(headquartersId, partnerId, year, month);

        BigDecimal scope3Category2 = isHeadquarters
                ? scopeEmissionRepository.sumScope3CategoryEmissionsByYearAndMonthForHeadquarters(headquartersId, 2, year, month)
                : scopeEmissionRepository.sumScope3CategoryEmissionsByYearAndMonthForPartner(headquartersId, partnerId, 2, year, month);

        // 하위 조직들의 Cat.2 finalTotal 합계 계산
        BigDecimal childOrganizationsCat2Total = calculateChildOrganizationsCat2Total(
                year, month, headquartersId, userType, partnerId, treePath);

        // finalTotal 계산 - 협력사는 하위 조직 데이터만, 본사는 본인 + 하위 조직
        BigDecimal finalTotal;
        if (isHeadquarters) {
            // 본사: 본인 계산 결과 + 하위 조직들의 Cat.2 finalTotal
            finalTotal = scope1Factory.add(scope2Factory).add(scope3Category2).add(childOrganizationsCat2Total);
        } else {
            // 협력사: 하위 조직들의 Cat.2 finalTotal만 (본인 데이터는 업스트림용)
            finalTotal = childOrganizationsCat2Total;
        }

        return Scope3SpecialAggregationResponse.Category2Detail.builder()
                .scope1Factory(scope1Factory)
                .scope2Factory(scope2Factory)
                .scope3Category2(isHeadquarters ? scope3Category2.add(childOrganizationsCat2Total) : scope3Category2) // 협력사는 본인 데이터만
                .finalTotal(finalTotal)
                .build();
    }

    /**
     * Cat.4 집계: Scope1 이동연소 + Scope3 Cat.4 + 하위 조직 Cat.4 finalTotal
     */
    private Scope3SpecialAggregationResponse.Category4Detail calculateCategory4(
            Integer year, Integer month, Long headquartersId, String userType, Long partnerId, String treePath) {

        boolean isHeadquarters = "HEADQUARTERS".equals(userType);

        BigDecimal scope1MobileCombustion = isHeadquarters
                ? scopeEmissionRepository.sumScope1MobileCombustionEmissionsByYearAndMonthForHeadquarters(headquartersId, year, month)
                : scopeEmissionRepository.sumScope1MobileCombustionEmissionsByYearAndMonthForPartner(headquartersId, partnerId, year, month);

        BigDecimal scope3Category4 = isHeadquarters
                ? scopeEmissionRepository.sumScope3CategoryEmissionsByYearAndMonthForHeadquarters(headquartersId, 4, year, month)
                : scopeEmissionRepository.sumScope3CategoryEmissionsByYearAndMonthForPartner(headquartersId, partnerId, 4, year, month);

        // 하위 조직들의 Cat.4 finalTotal 합계 계산
        BigDecimal childOrganizationsCat4Total = calculateChildOrganizationsCat4Total(
                year, month, headquartersId, userType, partnerId, treePath);

        // finalTotal 계산 - 협력사는 하위 조직 데이터만, 본사는 본인 + 하위 조직
        BigDecimal finalTotal;
        if (isHeadquarters) {
            // 본사: 본인 계산 결과 + 하위 조직들의 Cat.4 finalTotal
            finalTotal = scope1MobileCombustion.add(scope3Category4).add(childOrganizationsCat4Total);
        } else {
            // 협력사: 하위 조직들의 Cat.4 finalTotal만 (본인 데이터는 업스트림용)
            finalTotal = childOrganizationsCat4Total;
        }

        return Scope3SpecialAggregationResponse.Category4Detail.builder()
                .scope1MobileCombustion(scope1MobileCombustion)
                .scope3Category4(isHeadquarters ? scope3Category4.add(childOrganizationsCat4Total) : scope3Category4) // 협력사는 본인 데이터만
                .finalTotal(finalTotal)
                .build();
    }

    /**
     * Cat.5 집계: Scope1 폐수처리 + Scope3 Cat.5 + 하위 조직 Cat.5 finalTotal
     */
    private Scope3SpecialAggregationResponse.Category5Detail calculateCategory5(
            Integer year, Integer month, Long headquartersId, String userType, Long partnerId, String treePath) {

        boolean isHeadquarters = "HEADQUARTERS".equals(userType);

        BigDecimal scope1WasteWater = isHeadquarters
                ? scopeEmissionRepository.sumScope1WasteWaterTreatmentEmissionsByYearAndMonthForHeadquarters(headquartersId, year, month)
                : scopeEmissionRepository.sumScope1WasteWaterTreatmentEmissionsByYearAndMonthForPartner(headquartersId, partnerId, year, month);

        BigDecimal scope3Category5 = isHeadquarters
                ? scopeEmissionRepository.sumScope3CategoryEmissionsByYearAndMonthForHeadquarters(headquartersId, 5, year, month)
                : scopeEmissionRepository.sumScope3CategoryEmissionsByYearAndMonthForPartner(headquartersId, partnerId, 5, year, month);

        // 하위 조직들의 Cat.5 finalTotal 합계 계산
        BigDecimal childOrganizationsCat5Total = calculateChildOrganizationsCat5Total(
                year, month, headquartersId, userType, partnerId, treePath);

        // finalTotal 계산 - 협력사는 하위 조직 데이터만, 본사는 본인 + 하위 조직
        BigDecimal finalTotal;
        if (isHeadquarters) {
            // 본사: 본인 계산 결과 + 하위 조직들의 Cat.5 finalTotal
            finalTotal = scope1WasteWater.add(scope3Category5).add(childOrganizationsCat5Total);
        } else {
            // 협력사: 하위 조직들의 Cat.5 finalTotal만 (본인 데이터는 업스트림용)
            finalTotal = childOrganizationsCat5Total;
        }

        return Scope3SpecialAggregationResponse.Category5Detail.builder()
                .scope1WasteWater(scope1WasteWater)
                .scope3Category5(isHeadquarters ? scope3Category5.add(childOrganizationsCat5Total) : scope3Category5) // 협력사는 본인 데이터만
                .finalTotal(finalTotal)
                .build();
    }

    // ========================================================================
    // 하위 조직의 특수 집계 finalTotal 계산 헬퍼 메서드들
    // ========================================================================

    /**
     * 하위 조직들의 Cat.1 finalTotal 합계 계산
     */
    private BigDecimal calculateChildOrganizationsCat1Total(
            Integer year, Integer month, Long headquartersId, String userType, Long partnerId, String treePath) {
        
        if ("HEADQUARTERS".equals(userType)) {
            // 본사인 경우 모든 협력사의 Cat.1 finalTotal 합계 반환
            List<Long> allChildPartnerIds = scopeEmissionRepository.findAllChildPartnerIds(headquartersId, "/");
            return calculateCat1TotalForPartnerList(allChildPartnerIds, year, month, headquartersId);
        } else if (treePath != null) {
            // 협력사인 경우 하위 조직들의 Cat.1 finalTotal 합계 반환
            List<Long> childPartnerIds = scopeEmissionRepository.findAllChildPartnerIds(headquartersId, treePath);
            return calculateCat1TotalForPartnerList(childPartnerIds, year, month, headquartersId);
        }
        
        return BigDecimal.ZERO;
    }

    /**
     * 하위 조직들의 Cat.2 finalTotal 합계 계산
     */
    private BigDecimal calculateChildOrganizationsCat2Total(
            Integer year, Integer month, Long headquartersId, String userType, Long partnerId, String treePath) {
        
        if ("HEADQUARTERS".equals(userType)) {
            List<Long> allChildPartnerIds = scopeEmissionRepository.findAllChildPartnerIds(headquartersId, "/");
            return calculateCat2TotalForPartnerList(allChildPartnerIds, year, month, headquartersId);
        } else if (treePath != null) {
            List<Long> childPartnerIds = scopeEmissionRepository.findAllChildPartnerIds(headquartersId, treePath);
            return calculateCat2TotalForPartnerList(childPartnerIds, year, month, headquartersId);
        }
        
        return BigDecimal.ZERO;
    }

    /**
     * 하위 조직들의 Cat.4 finalTotal 합계 계산
     */
    private BigDecimal calculateChildOrganizationsCat4Total(
            Integer year, Integer month, Long headquartersId, String userType, Long partnerId, String treePath) {
        
        if ("HEADQUARTERS".equals(userType)) {
            List<Long> allChildPartnerIds = scopeEmissionRepository.findAllChildPartnerIds(headquartersId, "/");
            return calculateCat4TotalForPartnerList(allChildPartnerIds, year, month, headquartersId);
        } else if (treePath != null) {
            List<Long> childPartnerIds = scopeEmissionRepository.findAllChildPartnerIds(headquartersId, treePath);
            return calculateCat4TotalForPartnerList(childPartnerIds, year, month, headquartersId);
        }
        
        return BigDecimal.ZERO;
    }

    /**
     * 하위 조직들의 Cat.5 finalTotal 합계 계산
     */
    private BigDecimal calculateChildOrganizationsCat5Total(
            Integer year, Integer month, Long headquartersId, String userType, Long partnerId, String treePath) {
        
        if ("HEADQUARTERS".equals(userType)) {
            List<Long> allChildPartnerIds = scopeEmissionRepository.findAllChildPartnerIds(headquartersId, "/");
            return calculateCat5TotalForPartnerList(allChildPartnerIds, year, month, headquartersId);
        } else if (treePath != null) {
            List<Long> childPartnerIds = scopeEmissionRepository.findAllChildPartnerIds(headquartersId, treePath);
            return calculateCat5TotalForPartnerList(childPartnerIds, year, month, headquartersId);
        }
        
        return BigDecimal.ZERO;
    }

    // ========================================================================
    // 개별 협력사들의 특수 집계 계산 헬퍼 메서드들
    // ========================================================================

    /**
     * 협력사 목록의 Cat.1 finalTotal 합계 계산
     */
    private BigDecimal calculateCat1TotalForPartnerList(List<Long> partnerIds, Integer year, Integer month, Long headquartersId) {
        return partnerIds.stream()
                .map(pId -> calculateSinglePartnerCat1Total(pId, year, month, headquartersId))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 협력사 목록의 Cat.2 finalTotal 합계 계산
     */
    private BigDecimal calculateCat2TotalForPartnerList(List<Long> partnerIds, Integer year, Integer month, Long headquartersId) {
        return partnerIds.stream()
                .map(pId -> calculateSinglePartnerCat2Total(pId, year, month, headquartersId))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 협력사 목록의 Cat.4 finalTotal 합계 계산
     */
    private BigDecimal calculateCat4TotalForPartnerList(List<Long> partnerIds, Integer year, Integer month, Long headquartersId) {
        return partnerIds.stream()
                .map(pId -> calculateSinglePartnerCat4Total(pId, year, month, headquartersId))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 협력사 목록의 Cat.5 finalTotal 합계 계산
     */
    private BigDecimal calculateCat5TotalForPartnerList(List<Long> partnerIds, Integer year, Integer month, Long headquartersId) {
        return partnerIds.stream()
                .map(pId -> calculateSinglePartnerCat5Total(pId, year, month, headquartersId))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 단일 협력사의 Cat.1 finalTotal 계산
     */
    private BigDecimal calculateSinglePartnerCat1Total(Long partnerId, Integer year, Integer month, Long headquartersId) {
        BigDecimal scope1Total = scopeEmissionRepository.sumScope1TotalEmissionsByYearAndMonthForPartner(headquartersId, partnerId, year, month);
        BigDecimal scope1MobileCombustion = scopeEmissionRepository.sumScope1MobileCombustionEmissionsByYearAndMonthForPartner(headquartersId, partnerId, year, month);
        BigDecimal scope1Factory = scopeEmissionRepository.sumScope1FactoryEmissionsByYearAndMonthForPartner(headquartersId, partnerId, year, month);
        BigDecimal scope1WasteWater = scopeEmissionRepository.sumScope1WasteWaterTreatmentEmissionsByYearAndMonthForPartner(headquartersId, partnerId, year, month);
        BigDecimal scope2Total = scopeEmissionRepository.sumScope2TotalEmissionsByYearAndMonthForPartner(headquartersId, partnerId, year, month);
        BigDecimal scope2Factory = scopeEmissionRepository.sumScope2FactoryEmissionsByYearAndMonthForPartner(headquartersId, partnerId, year, month);
        BigDecimal scope3Category1 = scopeEmissionRepository.sumScope3CategoryEmissionsByYearAndMonthForPartner(headquartersId, partnerId, 1, year, month);

        BigDecimal scope1Remaining = scope1Total.subtract(scope1MobileCombustion).subtract(scope1Factory).subtract(scope1WasteWater);
        BigDecimal scope2Remaining = scope2Total.subtract(scope2Factory);
        
        return scope1Remaining.add(scope2Remaining).add(scope3Category1);
    }

    /**
     * 단일 협력사의 Cat.2 finalTotal 계산
     */
    private BigDecimal calculateSinglePartnerCat2Total(Long partnerId, Integer year, Integer month, Long headquartersId) {
        BigDecimal scope1Factory = scopeEmissionRepository.sumScope1FactoryEmissionsByYearAndMonthForPartner(headquartersId, partnerId, year, month);
        BigDecimal scope2Factory = scopeEmissionRepository.sumScope2FactoryEmissionsByYearAndMonthForPartner(headquartersId, partnerId, year, month);
        BigDecimal scope3Category2 = scopeEmissionRepository.sumScope3CategoryEmissionsByYearAndMonthForPartner(headquartersId, partnerId, 2, year, month);
        
        return scope1Factory.add(scope2Factory).add(scope3Category2);
    }

    /**
     * 단일 협력사의 Cat.4 finalTotal 계산
     */
    private BigDecimal calculateSinglePartnerCat4Total(Long partnerId, Integer year, Integer month, Long headquartersId) {
        BigDecimal scope1MobileCombustion = scopeEmissionRepository.sumScope1MobileCombustionEmissionsByYearAndMonthForPartner(headquartersId, partnerId, year, month);
        BigDecimal scope3Category4 = scopeEmissionRepository.sumScope3CategoryEmissionsByYearAndMonthForPartner(headquartersId, partnerId, 4, year, month);
        
        return scope1MobileCombustion.add(scope3Category4);
    }

    /**
     * 단일 협력사의 Cat.5 finalTotal 계산
     */
    private BigDecimal calculateSinglePartnerCat5Total(Long partnerId, Integer year, Integer month, Long headquartersId) {
        BigDecimal scope1WasteWater = scopeEmissionRepository.sumScope1WasteWaterTreatmentEmissionsByYearAndMonthForPartner(headquartersId, partnerId, year, month);
        BigDecimal scope3Category5 = scopeEmissionRepository.sumScope3CategoryEmissionsByYearAndMonthForPartner(headquartersId, partnerId, 5, year, month);
        
        return scope1WasteWater.add(scope3Category5);
    }
}