package com.nsmm.esg.scope_service.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Scope 배출량 종합 집계 응답 DTO
 */
@Schema(description = "Scope 배출량 종합 집계 응답")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ScopeAggregationResponse {

  @Schema(description = "보고 연도", example = "2024")
  private Integer reportingYear;

  @Schema(description = "보고 월", example = "12")
  private Integer reportingMonth;

  @Schema(description = "Scope 1 총 배출량 (tCO2eq)", example = "1250.50")
  private BigDecimal scope1Total;

  @Schema(description = "Scope 2 총 배출량 (tCO2eq)", example = "890.75")
  private BigDecimal scope2Total;

  @Schema(description = "Scope 3 총 배출량 (tCO2eq)", example = "3450.25")
  private BigDecimal scope3Total;

  @Schema(description = "전체 Scope 총 배출량 (tCO2eq)", example = "5591.50")
  private BigDecimal totalEmission;

  // Scope 3 특수 집계 결과
  @Schema(description = "Scope 3 Cat.1 집계 배출량", example = "2150.30")
  private BigDecimal scope3Category1Aggregated;

  @Schema(description = "Scope 3 Cat.2 집계 배출량", example = "950.45")
  private BigDecimal scope3Category2Aggregated;

  @Schema(description = "Scope 3 Cat.4 집계 배출량", example = "680.20")
  private BigDecimal scope3Category4Aggregated;

  @Schema(description = "Scope 3 Cat.5 집계 배출량", example = "320.15")
  private BigDecimal scope3Category5Aggregated;

  @Schema(description = "집계 상세 정보")
  private AggregationDetails aggregationDetails;

  @Schema(description = "계층별 배출량 집계 목록")
  private List<HierarchicalEmissionSummary> hierarchicalSummaries;
}