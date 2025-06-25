package com.nsmm.esg.scope_service.enums;

/**
 * API 에러 코드 정의
 * 
 * 용도: 프론트엔드에서 에러 유형별로 다른 처리를 할 수 있도록 에러 코드 제공
 * 분류: 유효성 검증, 비즈니스 로직, 권한, 데이터 관련, 서버 오류
 * 
 * @author ESG Project Team
 * @version 1.0
 * @since 2024
 */
public enum ErrorCode {

  // ================================================================
  // 유효성 검증 에러 (Validation Errors)
  // ================================================================

  VALIDATION_ERROR("VALIDATION_ERROR", "입력 데이터가 올바르지 않습니다"),
  MISSING_REQUIRED_FIELD("MISSING_REQUIRED_FIELD", "필수 필드가 누락되었습니다"),
  INVALID_EMISSION_FACTOR("INVALID_EMISSION_FACTOR", "배출계수는 0보다 커야 합니다"),
  INVALID_ACTIVITY_AMOUNT("INVALID_ACTIVITY_AMOUNT", "활동량은 0보다 커야 합니다"),
  INVALID_TOTAL_EMISSION("INVALID_TOTAL_EMISSION", "총 배출량은 0보다 커야 합니다"),
  INVALID_UNIT("INVALID_UNIT", "단위 정보가 올바르지 않습니다"),
  INVALID_DATE_RANGE("INVALID_DATE_RANGE", "날짜 범위가 올바르지 않습니다"),

  // ================================================================
  // 비즈니스 로직 에러 (Business Logic Errors)
  // ================================================================

  DUPLICATE_EMISSION_DATA("DUPLICATE_EMISSION_DATA", "해당 기간에 이미 데이터가 존재합니다"),
  INVALID_REPORTING_PERIOD("INVALID_REPORTING_PERIOD", "유효하지 않은 보고 기간입니다"),
  INVALID_CATEGORY_NUMBER("INVALID_CATEGORY_NUMBER", "유효하지 않은 카테고리 번호입니다"),
  CALCULATION_ERROR("CALCULATION_ERROR", "배출량 계산 중 오류가 발생했습니다"),
  INVALID_COMPANY_TYPE("INVALID_COMPANY_TYPE", "유효하지 않은 회사 유형입니다"),
  TREE_PATH_MISMATCH("TREE_PATH_MISMATCH", "조직 계층 경로가 일치하지 않습니다"),

  // ================================================================
  // 권한 관련 에러 (Authorization Errors)
  // ================================================================

  ACCESS_DENIED("ACCESS_DENIED", "해당 작업을 수행할 권한이 없습니다"),
  UNAUTHORIZED("UNAUTHORIZED", "인증이 필요합니다"),
  INSUFFICIENT_PERMISSION("INSUFFICIENT_PERMISSION", "권한이 부족합니다"),
  INVALID_USER_TYPE("INVALID_USER_TYPE", "유효하지 않은 사용자 유형입니다"),
  HEADER_MISSING("HEADER_MISSING", "필수 헤더 정보가 누락되었습니다"),

  // ================================================================
  // 데이터 관련 에러 (Data Errors)
  // ================================================================

  DATA_NOT_FOUND("DATA_NOT_FOUND", "요청한 데이터를 찾을 수 없습니다"),
  EMISSION_DATA_NOT_FOUND("EMISSION_DATA_NOT_FOUND", "배출량 데이터를 찾을 수 없습니다"),
  COMPANY_NOT_FOUND("COMPANY_NOT_FOUND", "회사 정보를 찾을 수 없습니다"),
  CATEGORY_NOT_FOUND("CATEGORY_NOT_FOUND", "카테고리 정보를 찾을 수 없습니다"),

  // ================================================================
  // 데이터베이스 관련 에러 (Database Errors)
  // ================================================================

  DATABASE_CONNECTION_ERROR("DATABASE_CONNECTION_ERROR", "데이터베이스 연결 오류가 발생했습니다"),
  DATABASE_CONSTRAINT_VIOLATION("DATABASE_CONSTRAINT_VIOLATION", "데이터베이스 제약 조건 위반입니다"),
  FOREIGN_KEY_CONSTRAINT("FOREIGN_KEY_CONSTRAINT", "참조 무결성 제약 조건 위반입니다"),

  // ================================================================
  // 서버 오류 (Server Errors)
  // ================================================================

  INTERNAL_SERVER_ERROR("INTERNAL_SERVER_ERROR", "서버 내부 오류가 발생했습니다"),
  SERVICE_UNAVAILABLE("SERVICE_UNAVAILABLE", "서비스를 일시적으로 사용할 수 없습니다"),
  TIMEOUT_ERROR("TIMEOUT_ERROR", "요청 처리 시간이 초과되었습니다"),

  // ================================================================
  // 외부 API 관련 에러 (External API Errors)
  // ================================================================

  EXTERNAL_API_ERROR("EXTERNAL_API_ERROR", "외부 API 호출 중 오류가 발생했습니다"),
  AUTH_SERVICE_ERROR("AUTH_SERVICE_ERROR", "인증 서비스 오류가 발생했습니다"),
  CONFIG_SERVICE_ERROR("CONFIG_SERVICE_ERROR", "설정 서비스 오류가 발생했습니다");

  private final String code;
  private final String message;

  /**
   * ErrorCode 생성자
   * 
   * @param code    에러 코드 (프론트엔드에서 사용)
   * @param message 기본 에러 메시지 (사용자에게 표시)
   */
  ErrorCode(String code, String message) {
    this.code = code;
    this.message = message;
  }

  /**
   * 에러 코드 반환
   * 
   * @return 에러 코드 문자열
   */
  public String getCode() {
    return code;
  }

  /**
   * 기본 에러 메시지 반환
   * 
   * @return 에러 메시지
   */
  public String getMessage() {
    return message;
  }

  /**
   * 에러 코드 문자열로 ErrorCode 찾기
   * 
   * @param code 찾을 에러 코드
   * @return 해당하는 ErrorCode, 없으면 INTERNAL_SERVER_ERROR
   */
  public static ErrorCode fromCode(String code) {
    for (ErrorCode errorCode : values()) {
      if (errorCode.getCode().equals(code)) {
        return errorCode;
      }
    }
    return INTERNAL_SERVER_ERROR;
  }
}