package com.nsmm.esg.scope_service.dto.response;

import com.nsmm.esg.scope_service.entity.Scope3Category;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Scope 3 카테고리 응답 DTO
 * 
 * 프론트엔드 카테고리 선택기와 연동:
 * - 카테고리 목록 제공
 * - 업스트림/다운스트림 구분
 * 
 * @author ESG Project Team
 * @version 1.0
 * @since 2024
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Scope 3 카테고리 응답 DTO (카테고리 선택기 연동)")
public class Scope3CategoryResponse {

  @Schema(description = "카테고리 번호 (1~15)", example = "1")
  private Integer categoryNumber; // 카테고리 번호 (1-15)

  @Schema(description = "카테고리 한국어 명칭", example = "구매한 제품 및 서비스")
  private String categoryName; // 카테고리 한국어 명칭

  @Schema(description = "업스트림/다운스트림 구분", example = "업스트림")
  private String categoryType; // 업스트림/다운스트림 구분

  /**
   * Scope3Category Enum에서 DTO로 변환하는 정적 팩토리 메서드
   * 
   * @param category Scope3Category Enum
   * @return Scope3CategoryResponse DTO
   */
  public static Scope3CategoryResponse from(Scope3Category category) {
    return Scope3CategoryResponse.builder()
        .categoryNumber(category.getCategoryNumber())
        .categoryName(category.getCategoryName())
        .categoryType(category.isUpstream() ? "업스트림" : "다운스트림")
        .build();
  }
}