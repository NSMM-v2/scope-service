package com.nsmm.esg.scope_service.entity;

public enum ScopeType {
    SCOPE1("직접 배출"),
    SCOPE2("간접 배출 - 에너지"),
    SCOPE3("기타 간접 배출");

    private final String description;

    ScopeType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}