package com.nsmm.esg.scope_service.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * Scope 3 배출량 업데이트 요청 DTO
 * 
 * 부분 업데이트 지원:
 * - 생성 시와 동일한 모든 필드 수정 가능
 * - 모든 필드가 Optional (null이 아닌 필드만 업데이트)
 * - 프론트엔드에서 유연한 부분 업데이트 가능
 * 
 * @author ESG Project Team
 * @version 1.0
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Scope 3 배출량 업데이트 요청 DTO (모든 필드 부분 업데이트 지원)")
public class Scope3EmissionUpdateRequest {

  // ========================================================================
  // 비즈니스 데이터 필드 (Business Data Fields)
  // ========================================================================

  @Schema(description = "대분류", example = "구매한 제품 및 서비스")
  @Size(max = 100, message = "대분류는 100자 이하여야 합니다")
  private String majorCategory; // 대분류

  @Schema(description = "구분", example = "원재료")
  @Size(max = 100, message = "구분은 100자 이하여야 합니다")
  private String subcategory; // 구분

  @Schema(description = "원료/에너지", example = "철강")
  @Size(max = 100, message = "원료/에너지는 100자 이하여야 합니다")
  private String rawMaterial; // 원료/에너지

  @Schema(description = "단위", example = "kg")
  @Size(max = 20, message = "단위는 20자 이하여야 합니다")
  private String unit; // 단위

  @Schema(description = "배출계수 (kgCO2eq/단위)", example = "2.1")
  @DecimalMin(value = "0.000001", message = "배출계수는 0.000001 이상이어야 합니다")
  @Digits(integer = 9, fraction = 6, message = "배출계수는 정수 9자리, 소수점 6자리까지 가능합니다")
  private BigDecimal emissionFactor; // 배출계수

  @Schema(description = "수량(활동량)", example = "1000")
  @DecimalMin(value = "0.001", message = "수량은 0.001 이상이어야 합니다")
  @Digits(integer = 12, fraction = 3, message = "수량은 정수 12자리, 소수점 3자리까지 가능합니다")
  private BigDecimal activityAmount; // 수량(활동량)

  @Schema(description = "계산된 배출량", example = "2100.0")
  @DecimalMin(value = "0.000001", message = "계산된 배출량은 0.000001 이상이어야 합니다")
  @Digits(integer = 15, fraction = 6, message = "계산된 배출량은 정수 15자리, 소수점 6자리까지 가능합니다")
  private BigDecimal totalEmission; // 계산된 배출량

  // ========================================================================
  // 메타데이터 필드 (Metadata Fields)
  // ========================================================================

  @Schema(description = "보고 연도", example = "2024")
  @Min(value = 2020, message = "보고 연도는 2020년 이상이어야 합니다")
  @Max(value = 2030, message = "보고 연도는 2030년 이하여야 합니다")
  private Integer reportingYear; // 보고 연도

  @Schema(description = "보고 월", example = "6")
  @Min(value = 1, message = "보고 월은 1 이상이어야 합니다")
  @Max(value = 12, message = "보고 월은 12 이하여야 합니다")
  private Integer reportingMonth; // 보고 월

  @Schema(description = "카테고리 번호 (1~15)", example = "1")
  @Min(value = 1, message = "카테고리 번호는 1 이상이어야 합니다")
  @Max(value = 15, message = "카테고리 번호는 15 이하여야 합니다")
  private Integer categoryNumber; // 카테고리 번호 (1-15)

  @Schema(description = "카테고리 명칭", example = "구매한 제품 및 서비스")
  @Size(max = 100, message = "카테고리 명칭은 100자 이하여야 합니다")
  private String categoryName; // 카테고리 명칭

  // ========================================================================
  // 유틸리티 메서드 (Utility Methods)
  // ========================================================================

  /**
   * 업데이트할 필드가 있는지 확인
   * 
   * @return 업데이트할 필드가 하나라도 있으면 true
   */
  public boolean hasAnyField() {
    return majorCategory != null || subcategory != null || rawMaterial != null ||
        unit != null || emissionFactor != null || activityAmount != null ||
        totalEmission != null || reportingYear != null || reportingMonth != null ||
        categoryNumber != null || categoryName != null;
  }

  /**
   * 총 배출량 자동 계산이 필요한지 확인
   * totalEmission이 제공되지 않았고 activityAmount나 emissionFactor가 변경된 경우
   * 
   * @return 자동 계산이 필요하면 true
   */
  public boolean needsEmissionCalculation() {
    return totalEmission == null && (activityAmount != null || emissionFactor != null);
  }

  /**
   * 중복 검증이 필요한 핵심 필드가 변경되었는지 확인
   * 
   * @return 핵심 필드가 변경되었으면 true
   */
  public boolean hasKeyFields() {
    return reportingYear != null || reportingMonth != null || categoryNumber != null ||
        majorCategory != null || subcategory != null || rawMaterial != null;
  }
}