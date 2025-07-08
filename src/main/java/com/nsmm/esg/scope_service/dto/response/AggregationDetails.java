package com.nsmm.esg.scope_service.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 집계 계산 상세 정보 DTO
 */
@Schema(description = "집계 계산 상세 정보")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AggregationDetails {

  // Cat.1 집계 상세
  @Schema(description = "Cat.1 집계 - Scope 1 포함된 배출량", example = "800.25")
  private BigDecimal cat1Scope1Included;

  @Schema(description = "Cat.1 집계 - Scope 1 제외된 배출량", example = "450.25")
  private BigDecimal cat1Scope1Excluded;

  @Schema(description = "Cat.1 집계 - Scope 2 포함된 배출량", example = "600.50")
  private BigDecimal cat1Scope2Included;

  @Schema(description = "Cat.1 집계 - Scope 2 제외된 배출량", example = "290.25")
  private BigDecimal cat1Scope2Excluded;

  @Schema(description = "Cat.1 집계 - Scope 3 Cat.1 배출량", example = "750.00")
  private BigDecimal cat1Scope3Original;

  // Cat.2 집계 상세
  @Schema(description = "Cat.2 집계 - Scope 1 공장설비 배출량", example = "200.30")
  private BigDecimal cat2Scope1Factory;

  @Schema(description = "Cat.2 집계 - Scope 2 공장설비 배출량", example = "400.15")
  private BigDecimal cat2Scope2Factory;

  @Schema(description = "Cat.2 집계 - Scope 3 Cat.2 배출량", example = "350.00")
  private BigDecimal cat2Scope3Original;

  // Cat.4 집계 상세
  @Schema(description = "Cat.4 집계 - Scope 1 이동연소 배출량", example = "300.20")
  private BigDecimal cat4Scope1Mobile;

  @Schema(description = "Cat.4 집계 - Scope 3 Cat.4 배출량", example = "380.00")
  private BigDecimal cat4Scope3Original;

  // Cat.5 집계 상세
  @Schema(description = "Cat.5 집계 - Scope 1 폐수처리 배출량", example = "150.15")
  private BigDecimal cat5Scope1Waste;

  @Schema(description = "Cat.5 집계 - Scope 3 Cat.5 배출량", example = "170.00")
  private BigDecimal cat5Scope3Original;
}