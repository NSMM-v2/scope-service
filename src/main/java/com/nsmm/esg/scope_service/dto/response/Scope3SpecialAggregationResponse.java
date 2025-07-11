package com.nsmm.esg.scope_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Scope 3 특수 집계 응답 DTO
 * 
 * Scope 3 Cat.1, 2, 4, 5에 대한 특수 집계 규칙 적용 결과:
 * - Cat.1: (Scope1 전체 - 이동연소 - 공장설비 - 폐수처리) + (Scope2 - 공장설비) + Scope3 Cat.1
 * - Cat.2: Scope1 공장설비 + Scope2 공장설비 + Scope3 Cat.2
 * - Cat.4: Scope1 이동연소 + Scope3 Cat.4
 * - Cat.5: Scope1 폐수처리 + Scope3 Cat.5
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Scope3SpecialAggregationResponse {

    // 기본 정보
    private Integer reportingYear; // 보고 연도
    private Integer reportingMonth; // 보고 월
    private String userType; // 사용자 타입 (HEADQUARTERS/PARTNER)
    private Long organizationId; // 조직 ID (본사 ID 또는 협력사 ID)

    // Cat.1: 구매한 상품 및 서비스 (특수 집계)
    private BigDecimal category1TotalEmission; // Cat.1 총 배출량
    private Category1Detail category1Detail; // Cat.1 상세 계산 내역

    // Cat.2: 자본재 (특수 집계)
    private BigDecimal category2TotalEmission; // Cat.2 총 배출량
    private Category2Detail category2Detail; // Cat.2 상세 계산 내역

    // Cat.4: 업스트림 운송 및 유통 (특수 집계)
    private BigDecimal category4TotalEmission; // Cat.4 총 배출량
    private Category4Detail category4Detail; // Cat.4 상세 계산 내역

    // Cat.5: 폐기물 처리 (특수 집계)
    private BigDecimal category5TotalEmission; // Cat.5 총 배출량
    private Category5Detail category5Detail; // Cat.5 상세 계산 내역

    /**
     * Cat.1 상세 계산 내역
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Category1Detail {
        private BigDecimal scope1Total; // Scope1 전체
        private BigDecimal scope1MobileCombustion; // Scope1 이동연소 (제외)
        private BigDecimal scope1Factory; // Scope1 공장설비 (제외)
        private BigDecimal scope1WasteWater; // Scope1 폐수처리 (제외)
        private BigDecimal scope1Remaining; // Scope1 잔여 (전체 - 제외 항목들)
        
        private BigDecimal scope2Total; // Scope2 전체
        private BigDecimal scope2Factory; // Scope2 공장설비 (제외)
        private BigDecimal scope2Remaining; // Scope2 잔여 (전체 - 공장설비)
        
        private BigDecimal scope3Category1; // Scope3 Cat.1
        private BigDecimal finalTotal; // 최종 총계
    }

    /**
     * Cat.2 상세 계산 내역
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Category2Detail {
        private BigDecimal scope1Factory; // Scope1 공장설비
        private BigDecimal scope2Factory; // Scope2 공장설비
        private BigDecimal scope3Category2; // Scope3 Cat.2
        private BigDecimal finalTotal; // 최종 총계
    }

    /**
     * Cat.4 상세 계산 내역
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Category4Detail {
        private BigDecimal scope1MobileCombustion; // Scope1 이동연소
        private BigDecimal scope3Category4; // Scope3 Cat.4
        private BigDecimal finalTotal; // 최종 총계
    }

    /**
     * Cat.5 상세 계산 내역
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Category5Detail {
        private BigDecimal scope1WasteWater; // Scope1 폐수처리
        private BigDecimal scope3Category5; // Scope3 Cat.5
        private BigDecimal finalTotal; // 최종 총계
    }
}