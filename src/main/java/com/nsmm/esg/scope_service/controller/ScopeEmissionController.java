package com.nsmm.esg.scope_service.controller;

import com.nsmm.esg.scope_service.dto.request.ScopeEmissionRequest;
import com.nsmm.esg.scope_service.dto.request.ScopeEmissionUpdateRequest;
import com.nsmm.esg.scope_service.dto.response.ScopeEmissionResponse;
import com.nsmm.esg.scope_service.dto.ApiResponse;
import com.nsmm.esg.scope_service.enums.ScopeType;
import com.nsmm.esg.scope_service.enums.ErrorCode;
import com.nsmm.esg.scope_service.service.ScopeEmissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import java.util.List;

/**
 * 통합 Scope 배출량 REST API 컨트롤러
 * 
 * 특징:
 * - Scope 1, 2, 3 통합 관리
 * - JWT 헤더 기반 인증
 * - 자재 코드 매핑 지원 (Scope 1, 2는 선택적)
 * - 기존 Scope 3 API 호환성 유지
 * 
 * 게이트웨이 헤더:
 * - X-USER-TYPE: 사용자 타입 (HEADQUARTERS | PARTNER)
 * - X-COMPANY-NAME: 회사명
 * - X-ACCOUNT-NUMBER: 계정 번호 (HQ001, L1-001 등)
 * - X-HEADQUARTERS-ID: 본사 ID
 * - X-PARTNER-ID: 파트너사 ID (파트너사인 경우만)
 * - X-TREE-PATH: 계층 경로
 * - X-LEVEL: 계층 레벨
 * 
 */
@Tag(name = "ScopeEmission", description = "통합 Scope 배출량 관리 API (Scope 1, 2, 3)")
@Slf4j
@RestController
@RequestMapping("/api/v1/scope")
@RequiredArgsConstructor
public class ScopeEmissionController {

  private final ScopeEmissionService scopeEmissionService;

  // 헤더 정보 로깅 유틸리티 메서드
  private void logHeaders(String methodName, String userType, String headquartersId, String partnerId,
      String treePath) {
    log.debug("=== {} 요청 헤더 정보 ===", methodName);
    log.debug("X-USER-TYPE: {}", userType);
    log.debug("X-HEADQUARTERS-ID: {}", headquartersId);
    log.debug("X-PARTNER-ID: {}", partnerId);
    log.debug("X-TREE-PATH: {}", treePath);
  }

  // ========================================================================
  // 생성 API (Creation APIs)
  // ========================================================================

  // 통합 Scope 배출량 데이터 생성
  @Operation(summary = "Scope 배출량 데이터 생성", description = "Scope 1, 2, 3 배출량 데이터를 생성합니다. 자재 코드 매핑은 Scope 1, 2만 가능합니다.")
  @PostMapping("/emissions")
  public ResponseEntity<ApiResponse<ScopeEmissionResponse>> createScopeEmission(
      @Valid @RequestBody ScopeEmissionRequest request,
      @RequestHeader(value = "X-USER-TYPE", required = false) String userType,
      @RequestHeader(value = "X-HEADQUARTERS-ID", required = false) String headquartersId,
      @RequestHeader(value = "X-PARTNER-ID", required = false) String partnerId,
      @RequestHeader(value = "X-TREE-PATH", required = false) String treePath) {

    log.info("Scope {} 배출량 생성 요청: categoryNumber={}, inputType={}, hasMaterialMapping={}",
        request.getScopeType(), request.getActiveCategoryNumber(),
        request.getInputType(), request.getHasMaterialMapping());
    logHeaders("Scope 배출량 생성", userType, headquartersId, partnerId, treePath);

    try {

      ScopeEmissionResponse response = scopeEmissionService.createScopeEmission(
          request, userType, headquartersId, partnerId, treePath);

      if (response == null) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(ApiResponse.error("동일한 조건의 배출량 데이터가 이미 존재합니다",
                ErrorCode.DUPLICATE_EMISSION_DATA.getCode()));
      }

      return ResponseEntity.status(HttpStatus.CREATED)
          .body(ApiResponse.success(response, "Scope 배출량 데이터가 성공적으로 생성되었습니다"));

    } catch (IllegalArgumentException e) {
      log.error("Scope {} 배출량 생성 실패: {}", request.getScopeType(), e.getMessage());
      // 주요 예외 메시지별로 ErrorCode 매핑
      if (e.getMessage() != null && e.getMessage().contains("권한")) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(ApiResponse.error(e.getMessage(), ErrorCode.ACCESS_DENIED.getCode()));
      } else if (e.getMessage() != null && e.getMessage().contains("필수")) {
        return ResponseEntity.badRequest()
            .body(ApiResponse.error(e.getMessage(), ErrorCode.MISSING_REQUIRED_FIELD.getCode()));
      } else if (e.getMessage() != null && e.getMessage().contains("카테고리")) {
        return ResponseEntity.badRequest()
            .body(ApiResponse.error(e.getMessage(), ErrorCode.INVALID_CATEGORY_NUMBER.getCode()));
      } else if (e.getMessage() != null && e.getMessage().contains("배출계수")) {
        return ResponseEntity.badRequest()
            .body(ApiResponse.error(e.getMessage(), ErrorCode.INVALID_EMISSION_FACTOR.getCode()));
      } else if (e.getMessage() != null && e.getMessage().contains("활동량")) {
        return ResponseEntity.badRequest()
            .body(ApiResponse.error(e.getMessage(), ErrorCode.INVALID_ACTIVITY_AMOUNT.getCode()));
      } else if (e.getMessage() != null && e.getMessage().contains("총 배출량")) {
        return ResponseEntity.badRequest()
            .body(ApiResponse.error(e.getMessage(), ErrorCode.INVALID_TOTAL_EMISSION.getCode()));
      } else {
        return ResponseEntity.badRequest()
            .body(ApiResponse.error(e.getMessage(), ErrorCode.VALIDATION_ERROR.getCode()));
      }
    } catch (Exception e) {
      log.error("Scope {} 배출량 생성 중 서버 오류: {}", request.getScopeType(), e.getMessage());
      return ResponseEntity.internalServerError()
          .body(ApiResponse.error("서버 내부 오류가 발생했습니다.", ErrorCode.INTERNAL_SERVER_ERROR.getCode()));
    }
  }

  // ========================================================================
  // 조회 API (Query APIs)
  // ========================================================================

  // Scope 타입별 배출량 데이터 조회
  @Operation(summary = "Scope 타입별 배출량 조회", description = "특정 Scope 타입의 배출량 데이터를 조회합니다.")
  @GetMapping("/emissions/scope/{scopeType}")
  public ResponseEntity<ApiResponse<List<ScopeEmissionResponse>>> getEmissionsByScope(
      @PathVariable ScopeType scopeType,
      @RequestHeader("X-ACCOUNT-NUMBER") String accountNumber,
      @RequestHeader(value = "X-USER-TYPE", required = false) String userType,
      @RequestHeader(value = "X-HEADQUARTERS-ID", required = false) String headquartersId,
      @RequestHeader(value = "X-PARTNER-ID", required = false) String partnerId,
      @RequestHeader(value = "X-TREE-PATH", required = false) String treePath) {

    log.info("Scope {} 배출량 조회 요청: accountNumber={}, userType={}", scopeType, accountNumber, userType);
    logHeaders("Scope 타입별 배출량 조회", userType, headquartersId, partnerId, treePath);

    try {
      List<ScopeEmissionResponse> response = scopeEmissionService.getEmissionsByScope(
          scopeType, accountNumber, userType, headquartersId, partnerId, treePath);
      return ResponseEntity.ok(ApiResponse.success(response,
          String.format("%s 배출량 데이터를 조회했습니다.", scopeType.getDescription())));
    } catch (IllegalArgumentException e) {
      log.error("Scope {} 배출량 조회 실패: {}", scopeType, e.getMessage());
      if (e.getMessage() != null && e.getMessage().contains("권한")) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(ApiResponse.error(e.getMessage(), ErrorCode.ACCESS_DENIED.getCode()));
      } else if (e.getMessage() != null && e.getMessage().contains("찾을 수 없습니다")) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ApiResponse.error(e.getMessage(), ErrorCode.DATA_NOT_FOUND.getCode()));
      } else {
        return ResponseEntity.badRequest()
            .body(ApiResponse.error(e.getMessage(), ErrorCode.VALIDATION_ERROR.getCode()));
      }
    } catch (Exception e) {
      log.error("Scope {} 배출량 조회 중 서버 오류: {}", scopeType, e.getMessage());
      return ResponseEntity.internalServerError()
          .body(ApiResponse.error("서버 내부 오류가 발생했습니다.", ErrorCode.INTERNAL_SERVER_ERROR.getCode()));
    }
  }

  // ========================================================================
  // 업데이트 API (Update APIs)
  // ========================================================================

  // Scope 배출량 데이터 수정
  @Operation(summary = "Scope 배출량 데이터 수정", description = "ID로 Scope 배출량 데이터를 수정합니다. 자재 코드 매핑은 Scope 1, 2만 가능합니다.")
  @PutMapping("/emissions/{id}")
  public ResponseEntity<ApiResponse<ScopeEmissionResponse>> updateScopeEmission(
      @PathVariable Long id,
      @Valid @RequestBody ScopeEmissionUpdateRequest request,
      @RequestHeader(value = "X-USER-TYPE", required = false) String userType,
      @RequestHeader(value = "X-HEADQUARTERS-ID", required = false) String headquartersId,
      @RequestHeader(value = "X-PARTNER-ID", required = false) String partnerId,
      @RequestHeader(value = "X-TREE-PATH", required = false) String treePath) {

    log.info("Scope 배출량 업데이트 요청: id={}, userType={}, inputType={}, hasMaterialMapping={}",
        id, userType, request.getInputType(), request.getHasMaterialMapping());
    logHeaders("Scope 배출량 업데이트", userType, headquartersId, partnerId, treePath);

    try {
      // 자재 코드 매핑 로깅
      if (Boolean.TRUE.equals(request.getHasMaterialMapping())) {
        log.info("자재 코드 매핑 정보 업데이트: MaterialCode={}, MaterialName={}",
            request.getMaterialName(), request.getMaterialName());
      }

      ScopeEmissionResponse response = scopeEmissionService.updateScopeEmission(
          id, request, userType, headquartersId, partnerId, treePath);
      return ResponseEntity.ok(ApiResponse.success(response, "Scope 배출량 데이터를 수정했습니다."));

    } catch (IllegalArgumentException e) {
      log.error("Scope 배출량 업데이트 실패: {}", e.getMessage());
      if (e.getMessage() != null && e.getMessage().contains("권한")) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(ApiResponse.error(e.getMessage(), ErrorCode.ACCESS_DENIED.getCode()));
      } else if (e.getMessage() != null && e.getMessage().contains("동일한 조건의 배출량 데이터가 이미 존재합니다")) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(ApiResponse.error(e.getMessage(), ErrorCode.DUPLICATE_EMISSION_DATA.getCode()));
      } else if (e.getMessage() != null && e.getMessage().contains("찾을 수 없습니다")) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ApiResponse.error(e.getMessage(), ErrorCode.EMISSION_DATA_NOT_FOUND.getCode()));
      } else {
        return ResponseEntity.badRequest()
            .body(ApiResponse.error(e.getMessage(), ErrorCode.VALIDATION_ERROR.getCode()));
      }
    } catch (Exception e) {
      log.error("Scope 배출량 업데이트 중 서버 오류: {}", e.getMessage());
      return ResponseEntity.internalServerError()
          .body(ApiResponse.error("서버 내부 오류가 발생했습니다.", ErrorCode.INTERNAL_SERVER_ERROR.getCode()));
    }
  }

  // ========================================================================
  // 삭제 API (Delete APIs)
  // ========================================================================

  // Scope 배출량 데이터 삭제
  @Operation(summary = "Scope 배출량 데이터 삭제", description = "ID로 Scope 배출량 데이터를 삭제합니다.")
  @DeleteMapping("/emissions/{id}")
  public ResponseEntity<ApiResponse<String>> deleteScopeEmission(
      @PathVariable Long id,
      @RequestHeader(value = "X-USER-TYPE", required = false) String userType,
      @RequestHeader(value = "X-HEADQUARTERS-ID", required = false) String headquartersId,
      @RequestHeader(value = "X-PARTNER-ID", required = false) String partnerId,
      @RequestHeader(value = "X-TREE-PATH", required = false) String treePath) {

    log.info("Scope 배출량 삭제 요청: id={}, userType={}", id, userType);
    logHeaders("Scope 배출량 삭제", userType, headquartersId, partnerId, treePath);

  try {
      scopeEmissionService.deleteScopeEmission(id, userType, headquartersId, partnerId, treePath);
      return ResponseEntity.ok(ApiResponse.success("삭제 완료", "Scope 배출량 데이터를 삭제했습니다."));
    } catch (IllegalArgumentException e) {
      log.error("Scope 배출량 삭제 실패: {}", e.getMessage());
      if (e.getMessage() != null && e.getMessage().contains("권한")) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(ApiResponse.error(e.getMessage(), ErrorCode.ACCESS_DENIED.getCode()));
      } else if (e.getMessage() != null && e.getMessage().contains("찾을 수 없습니다")) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ApiResponse.error(e.getMessage(), ErrorCode.EMISSION_DATA_NOT_FOUND.getCode()));
      } else {
        return ResponseEntity.badRequest()
            .body(ApiResponse.error(e.getMessage(), ErrorCode.VALIDATION_ERROR.getCode()));
      }
    } catch (Exception e) {
      log.error("Scope 배출량 삭제 중 서버 오류: {}", e.getMessage());
      return ResponseEntity.internalServerError()
          .body(ApiResponse.error("서버 내부 오류가 발생했습니다.", ErrorCode.INTERNAL_SERVER_ERROR.getCode()));
    }
  }

}