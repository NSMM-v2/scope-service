package com.nsmm.esg.scope_service.dto.response;

import com.nsmm.esg.scope_service.enums.InputType;
import com.nsmm.esg.scope_service.enums.ScopeType;
import com.nsmm.esg.scope_service.entity.ScopeEmission;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 통합 Scope 배출량 응답 DTO
 * 
 * 프론트엔드로 전송되는 데이터 구조:
 * - 민감한 정보 제외 (권한 제어 필드 등)
 * - 사용자 친화적인 형식으로 변환
 * - 모든 Scope 타입 지원
 * - 기존 Scope3EmissionResponse와 호환
 * 
 * @author ESG Project Team
 * @version 1.0
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "통합 Scope 배출량 응답 DTO (Scope 1, 2, 3 통합 지원)")
public class ScopeEmissionResponse {

  @Schema(description = "배출량 고유 식별자", example = "1")
  private Long id; // 배출량 고유 식별자

  // ========================================================================
  // Scope 분류 및 카테고리 정보 (Scope Classification & Category)
  // ========================================================================

  @Schema(description = "Scope 타입", example = "SCOPE3")
  private ScopeType scopeType; // SCOPE1, SCOPE2, SCOPE3

  @Schema(description = "Scope 1 카테고리 번호", example = "4")
  private Integer scope1CategoryNumber; // 1-10

  @Schema(description = "Scope 1 카테고리명", example = "이동연소 1")
  private String scope1CategoryName; // 카테고리명

  @Schema(description = "Scope 1 카테고리 그룹", example = "이동연소")
  private String scope1CategoryGroup; // 그룹명

  @Schema(description = "Scope 2 카테고리 번호", example = "1")
  private Integer scope2CategoryNumber; // 1-2

  @Schema(description = "Scope 2 카테고리명", example = "전력 사용")
  private String scope2CategoryName; // 카테고리명

  @Schema(description = "Scope 3 카테고리 번호", example = "6")
  private Integer scope3CategoryNumber; // 1-15

  @Schema(description = "Scope 3 카테고리명", example = "사업장 관련 활동")
  private String scope3CategoryName; // 카테고리명

  // ========================================================================
  // 제품 코드 매핑 정보 (Product Code Mapping)
  // ========================================================================

  @Schema(description = "회사별 제품 코드", example = "L01")
  private String companyProductCode; // 각 회사별 제품 코드

  @Schema(description = "제품명", example = "휠")
  private String productName; // 제품명

  // ========================================================================
  // 프론트엔드 입력 데이터 (Frontend Input Data)
  // ========================================================================

  @Schema(description = "대분류", example = "구매한 제품 및 서비스")
  private String majorCategory; // 대분류

  @Schema(description = "구분", example = "원재료")
  private String subcategory; // 구분

  @Schema(description = "원료/에너지", example = "철강")
  private String rawMaterial; // 원료/에너지

  @Schema(description = "수량(활동량)", example = "1000")
  private BigDecimal activityAmount; // 수량 (quantity)

  @Schema(description = "단위", example = "kg")
  private String unit; // 단위

  @Schema(description = "배출계수 (kgCO2eq/단위)", example = "2.1")
  private BigDecimal emissionFactor; // 배출계수

  @Schema(description = "계산된 배출량", example = "2100.0")
  private BigDecimal totalEmission; // 계산된 배출량

  // ========================================================================
  // 입력 모드 및 보고 기간 (Input Mode & Reporting Period)
  // ========================================================================

  @Schema(description = "입력 타입", example = "MANUAL")
  private InputType inputType;

  @Schema(description = "제품 코드 매핑 여부", example = "false")
  private Boolean hasProductMapping;

  @Schema(description = "보고 연도", example = "2024")
  private Integer reportingYear; // 보고 연도

  @Schema(description = "보고 월", example = "6")
  private Integer reportingMonth; // 보고 월

  // ========================================================================
  // 감사 정보 (Audit Information)
  // ========================================================================

  @Schema(description = "생성 일시", example = "2024-06-15T10:30:00")
  private LocalDateTime createdAt; // 생성 일시

  @Schema(description = "수정 일시", example = "2024-06-15T14:20:00")
  private LocalDateTime updatedAt; // 수정 일시

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
   * 현재 활성 카테고리명 반환
   */
  public String getActiveCategoryName() {
    return switch (scopeType) {
      case SCOPE1 -> scope1CategoryName;
      case SCOPE2 -> scope2CategoryName;
      case SCOPE3 -> scope3CategoryName;
    };
  }

  public static ScopeEmissionResponse from(ScopeEmission emission) {
    return ScopeEmissionResponse.builder()
        .id(emission.getId())
        .scopeType(emission.getScopeType())
        .scope1CategoryNumber(emission.getScope1CategoryNumber())
        .scope1CategoryName(emission.getScope1CategoryName())
        .scope1CategoryGroup(emission.getScope1CategoryGroup())
        .scope2CategoryNumber(emission.getScope2CategoryNumber())
        .scope2CategoryName(emission.getScope2CategoryName())
        .scope3CategoryNumber(emission.getScope3CategoryNumber())
        .scope3CategoryName(emission.getScope3CategoryName())
        .companyProductCode(emission.getCompanyProductCode())
        .productName(emission.getProductName())
        .majorCategory(emission.getMajorCategory())
        .subcategory(emission.getSubcategory())
        .rawMaterial(emission.getRawMaterial())
        .activityAmount(emission.getActivityAmount())
        .unit(emission.getUnit())
        .emissionFactor(emission.getEmissionFactor())
        .totalEmission(emission.getTotalEmission())
        .inputType(emission.getInputType())
        .hasProductMapping(emission.getHasProductMapping())
        .reportingYear(emission.getReportingYear())
        .reportingMonth(emission.getReportingMonth())
        .createdAt(emission.getCreatedAt())
        .updatedAt(emission.getUpdatedAt())
        .build();
  }
}