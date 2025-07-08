package com.nsmm.esg.scope_service.enums;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Scope 1 카테고리 열거형 - 프론트엔드 list1-10 매핑
 */
public enum Scope1Category {
    // 고정연소 그룹 (list1-3)
    STATIONARY_COMBUSTION_1(1, "액체연료", "고정연소", "보일러 등 고정 설비"),
    STATIONARY_COMBUSTION_2(2, "가스연료", "고정연소", "발전기 등 고정 설비"),
    STATIONARY_COMBUSTION_3(3, "고체연료", "고정연소", "기타 고정 설비"),

    // 이동연소 그룹 (list4-6)
    MOBILE_COMBUSTION_1(4, "차량", "이동연소", "차량 연료 연소"),
    MOBILE_COMBUSTION_2(5, "항공기", "이동연소", "선박 연료 연소"),
    MOBILE_COMBUSTION_3(6, "선박", "이동연소", "항공기 연료 연소"),

    // 공정배출 그룹 (list7-8)
    PROCESS_EMISSIONS_1(7, "제조배출", "공정배출", "화학반응 배출"),
    PROCESS_EMISSIONS_2(8, "폐수처리", "공정배출", "물리적 변화 배출"),

    // 냉매누출 그룹 (list9-10)
    REFRIGERANT_LEAKAGE_1(9, "냉동/냉방/설비냉매", "냉매누출", "냉장/냉동 설비"),
    REFRIGERANT_LEAKAGE_2(10, "소화기 방출", "냉매누출", "에어컨 설비");

    private final int categoryNumber;
    private final String categoryName;
    private final String groupName;
    private final String description;

    Scope1Category(int categoryNumber, String categoryName, String groupName, String description) {
        this.categoryNumber = categoryNumber;
        this.categoryName = categoryName;
        this.groupName = groupName;
        this.description = description;
    }

    public int getCategoryNumber() {
        return categoryNumber;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public String getGroupName() {
        return groupName;
    }

    public String getDescription() {
        return description;
    }

    public static Scope1Category fromCategoryNumber(int categoryNumber) {
        for (Scope1Category category : values()) {
            if (category.getCategoryNumber() == categoryNumber) {
                return category;
            }
        }
        throw new IllegalArgumentException("유효하지 않은 Scope 1 카테고리 번호: " + categoryNumber);
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
     * 그룹별 카테고리 조회
     */
    public static List<Scope1Category> getByGroup(String groupName) {
        return Arrays.stream(values())
                .filter(category -> category.getGroupName().equals(groupName))
                .collect(Collectors.toList());
    }
}