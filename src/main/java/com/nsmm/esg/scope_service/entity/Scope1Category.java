package com.nsmm.esg.scope_service.entity;
/**
 * Scope 1 카테고리 열거형 - 직접 온실가스 배출
 */
public enum Scope1Category {
    // scope1에서 사용하는 카테고리로 변경 (1-10)
    LIQUID_FUEL(1, "액체 연료", "보일러, 발전기 등에서의 액체 연료 연소"),
    GAS_FUEL(2, "가스 연료", "가스 형태 연료의 연소 과정에서 발생"),
    SOLID_FUEL(3, "고체 연료", "석탄 등 고체 연료 사용 시 발생"),
    GROUND_TRANSPORTATION(4, "차량", "지상 운송 수단 연료 사용"),
    AIR_TRANSPORTATION(5, "항공기", "항공 운송 수단 연료 사용"),
    SEA_TRANSPORTATION(6, "선박", "해상 운송 수단 연료 사용"),
    MANUFACTURING_EMISSIONS(7, "제조 배출", "산업 공정 중 화학 반응 등에서 발생"),
    WASTEWATER_TREATMENT(8, "폐수 처리", "하·폐수 처리 과정에서 발생"),
    REFRIGERANT_EQUIPMENT(9, "냉동/냉방 설비 냉매", "냉동·냉방 장비의 냉매 누출"),
    EXTINGUISHER_RELEASE(10, "소화기 방출", "소화기 사용 시 가스 배출");

    private final int scope1CategoryNumber;
    private final String scope1CategoryName;
    private final String description;

    Scope1Category(int scope1CategoryNumber, String scope1CategoryName, String description) {
        this.scope1CategoryNumber = scope1CategoryNumber;
        this.scope1CategoryName = scope1CategoryName;
        this.description = description;
    }

    public int getScope1CategoryNumber() { return scope1CategoryNumber; }
    public String getScope1CategoryName() { return scope1CategoryName; }
    public String getDescription() { return description; }

    public static Scope1Category fromCategoryNumber(int scope1CategoryNumber) {
        for (Scope1Category category : values()) {
            if (category.getScope1CategoryNumber() == scope1CategoryNumber) {
                return category;
            }
        }
        throw new IllegalArgumentException("유효하지 않은 Scope 1 카테고리 번호: " + scope1CategoryNumber);
    }
}
