package com.nsmm.esg.scope_service.dto.response;

import java.math.BigDecimal;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Scope 3 통합 배출량 응답 DTO
 * 특수집계배출량 + 일반 Scope3 카테고리별 배출량의 합계를 제공
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Scope3CombinedEmissionResponse {

    // 기본 정보
    private Integer reportingYear;
    private Integer reportingMonth; // 월별 조회시에만 값 존재
    private String userType;
    private Long organizationId;
    
    // 특수집계 배출량 (Cat.1, 2, 4, 5 finalTotal 합계)
    private BigDecimal specialAggregationTotal;
    private Scope3SpecialAggregationResponse specialAggregationDetail;
    
    // 일반 Scope3 카테고리별 배출량 합계
    private BigDecimal regularCategoryTotal;
    private List<CategoryYearlyEmission> yearlyCategories; // 연별 조회시
    private List<CategoryMonthlyEmission> monthlyCategories; // 월별 조회시
    
    // 최종 통합 배출량 (특수집계 + 일반 카테고리)
    private BigDecimal totalScope3Emission;
    
    // 데이터 건수
    private Long totalDataCount;
    
    /**
     * 월별 통합 배출량 응답 생성
     */
    public static Scope3CombinedEmissionResponse createMonthlyResponse(
            Integer year,
            Integer month,
            String userType,
            Long organizationId,
            Scope3SpecialAggregationResponse specialAggregation,
            List<CategoryMonthlyEmission> monthlyCategories) {
        
        // 특수집계 총합 계산
        BigDecimal specialTotal = calculateSpecialAggregationTotal(specialAggregation);
        
        // 일반 카테고리 총합 계산 (특수집계 카테고리 1,2,4,5 제외)
        BigDecimal regularTotal = monthlyCategories.stream()
                .filter(category -> !isSpecialAggregationCategory(category.getCategoryNumber()))
                .map(CategoryMonthlyEmission::getTotalEmission)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // 데이터 건수 합계
        Long totalDataCount = monthlyCategories.stream()
                .map(CategoryMonthlyEmission::getDataCount)
                .reduce(0L, Long::sum);
        
        return Scope3CombinedEmissionResponse.builder()
                .reportingYear(year)
                .reportingMonth(month)
                .userType(userType)
                .organizationId(organizationId)
                .specialAggregationTotal(specialTotal)
                .specialAggregationDetail(specialAggregation)
                .regularCategoryTotal(regularTotal)
                .monthlyCategories(monthlyCategories)
                .totalScope3Emission(specialTotal.add(regularTotal))
                .totalDataCount(totalDataCount)
                .build();
    }
    
    /**
     * 연별 통합 배출량 응답 생성
     */
    public static Scope3CombinedEmissionResponse createYearlyResponse(
            Integer year,
            String userType,
            Long organizationId,
            Scope3SpecialAggregationResponse specialAggregation,
            List<CategoryYearlyEmission> yearlyCategories) {
        
        // 특수집계 총합 계산
        BigDecimal specialTotal = calculateSpecialAggregationTotal(specialAggregation);
        
        // 일반 카테고리 총합 계산 (특수집계 카테고리 1,2,4,5 제외)
        BigDecimal regularTotal = yearlyCategories.stream()
                .filter(category -> !isSpecialAggregationCategory(category.getCategoryNumber()))
                .map(CategoryYearlyEmission::getTotalEmission)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // 데이터 건수 합계
        Long totalDataCount = yearlyCategories.stream()
                .map(CategoryYearlyEmission::getDataCount)
                .reduce(0L, Long::sum);
        
        return Scope3CombinedEmissionResponse.builder()
                .reportingYear(year)
                .userType(userType)
                .organizationId(organizationId)
                .specialAggregationTotal(specialTotal)
                .specialAggregationDetail(specialAggregation)
                .regularCategoryTotal(regularTotal)
                .yearlyCategories(yearlyCategories)
                .totalScope3Emission(specialTotal.add(regularTotal))
                .totalDataCount(totalDataCount)
                .build();
    }
    
    /**
     * 특수집계 응답에서 총합 계산
     */
    private static BigDecimal calculateSpecialAggregationTotal(Scope3SpecialAggregationResponse specialAggregation) {
        if (specialAggregation == null) {
            return BigDecimal.ZERO;
        }
        
        return specialAggregation.getCategory1TotalEmission()
                .add(specialAggregation.getCategory2TotalEmission())
                .add(specialAggregation.getCategory4TotalEmission())
                .add(specialAggregation.getCategory5TotalEmission());
    }
    
    /**
     * 특수집계 카테고리인지 확인 (Cat.1, 2, 4, 5)
     */
    private static boolean isSpecialAggregationCategory(Integer categoryNumber) {
        return categoryNumber == 1 || categoryNumber == 2 || categoryNumber == 4 || categoryNumber == 5;
    }
}