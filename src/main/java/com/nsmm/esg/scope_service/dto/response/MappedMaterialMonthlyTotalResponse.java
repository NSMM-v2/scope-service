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
 * - 지정된 연도의 1월부터 12월까지 각 월별 Scope 1 + Scope 2 총합 제공
 * - 자재별 상세 정보 없이 월별 총합만 반환
 * - 차트 데이터 표시용
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
    
    // 월별 총합 리스트 (1월부터 12월까지)
    private List<MonthlyTotal> monthlyTotals;
    
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
     * 본사용 월별 총합 응답 생성
     */
    public static MappedMaterialMonthlyTotalResponse createHeadquartersResponse(
            Long headquartersId,
            Integer year,
            List<MonthlyTotal> monthlyTotals) {
        
        // 연간 총합 계산
        BigDecimal yearlyTotal = monthlyTotals.stream()
                .map(MonthlyTotal::getTotalEmission)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        return MappedMaterialMonthlyTotalResponse.builder()
                .userType("HEADQUARTERS")
                .organizationId(headquartersId)
                .reportingYear(year)
                .monthlyTotals(monthlyTotals)
                .build();
    }
    
    /**
     * 협력사용 월별 총합 응답 생성
     */
    public static MappedMaterialMonthlyTotalResponse createPartnerResponse(
            Long partnerId,
            Integer year,
            List<MonthlyTotal> monthlyTotals) {
        
        return MappedMaterialMonthlyTotalResponse.builder()
                .userType("PARTNER")
                .organizationId(partnerId)
                .reportingYear(year)
                .monthlyTotals(monthlyTotals)
                .build();
    }
    

    

}