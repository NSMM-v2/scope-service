package com.nsmm.esg.scope_service.entity;
/**
 * Scope 2 카테고리 열거형 - 간접 온실가스 배출 (에너지)
 */
public enum Scope2Category {
    INDIRECT_EMISSIONS(1, "간접배출", "구매한 전력 사용으로 인한 간접 배출"),
    ENERGY_USE(2, "에너지사용", "스팀, 냉난방 등 구매한 에너지 사용"),
    EXTERNAL_EMISSIONS(3, "외부배출", "외부에서 공급받는 기타 에너지원 사용");

    private final int categoryNumber;
    private final String categoryName;
    private final String description;

    Scope2Category(int categoryNumber, String categoryName, String description) {
        this.categoryNumber = categoryNumber;
        this.categoryName = categoryName;
        this.description = description;
    }

    public int getCategoryNumber() { return categoryNumber; }
    public String getCategoryName() { return categoryName; }
    public String getDescription() { return description; }

    public static Scope2Category fromCategoryNumber(int categoryNumber) {
        for (Scope2Category category : values()) {
            if (category.getCategoryNumber() == categoryNumber) {
                return category;
            }
        }
        throw new IllegalArgumentException("유효하지 않은 Scope 2 카테고리 번호: " + categoryNumber);
    }
}