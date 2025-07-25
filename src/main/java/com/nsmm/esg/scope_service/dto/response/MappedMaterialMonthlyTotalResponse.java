package com.nsmm.esg.scope_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 맵핑된 자재코드 월별 총합 응답 DTO
 * 
 * 기능:
 * - 지정된 연도의 월별 Scope 1 + Scope 2 총합 제공
 * - 자재별 상세 정보 포함 (자재명, 내부자재코드, 상위자재코드, Scope별 배출량)
 * - 현재년도는 현재월까지, 다른 년도는 12월까지 표시
 * - 차트 및 테이블 데이터 표시용
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MappedMaterialMonthlyTotalResponse {

    // 조직 정보
    private String userType; // HEADQUARTERS | PARTNER
    private Long organizationId; // 본사ID 또는 협력사ID
    private Integer reportingYear; // 보고 연도
    
    // 월별 총합 리스트 (1월부터 현재월/12월까지)
    private List<MonthlyTotal> monthlyTotals;
    
    // 자재별 상세 정보 리스트
    private List<MaterialDetail> materialDetails;
    
    /**
     * 개별 월별 총합 항목
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyTotal {
        
        private Integer month; // 월 (1-12)
        private BigDecimal totalEmission; // 해당 월의 Scope 1 + Scope 2 총합
        private Long dataCount; // 해당 월의 데이터 건수
        
        /**
         * 빈 월별 총합 생성 (데이터 없는 월용)
         */
        public static MonthlyTotal createEmptyMonth(Integer month) {
            return MonthlyTotal.builder()
                    .month(month)
                    .totalEmission(BigDecimal.ZERO)
                    .dataCount(0L)
                    .build();
        }
    }
    
    /**
     * 자재별 상세 정보 항목
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MaterialDetail {
        
        private String materialName; // 자재명
        private String internalMaterialCode; // 내부 자재코드
        private String upstreamMaterialCode; // 상위 자재코드
        private BigDecimal scope1Emission; // Scope 1 배출량 (tCO₂eq)
        private BigDecimal scope2Emission; // Scope 2 배출량 (tCO₂eq)
        private BigDecimal totalEmission; // 통합 배출량 (Scope 1 + Scope 2)
        
        /**
         * 쿼리 결과를 MaterialDetail로 변환
         * Object[] 순서: materialName, internalMaterialCode, upstreamMaterialCode, scope1Emission, scope2Emission
         */
        public static MaterialDetail fromQueryResult(Object[] result) {
            BigDecimal scope1 = result[3] != null ? (BigDecimal) result[3] : BigDecimal.ZERO;
            BigDecimal scope2 = result[4] != null ? (BigDecimal) result[4] : BigDecimal.ZERO;
            
            return MaterialDetail.builder()
                    .materialName(result[0] != null ? (String) result[0] : "")
                    .internalMaterialCode(result[1] != null ? (String) result[1] : "")
                    .upstreamMaterialCode(result[2] != null ? (String) result[2] : "")
                    .scope1Emission(scope1)
                    .scope2Emission(scope2)
                    .totalEmission(scope1.add(scope2))
                    .build();
        }
    }
    
    /**
     * 본사용 월별 총합 응답 생성
     */
    public static MappedMaterialMonthlyTotalResponse createHeadquartersResponse(
            Long headquartersId,
            Integer year,
            List<MonthlyTotal> monthlyTotals,
            List<MaterialDetail> materialDetails) {
        
        return MappedMaterialMonthlyTotalResponse.builder()
                .userType("HEADQUARTERS")
                .organizationId(headquartersId)
                .reportingYear(year)
                .monthlyTotals(monthlyTotals)
                .materialDetails(materialDetails)
                .build();
    }
    
    /**
     * 협력사용 월별 총합 응답 생성
     */
    public static MappedMaterialMonthlyTotalResponse createPartnerResponse(
            Long partnerId,
            Integer year,
            List<MonthlyTotal> monthlyTotals,
            List<MaterialDetail> materialDetails) {
        
        return MappedMaterialMonthlyTotalResponse.builder()
                .userType("PARTNER")
                .organizationId(partnerId)
                .reportingYear(year)
                .monthlyTotals(monthlyTotals)
                .materialDetails(materialDetails)
                .build();
    }
    

    

}