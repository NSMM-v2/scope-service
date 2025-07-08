package com.nsmm.esg.scope_service.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 제품별 배출량 집계 요약 DTO
 * 
 * 특징:
 * - company_product_code 기준 집계
 * - Scope 1, 2, 3 배출량 분리
 * - 제품별 총 배출량 제공
 */
@Schema(description = "제품별 배출량 집계 요약")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProductEmissionSummary {

  @Schema(description = "회사별 제품 코드", example = "L01")
  private String companyProductCode;

  @Schema(description = "제품명", example = "휠")
  private String productName;

  @Schema(description = "Scope 1 배출량 (tCO2eq)", example = "125.50")
  private BigDecimal scope1Emission;

  @Schema(description = "Scope 2 배출량 (tCO2eq)", example = "89.75")
  private BigDecimal scope2Emission;

  @Schema(description = "Scope 3 배출량 (tCO2eq)", example = "345.25")
  private BigDecimal scope3Emission;

  @Schema(description = "제품별 총 배출량 (tCO2eq)", example = "560.50")
  private BigDecimal totalEmission;

  // ========================================================================
  // 정적 팩토리 메서드 (Static Factory Methods)
  // ========================================================================

  /**
   * Repository 쿼리 결과에서 DTO 생성
   * 
   * @param queryResult [companyProductCode, productName, scope1, scope2, scope3,
   *                    total]
   * @return ProductEmissionSummary DTO
   */
  public static ProductEmissionSummary from(Object[] queryResult) {
    return ProductEmissionSummary.builder()
        .companyProductCode((String) queryResult[0])
        .productName((String) queryResult[1])
        .scope1Emission((BigDecimal) queryResult[2])
        .scope2Emission((BigDecimal) queryResult[3])
        .scope3Emission((BigDecimal) queryResult[4])
        .totalEmission((BigDecimal) queryResult[5])
        .build();
  }

  /**
   * 개별 필드에서 DTO 생성
   */
  public static ProductEmissionSummary of(
      String companyProductCode,
      String productName,
      BigDecimal scope1Emission,
      BigDecimal scope2Emission,
      BigDecimal scope3Emission) {

    BigDecimal total = scope1Emission
        .add(scope2Emission)
        .add(scope3Emission);

    return ProductEmissionSummary.builder()
        .companyProductCode(companyProductCode)
        .productName(productName)
        .scope1Emission(scope1Emission)
        .scope2Emission(scope2Emission)
        .scope3Emission(scope3Emission)
        .totalEmission(total)
        .build();
  }
}