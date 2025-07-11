package com.nsmm.esg.scope_service.dto.response;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 월별 배출량 집계 응답 DTO
 * 
 * 협력사별 월별 Scope 1,2,3 배출량 총계 정보를 담는 DTO
 * 차트 및 테이블 데이터 표시에 사용됩니다.
 *
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "월별 배출량 집계 응답")
public class MonthlyEmissionSummary {

    @Schema(description = "연도", example = "2024")
    private Integer year;

    @Schema(description = "월", example = "12")
    private Integer month;

    @Schema(description = "Scope 1 총 배출량", example = "1234.56")
    private BigDecimal scope1Total;

    @Schema(description = "Scope 2 총 배출량", example = "2345.67")
    private BigDecimal scope2Total;

    @Schema(description = "Scope 3 총 배출량", example = "3456.78")
    private BigDecimal scope3Total;

    @Schema(description = "전체 총 배출량", example = "7037.01")
    private BigDecimal totalEmission;

    @Schema(description = "해당 월의 데이터 건수", example = "25")
    private Long dataCount;

    /**
     * 전체 총 배출량 계산
     * @return Scope 1 + Scope 2 + Scope 3 총합
     */
    public BigDecimal getTotalEmission() {
        if (totalEmission != null) {
            return totalEmission;
        }
        
        BigDecimal total = BigDecimal.ZERO;
        if (scope1Total != null) {
            total = total.add(scope1Total);
        }
        if (scope2Total != null) {
            total = total.add(scope2Total);
        }
        if (scope3Total != null) {
            total = total.add(scope3Total);
        }
        return total;
    }
}