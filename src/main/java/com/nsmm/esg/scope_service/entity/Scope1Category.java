package com.nsmm.esg.scope_service.entity;
/**
 * Scope 1 카테고리 열거형 - 직접 온실가스 배출
 */
public enum Scope1Category {
    STATIONARY_COMBUSTION(1, "고정연소", "보일러, 발전기 등 고정 설비에서의 연료 연소"),
    MOBILE_COMBUSTION(2, "이동연소", "차량, 선박, 항공기 등 이동 수단에서의 연료 연소"),
    PROCESS_EMISSIONS(3, "공정배출", "화학반응, 물리적 변화 등 산업공정에서 발생하는 배출"),
    REFRIGERANT_LEAKAGE(4, "냉매누출", "냉장, 냉동, 에어컨 등에서 냉매가스 누출");

    private final int scope3CategoryNumber;
    private final String categoryName;
    private final String description;

    Scope1Category(int categoryNumber, String categoryName, String description) {
        this.scope3CategoryNumber = categoryNumber;
        this.categoryName = categoryName;
        this.description = description;
    }

    public int getCategoryNumber() { return scope3CategoryNumber; }
    public String getCategoryName() { return categoryName; }
    public String getDescription() { return description; }

    public static Scope1Category fromCategoryNumber(int categoryNumber) {
        for (Scope1Category category : values()) {
            if (category.getCategoryNumber() == categoryNumber) {
                return category;
            }
        }
        throw new IllegalArgumentException("유효하지 않은 Scope 1 카테고리 번호: " + categoryNumber);
    }
}
