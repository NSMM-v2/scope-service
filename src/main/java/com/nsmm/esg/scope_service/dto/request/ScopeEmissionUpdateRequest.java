package com.nsmm.esg.scope_service.dto.request;

import lombok.Getter;
import lombok.Setter;

/**
 * Scope 배출량 수정 요청 DTO
 * - 모든 필드는 선택 입력 (null 허용)
 * - 제품 정보는 Scope 1, 2에서만 사용
 */
@Getter
@Setter
public class ScopeEmissionUpdateRequest {
  private String companyProduct; // 제품명
  private String companyProductCode; // 제품 코드
  private String majorCategory; // 대분류
  private String subcategory; // 소분류
  private String rawMaterial; // 원재료
  private Double activityAmount; // 활동량
  private String unit; // 단위
  private Double emissionFactor; // 배출계수
  private Double totalEmission; // 총 배출량
  private Boolean isManualInput; // 수동입력 여부
  private Integer categoryNumber; // 카테고리 번호
  private String categoryName; // 카테고리명
}