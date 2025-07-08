package com.nsmm.esg.scope_service.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 계층별 배출량 집계 요약 DTO
 */
@Schema(description = "계층별 배출량 집계 요약")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class HierarchicalEmissionSummary {

  @Schema(description = "계층 경로", example = "/1/L1-001/L2-003/")
  private String treePath;

  @Schema(description = "회사명", example = "1차 협력사 A")
  private String companyName;

  @Schema(description = "계층 레벨", example = "2")
  private Integer level;

  @Schema(description = "Scope 1 배출량 (tCO2eq)", example = "450.30")
  private BigDecimal scope1Emission;

  @Schema(description = "Scope 2 배출량 (tCO2eq)", example = "230.50")
  private BigDecimal scope2Emission;

  @Schema(description = "Scope 3 배출량 (tCO2eq)", example = "1250.75")
  private BigDecimal scope3Emission;

  @Schema(description = "계층별 총 배출량 (tCO2eq)", example = "1931.55")
  private BigDecimal totalEmission;

  @Schema(description = "하위 조직 수", example = "3")
  private Integer childCount;
}