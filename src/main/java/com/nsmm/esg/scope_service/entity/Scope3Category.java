package com.nsmm.esg.scope_service.entity;
/**
 * Scope 3 카테고리 열거형 - GHG 프로토콜 기준 15개 카테고리
 */
public enum Scope3Category {
  // 업스트림 카테고리 (1-8)
  PURCHASED_GOODS_SERVICES(1, "구매한 상품 및 서비스"),
  CAPITAL_GOODS(2, "자본재"),
  FUEL_ENERGY_ACTIVITIES(3, "연료 및 에너지 관련 활동"),
  UPSTREAM_TRANSPORTATION(4, "업스트림 운송 및 유통"),
  WASTE_GENERATED(5, "폐기물 처리"),
  BUSINESS_TRAVEL(6, "사업장 관련 활동"),
  EMPLOYEE_COMMUTING(7, "직원 통근"),
  UPSTREAM_LEASED_ASSETS(8, "업스트림 임대 자산"),

  // 다운스트림 카테고리 (9-15)
  DOWNSTREAM_TRANSPORTATION(9, "다운스트림 운송 및 유통"),
  PROCESSING_SOLD_PRODUCTS(10, "판매 후 처리"),
  USE_SOLD_PRODUCTS(11, "제품 사용"),
  END_OF_LIFE_SOLD_PRODUCTS(12, "제품 폐기"),
  DOWNSTREAM_LEASED_ASSETS(13, "다운스트림 임대 자산"),
  FRANCHISES(14, "프랜차이즈"),
  INVESTMENTS(15, "투자");

  private final int scope3CategoryNumber;
  private final String scope3CategoryName;

  Scope3Category(int scope3CategoryNumber, String scope3CategoryName) {
    this.scope3CategoryNumber = scope3CategoryNumber;
    this.scope3CategoryName = scope3CategoryName;
  }

  public int getScope3CategoryNumber() { return scope3CategoryNumber; }
  public String getScope3CategoryName() { return scope3CategoryName; }

  public static Scope3Category fromCategoryNumber(int scope3CategoryNumber) {
    for (Scope3Category category : values()) {
      if (category.getScope3CategoryNumber() == scope3CategoryNumber) {
        return category;
      }
    }
    throw new IllegalArgumentException("유효하지 않은 Scope 3 카테고리 번호: " + scope3CategoryNumber);
  }

  public boolean isUpstream() {
    return scope3CategoryNumber >= 1 && scope3CategoryNumber <= 8;
  }

  public boolean isDownstream() {
    return scope3CategoryNumber >= 9 && scope3CategoryNumber <= 15;
  }
}