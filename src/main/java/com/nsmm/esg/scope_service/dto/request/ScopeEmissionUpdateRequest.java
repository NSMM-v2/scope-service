package com.nsmm.esg.scope_service.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

import com.nsmm.esg.scope_service.enums.InputType;

/**
 * 통합 Scope 배출량 업데이트 요청 DTO
 * 
 * 특징:
 * - 모든 필드가 선택적 (null인 경우 업데이트하지 않음)
 * - 프론트엔드에서 유연한 부분 업데이트 가능
 * - 기존 Scope3EmissionUpdateRequest와 호환
 * 
 * @author ESG 프로젝트팀
 * @version 1.0
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "통합 Scope 배출량 업데이트 요청 DTO (모든 필드 부분 업데이트 지원)")
public class ScopeEmissionUpdateRequest {

  // ========================================================================
  // 입력 모드 제어 (Input Mode Control)
  // ========================================================================

  @Schema(description = "입력 타입 (MANUAL/LCA)", example = "MANUAL")
  private InputType inputType;

  @Schema(description = "제품 코드 매핑 여부", example = "false")
  private Boolean hasProductMapping;

  // ========================================================================
  // 제품 코드 매핑 정보 (Product Code Mapping)
  // ========================================================================

  @Schema(description = "회사별 제품 코드", example = "L01")
  @Size(max = 50, message = "제품 코드는 50자 이하여야 합니다")
  private String companyProductCode; // 각 회사별 제품 코드

  @Schema(description = "제품명", example = "휠")
  @Size(max = 100, message = "제품명은 100자 이하여야 합니다")
  private String productName; // 제품명

  // ========================================================================
  // 프론트엔드 입력 데이터 (Frontend Input Data)
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

  @Schema(description = "수량(활동량)", example = "1000")
  @DecimalMin(value = "0.001", message = "수량은 0.001 이상이어야 합니다")
  @Digits(integer = 12, fraction = 3, message = "수량은 정수 12자리, 소수점 3자리까지 가능합니다")
  private BigDecimal activityAmount; // 수량 (quantity)

  @Schema(description = "단위", example = "kg")
  @Size(max = 20, message = "단위는 20자 이하여야 합니다")
  private String unit; // 단위

  @Schema(description = "배출계수 (kgCO2eq/단위)", example = "2.1")
  @DecimalMin(value = "0.000001", message = "배출계수는 0.000001 이상이어야 합니다")
  @Digits(integer = 9, fraction = 6, message = "배출계수는 정수 9자리, 소수점 6자리까지 가능합니다")
  private BigDecimal emissionFactor; // 배출계수

  @Schema(description = "계산된 배출량", example = "2100.0")
  @DecimalMin(value = "0.000001", message = "계산된 배출량은 0.000001 이상이어야 합니다")
  @Digits(integer = 15, fraction = 6, message = "계산된 배출량은 정수 15자리, 소수점 6자리까지 가능합니다")
  private BigDecimal totalEmission; // 계산된 배출량

  // ========================================================================
  // 보고 기간 정보 (Reporting Period)
  // ========================================================================

  @Schema(description = "보고 연도", example = "2024")
  @NotNull(message = "보고 연도는 필수입니다")
  @Min(value = 2020, message = "보고 연도는 2020년 이상이어야 합니다")
  @Max(value = 2030, message = "보고 연도는 2030년 이하이어야 합니다")
  private Integer reportingYear;

  @Schema(description = "보고 월", example = "6")
  @NotNull(message = "보고 월은 필수입니다")
  @Min(value = 1, message = "보고 월은 1 이상이어야 합니다")
  @Max(value = 12, message = "보고 월은 12 이하이어야 합니다")
  private Integer reportingMonth;


}