package com.nsmm.esg.scope_service.enums;

/**
 * Scope 2 카테고리 열거형 - 프론트엔드 list1-2 매핑
 */
public enum Scope2Category {
    ELECTRIC_POWER(1, "전력 사용"),
    STEAM_HEAT(2, "스팀/열");

    private final int categoryNumber;
    private final String categoryName;

    Scope2Category(int categoryNumber, String categoryName) {
        this.categoryNumber = categoryNumber;
        this.categoryName = categoryName;
    }

    public int getCategoryNumber() {
        return categoryNumber;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public static Scope2Category fromCategoryNumber(int categoryNumber) {
        for (Scope2Category category : values()) {
            if (category.getCategoryNumber() == categoryNumber) {
                return category;
            }
        }
        throw new IllegalArgumentException("유효하지 않은 Scope 2 카테고리 번호: " + categoryNumber);
    }

    /**
     * 프론트엔드 list 키에서 카테고리 번호 변환
     */
    public static int getNumberFromListKey(String listKey) {
        return switch (listKey) {
            case "list1" -> 1;
            case "list2" -> 2;
            default -> throw new IllegalArgumentException("유효하지 않은 list 키: " + listKey);
        };
    }

    /**
     * 카테고리 번호에서 프론트엔드 list 키 변환
     */
    public static String getListKeyFromNumber(int categoryNumber) {
        return "list" + categoryNumber;
    }
}