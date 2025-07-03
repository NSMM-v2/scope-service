package com.nsmm.esg.scope_service.entity;
/**
 * Scope 2 카테고리 열거형 - 간접 온실가스 배출 (에너지)
 */
public enum Scope2Category {
    // scope 2 카테고리 수정 (11-12)
    INDIRECT_EMISSIONS(11, "전기", "구매한 전력 사용으로 인한 간접 배출"),
    ENERGY_USE(12, "스팀", "스팀, 냉난방 등 구매한 에너지 사용");

    private final int scope2CategoryNumber;
    private final String scope2CategoryName;
    private final String description;

    Scope2Category(int scope2CategoryNumber, String scope2CategoryName, String description) {
        this.scope2CategoryNumber = scope2CategoryNumber;
        this.scope2CategoryName = scope2CategoryName;
        this.description = description;
    }

    public int getScope2CategoryNumber() { return scope2CategoryNumber; }
    public String getScope2CategoryName() { return scope2CategoryName; }
    public String getDescription() { return description; }

    public static Scope2Category fromCategoryNumber(int scope2CategoryNumber) {
        for (Scope2Category category : values()) {
            if (category.getScope2CategoryNumber() == scope2CategoryNumber) {
                return category;
            }
        }
        throw new IllegalArgumentException("유효하지 않은 Scope 2 카테고리 번호: " + scope2CategoryNumber);
    }
}