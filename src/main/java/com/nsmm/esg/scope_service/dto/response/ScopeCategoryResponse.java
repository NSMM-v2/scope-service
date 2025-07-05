package com.nsmm.esg.scope_service.dto.response;

import com.nsmm.esg.scope_service.entity.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.stream.Collectors;
import java.util.Arrays;

/**
 * 통합 Scope 카테고리 응답 DTO
 * 
 * 프론트엔드 카테고리 선택기와 연동:
 * - 모든 Scope 타입 카테고리 목록 제공
 * - 그룹별 분류 지원
 * - 기존 Scope3CategoryResponse와 호환
 * 
 * @author ESG Project Team
 * @version 1.0
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "통합 Scope 카테고리 응답 DTO (모든 Scope 타입 지원)")
public class ScopeCategoryResponse {

  @Schema(description = "Scope 타입", example = "SCOPE3")
  private ScopeType scopeType; // SCOPE1, SCOPE2, SCOPE3

  @Schema(description = "카테고리 번호", example = "6")
  private Integer categoryNumber; // 카테고리 번호

  @Schema(description = "카테고리 한국어 명칭", example = "사업장 관련 활동")
  private String categoryName; // 카테고리 한국어 명칭

  @Schema(description = "카테고리 그룹", example = "업스트림")
  private String categoryGroup; // 카테고리 그룹

  @Schema(description = "카테고리 설명", example = "출장 등 사업장 관련 활동")
  private String description; // 카테고리 설명

  // ========================================================================
  // 정적 팩토리 메서드 (Static Factory Methods)
  // ========================================================================

  /**
   * Scope1Category에서 DTO로 변환
   */
  public static ScopeCategoryResponse from(Scope1Category category) {
    return ScopeCategoryResponse.builder()
        .scopeType(ScopeType.SCOPE1)
        .categoryNumber(category.getCategoryNumber())
        .categoryName(category.getCategoryName())
        .categoryGroup(category.getGroupName())
        .description(category.getDescription())
        .build();
  }

  /**
   * Scope2Category에서 DTO로 변환
   */
  public static ScopeCategoryResponse from(Scope2Category category) {
    return ScopeCategoryResponse.builder()
        .scopeType(ScopeType.SCOPE2)
        .categoryNumber(category.getCategoryNumber())
        .categoryName(category.getCategoryName())
        .categoryGroup("에너지")
        .description(category.getDescription())
        .build();
  }

  /**
   * Scope3Category에서 DTO로 변환
   */
  public static ScopeCategoryResponse from(Scope3Category category) {
    return ScopeCategoryResponse.builder()
        .scopeType(ScopeType.SCOPE3)
        .categoryNumber(category.getCategoryNumber())
        .categoryName(category.getCategoryName())
        .categoryGroup(category.isUpstream() ? "업스트림" : "다운스트림")
        .description(category.getCategoryName())
        .build();
  }

  // ========================================================================
  // 카테고리 목록 조회 메서드 (Category List Methods)
  // ========================================================================

  /**
   * 모든 Scope 1 카테고리 목록 반환
   */
  public static List<ScopeCategoryResponse> getAllScope1Categories() {
    return Arrays.stream(Scope1Category.values())
        .map(ScopeCategoryResponse::from)
        .collect(Collectors.toList());
  }

  /**
   * 모든 Scope 2 카테고리 목록 반환
   */
  public static List<ScopeCategoryResponse> getAllScope2Categories() {
    return Arrays.stream(Scope2Category.values())
        .map(ScopeCategoryResponse::from)
        .collect(Collectors.toList());
  }

  /**
   * 모든 Scope 3 카테고리 목록 반환
   */
  public static List<ScopeCategoryResponse> getAllScope3Categories() {
    return Arrays.stream(Scope3Category.values())
        .map(ScopeCategoryResponse::from)
        .collect(Collectors.toList());
  }

  /**
   * 특정 Scope 타입의 모든 카테고리 목록 반환
   */
  public static List<ScopeCategoryResponse> getAllCategoriesByScope(ScopeType scopeType) {
    return switch (scopeType) {
      case SCOPE1 -> getAllScope1Categories();
      case SCOPE2 -> getAllScope2Categories();
      case SCOPE3 -> getAllScope3Categories();
    };
  }
}