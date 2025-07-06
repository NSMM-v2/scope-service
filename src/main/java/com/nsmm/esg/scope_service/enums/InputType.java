package com.nsmm.esg.scope_service.enums;

public enum InputType {
  MANUAL("수동 입력"),
  LCA("LCA 기반 입력");

  private final String description;

  InputType(String description) {
    this.description = description;
  }

  public String getDescription() {
    return description;
  }
}