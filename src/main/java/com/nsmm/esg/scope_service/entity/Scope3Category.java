package com.nsmm.esg.scope_service.entity;

/**
 * Scope 3 카테고리 열거형
 * 
 * GHG 프로토콜 기준 15개 Scope 3 카테고리 정의:
 * - 업스트림 카테고리 (1-8): 기업 운영 이전 단계 배출
 * - 다운스트림 카테고리 (9-15): 기업 운영 이후 단계 배출
 * 
 * 각 카테고리는 고유 번호와 한국어 명칭을 포함
 * 
 * @author ESG Project Team
 * @version 1.0
 */
public enum Scope3Category {

  // ========================================================================
  // 업스트림 카테고리 (Upstream Categories) 1-8
  // ========================================================================

  PURCHASED_GOODS_SERVICES(1, "구매한 상품 및 서비스"),
  CAPITAL_GOODS(2, "자본재"),
  FUEL_ENERGY_ACTIVITIES(3, "연료 및 에너지 관련 활동"),
  UPSTREAM_TRANSPORTATION(4, "업스트림 운송 및 유통"),
  WASTE_GENERATED(5, "폐기물 처리"),
  BUSINESS_TRAVEL(6, "사업장 관련 활동"),
  EMPLOYEE_COMMUTING(7, "직원 통근"),
  UPSTREAM_LEASED_ASSETS(8, "업스트림 임대 자산"),

  // ========================================================================
  // 다운스트림 카테고리 (Downstream Categories) 9-15
  // ========================================================================

  DOWNSTREAM_TRANSPORTATION(9, "다운스트림 운송 및 유통"),
  PROCESSING_SOLD_PRODUCTS(10, "판매 후 처리"),
  USE_SOLD_PRODUCTS(11, "제품 사용"),
  END_OF_LIFE_SOLD_PRODUCTS(12, "제품 폐기"),
  DOWNSTREAM_LEASED_ASSETS(13, "다운스트림 임대 자산"),
  FRANCHISES(14, "프랜차이즈"),
  INVESTMENTS(15, "투자");

  private final int categoryNumber; // 카테고리 번호 (1-15)
  private final String categoryName; // 카테고리 한국어 명칭

  /**
   * Scope 3 카테고리 생성자
   */
  Scope3Category(int categoryNumber, String categoryName) {
    this.categoryNumber = categoryNumber;
    this.categoryName = categoryName;
  }

  /**
   * 카테고리 번호 반환
   */
  public int getCategoryNumber() {
    return categoryNumber;
  }

  /**
   * 카테고리 한국어 명칭 반환
   */
  public String getCategoryName() {
    return categoryName;
  }

  /**
   * 카테고리 번호로 카테고리 조회
   */
  public static Scope3Category fromCategoryNumber(int categoryNumber) {
    for (Scope3Category category : values()) {
      if (category.getCategoryNumber() == categoryNumber) {
        return category;
      }
    }
    throw new IllegalArgumentException("유효하지 않은 Scope 3 카테고리 번호: " + categoryNumber);
  }

  /**
   * 업스트림 카테고리 여부 확인
   */
  public boolean isUpstream() {
    return categoryNumber >= 1 && categoryNumber <= 8;
  }

  /**
   * 다운스트림 카테고리 여부 확인
   */
  public boolean isDownstream() {
    return categoryNumber >= 9 && categoryNumber <= 15;
  }
}