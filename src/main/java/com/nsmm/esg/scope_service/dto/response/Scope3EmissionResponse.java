package com.nsmm.esg.scope_service.dto.response;

import com.nsmm.esg.scope_service.entity.Scope3Emission;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Scope 3 배출량 응답 DTO
 * 
 * 프론트엔드로 전송되는 데이터 구조:
 * - 민감한 정보 제외 (권한 제어 필드 등)
 * - 사용자 친화적인 형식으로 변환
 * - 카테고리 정보 포함
 * 
 * @author ESG Project Team
 * @version 1.0
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Scope 3 배출량 응답 DTO (프론트엔드와 1:1 매핑)")
public class Scope3EmissionResponse {

  @Schema(description = "Scope 3 배출량 고유 식별자", example = "1")
  private Long id; // Scope 3 배출량 고유 식별자

  @Schema(description = "대분류", example = "구매한 제품 및 서비스")
  private String majorCategory; // 대분류

  @Schema(description = "구분", example = "원재료")
  private String subcategory; // 구분

  @Schema(description = "원료/에너지", example = "철강")
  private String rawMaterial; // 원료/에너지

  @Schema(description = "단위", example = "kg")
  private String unit; // 단위

  @Schema(description = "배출계수 (kgCO2eq/단위)", example = "2.1")
  private BigDecimal emissionFactor; // 배출계수 (kgCO2eq/단위)

  @Schema(description = "수량(활동량)", example = "1000")
  private BigDecimal activityAmount; // 수량(활동량)

  @Schema(description = "계산된 배출량", example = "2100.0")
  private BigDecimal totalEmission; // 계산된 배출량

  @Schema(description = "수동 입력 여부", example = "true")
  private Boolean isManualInput; // 수동 입력 여부 (true: 수동, false: 자동)

  @Schema(description = "보고 연도", example = "2024")
  private Integer reportingYear; // 보고 연도

  @Schema(description = "보고 월", example = "6")
  private Integer reportingMonth; // 보고 월

  @Schema(description = "카테고리 번호 (1~15)", example = "1")
  private Integer categoryNumber; // 카테고리 번호 (1-15)

  @Schema(description = "카테고리 명칭", example = "구매한 제품 및 서비스")
  private String categoryName; // 카테고리 명칭

  @Schema(description = "생성 일시", example = "2024-06-20T12:34:56")
  private LocalDateTime createdAt; // 생성 일시

  @Schema(description = "수정 일시", example = "2024-06-20T12:34:56")
  private LocalDateTime updatedAt; // 수정 일시

  /**
   * 엔티티에서 DTO로 변환하는 정적 팩토리 메서드
   * 
   * @param entity Scope3Emission 엔티티
   * @return Scope3EmissionResponse DTO
   */
  public static Scope3EmissionResponse from(Scope3Emission entity) {
    return Scope3EmissionResponse.builder()
        .id(entity.getId())
        .majorCategory(entity.getMajorCategory())
        .subcategory(entity.getSubcategory())
        .rawMaterial(entity.getRawMaterial())
        .unit(entity.getUnit())
        .emissionFactor(entity.getEmissionFactor())
        .activityAmount(entity.getActivityAmount())
        .totalEmission(entity.getTotalEmission())
        .isManualInput(entity.getIsManualInput()) // 수동 입력 여부 추가
        .reportingYear(entity.getReportingYear())
        .reportingMonth(entity.getReportingMonth())
        .categoryNumber(entity.getCategoryNumber())
        .categoryName(entity.getCategoryName())
        .createdAt(entity.getCreatedAt())
        .updatedAt(entity.getUpdatedAt())
        .build();
  }
}