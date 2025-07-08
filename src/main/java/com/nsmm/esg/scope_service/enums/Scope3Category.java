package com.nsmm.esg.scope_service.enums;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Scope 3 카테고리 열거형 - 프론트엔드 list1-15 매핑
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

  private final int categoryNumber;
  private final String categoryName;

  Scope3Category(int categoryNumber, String categoryName) {
    this.categoryNumber = categoryNumber;
    this.categoryName = categoryName;
  }

  public int getCategoryNumber() {
    return categoryNumber;
  }

  public String getCategoryName() {
    return categoryName;
  }

  public static Scope3Category fromCategoryNumber(int categoryNumber) {
    for (Scope3Category category : values()) {
      if (category.getCategoryNumber() == categoryNumber) {
        return category;
      }
    }
    throw new IllegalArgumentException("유효하지 않은 Scope 3 카테고리 번호: " + categoryNumber);
  }

  /**
   * 프론트엔드 list 키에서 카테고리 번호 변환
   */
  public static int getNumberFromListKey(String listKey) {
    return switch (listKey) {
      case "list1" -> 1;
      case "list2" -> 2;
      case "list3" -> 3;
      case "list4" -> 4;
      case "list5" -> 5;
      case "list6" -> 6;
      case "list7" -> 7;
      case "list8" -> 8;
      case "list9" -> 9;
      case "list10" -> 10;
      case "list11" -> 11;
      case "list12" -> 12;
      case "list13" -> 13;
      case "list14" -> 14;
      case "list15" -> 15;
      default -> throw new IllegalArgumentException("유효하지 않은 list 키: " + listKey);
    };
  }

  /**
   * 카테고리 번호에서 프론트엔드 list 키 변환
   */
  public static String getListKeyFromNumber(int categoryNumber) {
    return "list" + categoryNumber;
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

  /**
   * 업스트림 카테고리 목록 조회
   */
  public static List<Scope3Category> getUpstreamCategories() {
    return Arrays.stream(values())
        .filter(Scope3Category::isUpstream)
        .collect(Collectors.toList());
  }

  /**
   * 다운스트림 카테고리 목록 조회
   */
  public static List<Scope3Category> getDownstreamCategories() {
    return Arrays.stream(values())
        .filter(Scope3Category::isDownstream)
        .collect(Collectors.toList());
  }
}