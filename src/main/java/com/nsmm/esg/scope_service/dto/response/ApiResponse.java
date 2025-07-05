package com.nsmm.esg.scope_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * API 표준 응답 래퍼
 * - 성공/실패 응답 구조 통일
 */
@Getter
@AllArgsConstructor
public class ApiResponse<T> {
  private boolean success;
  private T data;
  private String message;
  private String errorCode;

  public static <T> ApiResponse<T> success(T data, String message) {
    return new ApiResponse<>(true, data, message, null);
  }

  public static <T> ApiResponse<T> error(String message, String errorCode) {
    return new ApiResponse<>(false, null, message, errorCode);
  }
}