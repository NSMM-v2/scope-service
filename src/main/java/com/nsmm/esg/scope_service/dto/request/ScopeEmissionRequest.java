package com.nsmm.esg.scope_service.dto.request;

import com.nsmm.esg.scope_service.enums.InputType;
import com.nsmm.esg.scope_service.enums.ScopeType;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * 통합 Scope 배출량 요청 DTO
 * 
 * 특징:
 * - Scope 1, 2, 3 통합 지원
 * - 프론트엔드 폼 구조와 1:1 매핑
 * - 자재 코드 매핑 지원
 * - 카테고리별 유효성 검증
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "통합 Scope 배출량 요청 DTO (Scope 1, 2, 3 통합 지원)")
public class ScopeEmissionRequest {

  // ========================================================================
  // Scope 분류 및 카테고리 정보 (Scope Classification & Category)
  // ========================================================================

  @Schema(description = "Scope 타입", example = "SCOPE3", allowableValues = { "SCOPE1", "SCOPE2", "SCOPE3" })
  @NotNull(message = "Scope 타입은 필수입니다")
  private ScopeType scopeType; // SCOPE1, SCOPE2, SCOPE3

  @Schema(description = "Scope 1 카테고리 번호 (1-10)", example = "4")
  @Min(value = 1, message = "Scope 1 카테고리 번호는 1 이상이어야 합니다")
  @Max(value = 10, message = "Scope 1 카테고리 번호는 10 이하여야 합니다")
  private Integer scope1CategoryNumber; // 1-10 (list1-10)

  @Schema(description = "Scope 2 카테고리 번호 (1-2)", example = "1")
  @Min(value = 1, message = "Scope 2 카테고리 번호는 1 이상이어야 합니다")
  @Max(value = 2, message = "Scope 2 카테고리 번호는 2 이하여야 합니다")
  private Integer scope2CategoryNumber; // 1-2 (list1-2)

  @Schema(description = "Scope 3 카테고리 번호 (1-15)", example = "6")
  @Min(value = 1, message = "Scope 3 카테고리 번호는 1 이상이어야 합니다")
  @Max(value = 15, message = "Scope 3 카테고리 번호는 15 이하여야 합니다")
  private Integer scope3CategoryNumber; // 1-15 (list1-15)

  // ========================================================================
  // 자재코드 매핑 정보 (Material Code Mapping)
  // ========================================================================

  @Schema(description = "자재 할당 정보 ID", example = "1")
  private Long materialAssignmentId; // 자재 할당 정보 연결용 ID

  @Schema(description = "자재 매핑 정보 ID", example = "1")
  private Long materialMappingId; // 자재 매핑 정보 연결용 ID

  @Schema(description = "상위에서 할당받은 자재코드", example = "A100")
  @Size(max = 50, message = "상위 자재코드는 50자 이하여야 합니다")
  private String upstreamMaterialCode; // 상위에서 할당받은 자재코드 (A100, FE100...) - 최상위인 경우 null

  @Schema(description = "내부 자재코드", example = "B100")
  @Size(max = 50, message = "내부 자재코드는 50자 이하여야 합니다")
  private String internalMaterialCode; // 내부 자재코드 (B100, FE200...)

  @Schema(description = "자재명", example = "철강 부품")
  @Size(max = 200, message = "자재명은 200자 이하여야 합니다")
  private String materialName; // 자재명

  @Schema(description = "상위 협력사 ID", example = "1")
  private Long upstreamPartnerId; // 상위 협력사 ID (null이면 본사)

  // ========================================================================
  // 프론트엔드 입력 데이터 (Frontend Input Data)
  // ========================================================================

  @Schema(description = "대분류", example = "구매한 자재 및 서비스")
  @NotBlank(message = "대분류는 필수입니다")
  @Size(max = 100, message = "대분류는 100자 이하여야 합니다")
  private String majorCategory; // 대분류

  @Schema(description = "구분", example = "원재료")
  @NotBlank(message = "구분은 필수입니다")
  @Size(max = 100, message = "구분은 100자 이하여야 합니다")
  private String subcategory; // 구분

  @Schema(description = "원료/에너지", example = "철강")
  @NotBlank(message = "원료/에너지는 필수입니다")
  @Size(max = 100, message = "원료/에너지는 100자 이하여야 합니다")
  private String rawMaterial; // 원료/에너지

  @Schema(description = "수량(활동량)", example = "1000")
  @NotNull(message = "수량(활동량)은 필수입니다")
  @DecimalMin(value = "0.001", message = "수량은 0.001 이상이어야 합니다")
  @Digits(integer = 12, fraction = 3, message = "수량은 정수 12자리, 소수점 3자리까지 가능합니다")
  private BigDecimal activityAmount; // 수량 (quantity)

  @Schema(description = "단위", example = "kg")
  @NotBlank(message = "단위는 필수입니다")
  @Size(max = 20, message = "단위는 20자 이하여야 합니다")
  private String unit; // 단위

  @Schema(description = "배출계수 (kgCO2eq/단위)", example = "2.1")
  @NotNull(message = "배출계수는 필수입니다")
  @DecimalMin(value = "0.000001", message = "배출계수는 0.000001 이상이어야 합니다")
  @Digits(integer = 9, fraction = 6, message = "배출계수는 정수 9자리, 소수점 6자리까지 가능합니다")
  private BigDecimal emissionFactor; // 배출계수

  @Schema(description = "계산된 배출량", example = "2100.0")
  @NotNull(message = "계산된 배출량은 필수입니다")
  @DecimalMin(value = "0.000001", message = "계산된 배출량은 0.000001 이상이어야 합니다")
  @Digits(integer = 15, fraction = 6, message = "계산된 배출량은 정수 15자리, 소수점 6자리까지 가능합니다")
  private BigDecimal totalEmission; // 계산된 배출량

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

  // ========================================================================
  // 입력 모드 제어 (Input Mode Control)
  // ========================================================================
  @Schema(description = "입력 타입 (MANUAL/LCA)", example = "MANUAL")
  @NotNull(message = "입력 타입은 필수입니다")
  @Builder.Default
  private InputType inputType = InputType.MANUAL;

  @Schema(description = "자재 코드 매핑 여부", example = "false")
  @NotNull(message = "자재 코드 매핑 여부는 필수입니다")
  @Builder.Default
  private Boolean hasMaterialMapping = false;

  @Schema(description = "공장 설비 활성화 여부", example = "false")
  @NotNull(message = "공장 설비 활성화 여부는 필수입니다")
  @Builder.Default
  private Boolean factoryEnabled = false;

  // ========================================================================
  // 편의 메서드 (Convenience Methods)
  // ========================================================================

  /**
   * 현재 활성 카테고리 번호 반환
   */
  public Integer getActiveCategoryNumber() {
    return switch (scopeType) {
      case SCOPE1 -> scope1CategoryNumber;
      case SCOPE2 -> scope2CategoryNumber;
      case SCOPE3 -> scope3CategoryNumber;
    };
  }

  /**
   * 기존 ScopeEmissionRequest와의 호환성을 위한 메서드
   */
  public Integer getCategoryNumber() {
    return getActiveCategoryNumber();
  }

  // ========================================================================
  // 호환성 메서드 (Compatibility Methods)
  // ========================================================================

  /**
   * 기존 mappedMaterialCode 호환성
   */
  public String getMappedMaterialCode() {
    return this.upstreamMaterialCode;
  }

  public void setMappedMaterialCode(String mappedMaterialCode) {
    this.upstreamMaterialCode = mappedMaterialCode;
  }

  /**
   * 기존 assignedMaterialCode 호환성
   */
  public String getAssignedMaterialCode() {
    return this.internalMaterialCode;
  }

  public void setAssignedMaterialCode(String assignedMaterialCode) {
    this.internalMaterialCode = assignedMaterialCode;
  }

  /**
   * 기존 mappedMaterialName 호환성
   */
  public String getMappedMaterialName() {
    return this.materialName;
  }

  public void setMappedMaterialName(String mappedMaterialName) {
    this.materialName = mappedMaterialName;
  }

  /**
   * 기존 companyMaterialCode 호환성 (internalMaterialCode와 동일)
   */
  public String getCompanyMaterialCode() {
    return this.internalMaterialCode;
  }

  public void setCompanyMaterialCode(String companyMaterialCode) {
    this.internalMaterialCode = companyMaterialCode;
  }

}