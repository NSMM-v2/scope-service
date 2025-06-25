package com.nsmm.esg.scope_service.config;

import com.nsmm.esg.scope_service.dto.ApiResponse;
import com.nsmm.esg.scope_service.enums.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 전역 예외 처리기
 * 
 * 기능: 모든 API 에러를 일관된 형식으로 처리하고 클라이언트에게 적절한 에러 응답 제공
 * 특징: 에러 코드 기반으로 프론트엔드에서 차별화된 에러 처리 가능
 * 로깅: 모든 에러는 로그에 기록하여 디버깅 및 모니터링 지원
 * 
 * @author ESG Project Team
 * @version 1.0
 * @since 2024
 */
@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // ================================================================
    // 유효성 검증 관련 예외 처리 (Validation Exceptions)
    // ================================================================

    /**
     * 요청 본문 유효성 검증 실패 처리
     * 주로 @Valid 어노테이션으로 검증되는 DTO의 필드 유효성 검증 실패 시 발생
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidationException(
            MethodArgumentNotValidException ex) {

        log.warn("유효성 검증 실패: {}", ex.getMessage());

        // 필드별 에러 메시지 수집
        List<String> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> String.format("%s: %s", error.getField(), error.getDefaultMessage()))
                .collect(Collectors.toList());

        String errorMessage = "입력 데이터 검증 실패: " + String.join(", ", fieldErrors);

        return ResponseEntity.badRequest()
                .body(ApiResponse.error(errorMessage, ErrorCode.VALIDATION_ERROR.getCode()));
    }

    /**
     * 제약 조건 위반 예외 처리
     * 주로 Bean Validation의 제약 조건 위반 시 발생
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Object>> handleConstraintViolationException(
            ConstraintViolationException ex) {

        log.warn("제약 조건 위반: {}", ex.getMessage());

        // 제약 조건 위반 메시지 수집
        List<String> violations = ex.getConstraintViolations()
                .stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toList());

        String errorMessage = "제약 조건 위반: " + String.join(", ", violations);

        return ResponseEntity.badRequest()
                .body(ApiResponse.error(errorMessage, ErrorCode.VALIDATION_ERROR.getCode()));
    }

    /**
     * 요청 본문 읽기 실패 처리
     * 주로 JSON 파싱 오류나 필드 타입 불일치 시 발생
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Object>> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException ex) {

        log.warn("요청 데이터 읽기 실패: {}", ex.getMessage());

        return ResponseEntity.badRequest()
                .body(ApiResponse.error("요청 데이터 형식이 올바르지 않습니다",
                        ErrorCode.VALIDATION_ERROR.getCode()));
    }

    /**
     * 메서드 인수 타입 불일치 처리
     * 주로 URL 경로 변수나 쿼리 파라미터의 타입 변환 실패 시 발생
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Object>> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException ex) {

        log.warn("메서드 인수 타입 불일치: {} = {}", ex.getName(), ex.getValue());

        String errorMessage = String.format("파라미터 '%s'의 값 '%s'이(가) 올바르지 않습니다",
                ex.getName(), ex.getValue());

        return ResponseEntity.badRequest()
                .body(ApiResponse.error(errorMessage, ErrorCode.VALIDATION_ERROR.getCode()));
    }

    /**
     * 필수 헤더 누락 처리
     * 주로 JWT 인증 헤더나 기타 필수 헤더가 누락된 경우 발생
     */
    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ApiResponse<Object>> handleMissingRequestHeaderException(
            MissingRequestHeaderException ex) {

        log.warn("필수 헤더 누락: {}", ex.getHeaderName());

        String errorMessage = String.format("필수 헤더가 누락되었습니다: %s", ex.getHeaderName());

        return ResponseEntity.badRequest()
                .body(ApiResponse.error(errorMessage, ErrorCode.HEADER_MISSING.getCode()));
    }

    // ================================================================
    // 비즈니스 로직 관련 예외 처리 (Business Logic Exceptions)
    // ================================================================

    /**
     * 비즈니스 로직 예외 처리
     * 주로 Service 계층에서 비즈니스 규칙 위반 시 발생
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Object>> handleIllegalArgumentException(
            IllegalArgumentException ex) {

        log.warn("비즈니스 로직 오류: {}", ex.getMessage());

        return ResponseEntity.badRequest()
                .body(ApiResponse.error(ex.getMessage(), ErrorCode.VALIDATION_ERROR.getCode()));
    }

    /**
     * 상태 오류 예외 처리
     * 주로 객체의 상태가 요청된 작업에 적합하지 않을 때 발생
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Object>> handleIllegalStateException(
            IllegalStateException ex) {

        log.warn("상태 오류: {}", ex.getMessage());

        return ResponseEntity.badRequest()
                .body(ApiResponse.error(ex.getMessage(), ErrorCode.VALIDATION_ERROR.getCode()));
    }

    // ================================================================
    // 권한 관련 예외 처리 (Security Exceptions)
    // ================================================================

    /**
     * 접근 권한 부족 예외 처리
     * 주로 Spring Security의 @PreAuthorize나 권한 검증 실패 시 발생
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Object>> handleAccessDeniedException(
            AccessDeniedException ex) {

        log.warn("접근 권한 부족: {}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("해당 작업을 수행할 권한이 없습니다",
                        ErrorCode.ACCESS_DENIED.getCode()));
    }

    // ================================================================
    // 데이터베이스 관련 예외 처리 (Database Exceptions)
    // ================================================================

    /**
     * 데이터 무결성 위반 예외 처리
     * 주로 UNIQUE 제약 조건이나 FK 제약 조건 위반 시 발생
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Object>> handleDataIntegrityViolationException(
            DataIntegrityViolationException ex) {

        log.error("데이터 무결성 위반: {}", ex.getMessage());

        // 중복 데이터 에러인지 확인
        if (ex.getMessage() != null && ex.getMessage().contains("Duplicate")) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("이미 존재하는 데이터입니다",
                            ErrorCode.DUPLICATE_EMISSION_DATA.getCode()));
        }

        return ResponseEntity.badRequest()
                .body(ApiResponse.error("데이터베이스 제약 조건을 위반했습니다",
                        ErrorCode.DATABASE_CONSTRAINT_VIOLATION.getCode()));
    }

    /**
     * SQL 무결성 제약 조건 위반 예외 처리
     * 주로 직접적인 SQL 제약 조건 위반 시 발생
     */
    @ExceptionHandler(SQLIntegrityConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Object>> handleSQLIntegrityConstraintViolationException(
            SQLIntegrityConstraintViolationException ex) {

        log.error("SQL 제약 조건 위반: {}", ex.getMessage());

        return ResponseEntity.badRequest()
                .body(ApiResponse.error("데이터베이스 제약 조건을 위반했습니다",
                        ErrorCode.FOREIGN_KEY_CONSTRAINT.getCode()));
    }

    // ================================================================
    // 런타임 및 일반 예외 처리 (Runtime and General Exceptions)
    // ================================================================

    /**
     * 런타임 예외 처리
     * 주로 예상치 못한 런타임 오류 발생 시
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Object>> handleRuntimeException(RuntimeException ex) {
        log.error("런타임 예외 발생", ex);

        return ResponseEntity.internalServerError()
                .body(ApiResponse.error("요청 처리 중 오류가 발생했습니다",
                        ErrorCode.INTERNAL_SERVER_ERROR.getCode()));
    }

    /**
     * 모든 예외의 최종 처리기
     * 위에서 처리되지 않은 모든 예외를 캐치
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGeneralException(Exception ex) {
        log.error("예상치 못한 오류 발생", ex);

        return ResponseEntity.internalServerError()
                .body(ApiResponse.error("서버 내부 오류가 발생했습니다",
                        ErrorCode.INTERNAL_SERVER_ERROR.getCode()));
    }

    // ================================================================
    // 커스텀 예외 처리를 위한 헬퍼 메서드들
    // ================================================================

    /**
     * 에러 코드와 메시지로 BadRequest 응답 생성
     * 
     * @param errorCode     에러 코드 enum
     * @param customMessage 커스텀 메시지 (null이면 기본 메시지 사용)
     * @return ResponseEntity<ApiResponse<Object>>
     */
    private ResponseEntity<ApiResponse<Object>> createBadRequestResponse(
            ErrorCode errorCode, String customMessage) {

        String message = customMessage != null ? customMessage : errorCode.getMessage();

        return ResponseEntity.badRequest()
                .body(ApiResponse.error(message, errorCode.getCode()));
    }

    /**
     * 에러 코드와 메시지로 Forbidden 응답 생성
     * 
     * @param errorCode     에러 코드 enum
     * @param customMessage 커스텀 메시지 (null이면 기본 메시지 사용)
     * @return ResponseEntity<ApiResponse<Object>>
     */
    private ResponseEntity<ApiResponse<Object>> createForbiddenResponse(
            ErrorCode errorCode, String customMessage) {

        String message = customMessage != null ? customMessage : errorCode.getMessage();

        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(message, errorCode.getCode()));
    }

    /**
     * 에러 코드와 메시지로 InternalServerError 응답 생성
     * 
     * @param errorCode     에러 코드 enum
     * @param customMessage 커스텀 메시지 (null이면 기본 메시지 사용)
     * @return ResponseEntity<ApiResponse<Object>>
     */
    private ResponseEntity<ApiResponse<Object>> createInternalServerErrorResponse(
            ErrorCode errorCode, String customMessage) {

        String message = customMessage != null ? customMessage : errorCode.getMessage();

        return ResponseEntity.internalServerError()
                .body(ApiResponse.error(message, errorCode.getCode()));
    }
}