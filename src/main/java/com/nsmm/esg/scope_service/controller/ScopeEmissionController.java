package com.nsmm.esg.scope_service.controller;

import com.nsmm.esg.scope_service.dto.request.ScopeEmissionRequest;
import com.nsmm.esg.scope_service.dto.request.ScopeEmissionUpdateRequest;
import com.nsmm.esg.scope_service.dto.response.ScopeEmissionResponse;
import com.nsmm.esg.scope_service.dto.response.ScopeCategoryResponse;
import com.nsmm.esg.scope_service.dto.ApiResponse;
import com.nsmm.esg.scope_service.entity.ScopeType;
import com.nsmm.esg.scope_service.service.ScopeEmissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 통합 Scope 배출량 REST API 컨트롤러
 * 
 * 특징:
 * - Scope 1, 2, 3 통합 관리
 * - JWT 헤더 기반 인증
 * - 제품 코드 매핑 지원 (Scope 1, 2는 선택적)
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
 * @author ESG Project Team
 * @version 2.0
 */
@Tag(name = "ScopeEmission", description = "통합 Scope 배출량 관리 API (Scope 1, 2, 3)")
@Slf4j
@RestController
@RequestMapping("/api/v1/scope")
@RequiredArgsConstructor
public class ScopeEmissionController {

  private final ScopeEmissionService scopeEmissionService;

  /**
   * 헤더 정보 로깅 유틸리티 메서드
   */
  private void logHeaders(String methodName, String userType, String headquartersId, String partnerId,
      String treePath) {
    log.debug("=== {} 요청 헤더 정보 ===", methodName);
    log.debug("X-USER-TYPE: {}", userType);
    log.debug("X-HEADQUARTERS-ID: {}", headquartersId);
    log.debug("X-PARTNER-ID: {}", partnerId);
    log.debug("X-TREE-PATH: {}", treePath);
  }

  /**
   * 제품 코드 유효성 검증 (Scope 1, 2는 선택적)
   */
  private void validateProductCodeForScope(ScopeEmissionRequest request) {
    if (request.getScopeType() == ScopeType.SCOPE3) {
      // Scope 3는 제품 코드가 권장되지만 필수는 아님
      if (request.getCompanyProductCode() != null || request.getProductName() != null) {
        log.info("Scope 3 - 제품 코드 매핑 포함: productCode={}, productName={}",
            request.getCompanyProductCode(), request.getProductName());
      }
    } else {
      // Scope 1, 2는 제품 코드가 완전히 선택적
      if (request.getCompanyProductCode() != null || request.getProductName() != null) {
        log.info("Scope {} - 선택적 제품 코드 매핑: productCode={}, productName={}",
            request.getScopeType(), request.getCompanyProductCode(), request.getProductName());
      } else {
        log.info("Scope {} - 제품 코드 없이 진행", request.getScopeType());
      }
    }
  }

  // ========================================================================
  // 카테고리 조회 API (Category APIs)
  // ========================================================================

  /**
   * 모든 Scope 타입의 카테고리 목록 조회
   */
  @Operation(summary = "모든 Scope 카테고리 조회", description = "Scope 1, 2, 3의 모든 카테고리 목록을 조회합니다.")
  @GetMapping("/categories")
  public ResponseEntity<ApiResponse<Map<String, List<ScopeCategoryResponse>>>> getAllScopeCategories() {
    log.info("모든 Scope 카테고리 조회 요청");

    try {
      Map<String, List<ScopeCategoryResponse>> categories = Map.of(
          "scope1", ScopeCategoryResponse.getAllScope1Categories(),
          "scope2", ScopeCategoryResponse.getAllScope2Categories(),
          "scope3", ScopeCategoryResponse.getAllScope3Categories());

      return ResponseEntity.ok(ApiResponse.success(categories, "모든 Scope 카테고리를 조회했습니다."));
    } catch (Exception e) {
      log.error("카테고리 조회 중 오류 발생", e);
      return ResponseEntity.internalServerError()
          .body(ApiResponse.error("카테고리 조회 중 오류가 발생했습니다.", "CATEGORY_FETCH_ERROR"));
    }
  }

  /**
   * 특정 Scope 타입의 카테고리 목록 조회
   */
  @Operation(summary = "특정 Scope 카테고리 조회", description = "지정된 Scope 타입의 카테고리 목록을 조회합니다.")
  @GetMapping("/categories/{scopeType}")
  public ResponseEntity<ApiResponse<List<ScopeCategoryResponse>>> getCategoriesByScope(
      @Parameter(description = "Scope 타입", example = "SCOPE1") @PathVariable ScopeType scopeType) {

    log.info("Scope {} 카테고리 조회 요청", scopeType);

    try {
      List<ScopeCategoryResponse> categories = ScopeCategoryResponse.getAllCategoriesByScope(scopeType);
      return ResponseEntity.ok(ApiResponse.success(categories,
          String.format("%s 카테고리를 조회했습니다.", scopeType.getDescription())));
    } catch (Exception e) {
      log.error("Scope {} 카테고리 조회 중 오류 발생", scopeType, e);
      return ResponseEntity.internalServerError()
          .body(ApiResponse.error("카테고리 조회 중 오류가 발생했습니다.", "SCOPE_CATEGORY_FETCH_ERROR"));
    }
  }

  // ========================================================================
  // 생성 API (Creation APIs)
  // ========================================================================

  /**
   * 통합 Scope 배출량 데이터 생성
   */
  @Operation(summary = "Scope 배출량 데이터 생성", description = "Scope 1, 2, 3 배출량 데이터를 생성합니다. Scope 1, 2는 제품 코드 선택적, Scope 3는 권장.")
  @PostMapping("/emissions")
  public ResponseEntity<ApiResponse<ScopeEmissionResponse>> createScopeEmission(
      @Valid @RequestBody ScopeEmissionRequest request,
      @RequestHeader(value = "X-USER-TYPE", required = false) String userType,
      @RequestHeader(value = "X-HEADQUARTERS-ID", required = false) String headquartersId,
      @RequestHeader(value = "X-PARTNER-ID", required = false) String partnerId,
      @RequestHeader(value = "X-TREE-PATH", required = false) String treePath) {

    log.info("Scope {} 배출량 생성 요청: categoryNumber={}, productCode={}",
        request.getScopeType(), request.getActiveCategoryNumber(), request.getCompanyProductCode());
    logHeaders("Scope 배출량 생성", userType, headquartersId, partnerId, treePath);

    try {
      // 제품 코드 유효성 검증 (Scope 1, 2는 선택적)
      validateProductCodeForScope(request);

      ScopeEmissionResponse response = scopeEmissionService.createScopeEmission(
          request, userType, headquartersId, partnerId, treePath);

      return ResponseEntity.status(HttpStatus.CREATED)
          .body(ApiResponse.success(response,
              String.format("%s 배출량 데이터가 성공적으로 생성되었습니다.", request.getScopeType().getDescription())));
    } catch (IllegalArgumentException e) {
      log.error("Scope {} 배출량 생성 실패: {}", request.getScopeType(), e.getMessage());
      return ResponseEntity.badRequest()
          .body(ApiResponse.error(e.getMessage(), "INVALID_REQUEST"));
    } catch (Exception e) {
      log.error("Scope {} 배출량 생성 중 오류 발생", request.getScopeType(), e);
      return ResponseEntity.internalServerError()
          .body(ApiResponse.error("서버 내부 오류가 발생했습니다.", "INTERNAL_SERVER_ERROR"));
    }
  }

  // ========================================================================
  // 조회 API (Query APIs)
  // ========================================================================

  /**
   * 특정 배출량 데이터 조회
   */
  @Operation(summary = "Scope 배출량 단건 조회", description = "ID로 Scope 배출량 데이터를 조회합니다.")
  @GetMapping("/emissions/{id}")
  public ResponseEntity<ApiResponse<ScopeEmissionResponse>> getScopeEmissionById(
      @PathVariable Long id,
      @RequestHeader("X-ACCOUNT-NUMBER") String accountNumber,
      @RequestHeader("X-USER-TYPE") String userType,
      @RequestHeader("X-TREE-PATH") String treePath) {

    log.info("Scope 배출량 상세 조회 요청: id={}, accountNumber={}, userType={}", id, accountNumber, userType);
    logHeaders("Scope 배출량 상세 조회", userType, null, null, treePath);

    try {
      ScopeEmissionResponse response = scopeEmissionService.getScopeEmissionById(id, accountNumber, userType, treePath);
      return ResponseEntity.ok(ApiResponse.success(response, "Scope 배출량 데이터를 조회했습니다."));
    } catch (IllegalArgumentException e) {
      log.error("Scope 배출량 조회 실패: {}", e.getMessage());
      return ResponseEntity.badRequest()
          .body(ApiResponse.error(e.getMessage(), "INVALID_REQUEST"));
    } catch (Exception e) {
      log.error("Scope 배출량 조회 중 오류 발생", e);
      return ResponseEntity.internalServerError()
          .body(ApiResponse.error("배출량 데이터 조회 중 오류가 발생했습니다.", "DATA_FETCH_ERROR"));
    }
  }

  /**
   * Scope 타입별 배출량 데이터 조회
   */
  @Operation(summary = "Scope 타입별 배출량 조회", description = "특정 Scope 타입의 배출량 데이터를 조회합니다.")
  @GetMapping("/emissions/scope/{scopeType}")
  public ResponseEntity<ApiResponse<List<ScopeEmissionResponse>>> getEmissionsByScope(
      @PathVariable ScopeType scopeType,
      @RequestHeader(value = "X-USER-TYPE", required = false) String userType,
      @RequestHeader(value = "X-HEADQUARTERS-ID", required = false) String headquartersId,
      @RequestHeader(value = "X-PARTNER-ID", required = false) String partnerId,
      @RequestHeader(value = "X-TREE-PATH", required = false) String treePath) {

    log.info("Scope {} 배출량 조회 요청: userType={}", scopeType, userType);
    logHeaders("Scope 타입별 배출량 조회", userType, headquartersId, partnerId, treePath);

    try {
      List<ScopeEmissionResponse> response = scopeEmissionService.getEmissionsByScope(
          scopeType, userType, headquartersId, partnerId, treePath);
      return ResponseEntity.ok(ApiResponse.success(response,
          String.format("%s 배출량 데이터를 조회했습니다.", scopeType.getDescription())));
    } catch (IllegalArgumentException e) {
      log.error("Scope {} 배출량 조회 실패: {}", scopeType, e.getMessage());
      return ResponseEntity.badRequest()
          .body(ApiResponse.error(e.getMessage(), "INVALID_REQUEST"));
    } catch (Exception e) {
      log.error("Scope {} 배출량 조회 중 오류 발생", scopeType, e);
      return ResponseEntity.internalServerError()
          .body(ApiResponse.error("Scope 타입별 배출량 데이터 조회 중 오류가 발생했습니다.", "SCOPE_DATA_FETCH_ERROR"));
    }
  }

  /**
   * 연도/월별 배출량 데이터 조회 (모든 Scope)
   */
  @Operation(summary = "연도/월별 전체 배출량 조회", description = "선택된 연도/월의 모든 Scope 배출량 데이터를 조회합니다.")
  @GetMapping("/emissions/year/{year}/month/{month}")
  public ResponseEntity<ApiResponse<List<ScopeEmissionResponse>>> getEmissionsByYearAndMonth(
      @PathVariable Integer year,
      @PathVariable Integer month,
      @Parameter(description = "Scope 타입 필터 (선택적)") @RequestParam(value = "scopeType", required = false) ScopeType scopeType,
      @RequestHeader(value = "X-USER-TYPE", required = false) String userType,
      @RequestHeader(value = "X-HEADQUARTERS-ID", required = false) String headquartersId,
      @RequestHeader(value = "X-PARTNER-ID", required = false) String partnerId,
      @RequestHeader(value = "X-TREE-PATH", required = false) String treePath) {

    log.info("연도/월별 배출량 조회 요청: year={}, month={}, scopeType={}, userType={}",
        year, month, scopeType, userType);
    logHeaders("연도/월별 배출량 조회", userType, headquartersId, partnerId, treePath);

    try {
      List<ScopeEmissionResponse> response = scopeEmissionService.getEmissionsByYearAndMonth(
          year, month, scopeType, userType, headquartersId, partnerId, treePath);

      String message = scopeType != null
          ? String.format("%d년 %d월 %s 배출량 데이터를 조회했습니다.", year, month, scopeType.getDescription())
          : String.format("%d년 %d월 전체 Scope 배출량 데이터를 조회했습니다.", year, month);

      return ResponseEntity.ok(ApiResponse.success(response, message));
    } catch (IllegalArgumentException e) {
      log.error("연도/월별 배출량 조회 실패: {}", e.getMessage());
      return ResponseEntity.badRequest()
          .body(ApiResponse.error(e.getMessage(), "INVALID_REQUEST"));
    } catch (Exception e) {
      log.error("연도/월별 배출량 조회 중 오류 발생", e);
      return ResponseEntity.internalServerError()
          .body(ApiResponse.error("배출량 데이터 조회 중 오류가 발생했습니다.", "DATA_FETCH_ERROR"));
    }
  }

  /**
   * 연도/월/카테고리별 배출량 데이터 조회
   */
  @Operation(summary = "연도/월/카테고리별 배출량 조회", description = "선택된 연도/월의 특정 카테고리 배출량 데이터를 조회합니다.")
  @GetMapping("/emissions/year/{year}/month/{month}/scope/{scopeType}/category/{categoryNumber}")
  public ResponseEntity<ApiResponse<List<ScopeEmissionResponse>>> getEmissionsByYearAndMonthAndCategory(
      @PathVariable Integer year,
      @PathVariable Integer month,
      @PathVariable ScopeType scopeType,
      @PathVariable Integer categoryNumber,
      @RequestHeader(value = "X-USER-TYPE", required = false) String userType,
      @RequestHeader(value = "X-HEADQUARTERS-ID", required = false) String headquartersId,
      @RequestHeader(value = "X-PARTNER-ID", required = false) String partnerId,
      @RequestHeader(value = "X-TREE-PATH", required = false) String treePath) {

    log.info("연도/월/카테고리별 배출량 조회 요청: year={}, month={}, scope={}, category={}, userType={}",
        year, month, scopeType, categoryNumber, userType);
    logHeaders("연도/월/카테고리별 배출량 조회", userType, headquartersId, partnerId, treePath);

    try {
      List<ScopeEmissionResponse> response = scopeEmissionService.getEmissionsByYearAndMonthAndCategory(
          year, month, scopeType, categoryNumber, userType, headquartersId, partnerId, treePath);
      return ResponseEntity.ok(ApiResponse.success(response,
          String.format("%d년 %d월 %s 카테고리 %d번 배출량 데이터를 조회했습니다.",
              year, month, scopeType.getDescription(), categoryNumber)));
    } catch (IllegalArgumentException e) {
      log.error("연도/월/카테고리별 배출량 조회 실패: {}", e.getMessage());
      return ResponseEntity.badRequest()
          .body(ApiResponse.error(e.getMessage(), "INVALID_REQUEST"));
    } catch (Exception e) {
      log.error("연도/월/카테고리별 배출량 조회 중 오류 발생", e);
      return ResponseEntity.internalServerError()
          .body(ApiResponse.error("연도/월/카테고리별 배출량 데이터 조회 중 오류가 발생했습니다.", "DETAILED_DATA_FETCH_ERROR"));
    }
  }

  /**
   * 제품 코드별 배출량 데이터 조회 (Scope 1, 2, 3 모두 지원)
   */
  @Operation(summary = "제품 코드별 배출량 조회", description = "제품 코드로 배출량 데이터를 조회합니다. Scope 1, 2는 선택적 제품 코드 지원.")
  @GetMapping("/emissions/product/{productCode}")
  public ResponseEntity<ApiResponse<List<ScopeEmissionResponse>>> getEmissionsByProductCode(
      @PathVariable String productCode,
      @Parameter(description = "Scope 타입 필터 (선택적)") @RequestParam(value = "scopeType", required = false) ScopeType scopeType,
      @RequestHeader(value = "X-USER-TYPE", required = false) String userType,
      @RequestHeader(value = "X-HEADQUARTERS-ID", required = false) String headquartersId,
      @RequestHeader(value = "X-PARTNER-ID", required = false) String partnerId,
      @RequestHeader(value = "X-TREE-PATH", required = false) String treePath) {

    log.info("제품 코드별 배출량 조회 요청: productCode={}, scopeType={}, userType={}",
        productCode, scopeType, userType);
    logHeaders("제품 코드별 배출량 조회", userType, headquartersId, partnerId, treePath);

    try {
      List<ScopeEmissionResponse> response = scopeEmissionService.getEmissionsByProductCode(
          productCode, scopeType, userType, headquartersId, partnerId, treePath);

      String message = scopeType != null
          ? String.format("제품 코드 %s의 %s 배출량 데이터를 조회했습니다.", productCode, scopeType.getDescription())
          : String.format("제품 코드 %s의 전체 Scope 배출량 데이터를 조회했습니다.", productCode);

      return ResponseEntity.ok(ApiResponse.success(response, message));
    } catch (IllegalArgumentException e) {
      log.error("제품 코드별 배출량 조회 실패: {}", e.getMessage());
      return ResponseEntity.badRequest()
          .body(ApiResponse.error(e.getMessage(), "INVALID_REQUEST"));
    } catch (Exception e) {
      log.error("제품 코드별 배출량 조회 중 오류 발생", e);
      return ResponseEntity.internalServerError()
          .body(ApiResponse.error("제품 코드별 배출량 데이터 조회 중 오류가 발생했습니다.", "PRODUCT_DATA_FETCH_ERROR"));
    }
  }

  // ========================================================================
  // 집계 및 요약 API (Summary & Aggregation APIs)
  // ========================================================================

  /**
   * 연도/월별 Scope 타입별 총계 조회
   */
  @Operation(summary = "연도/월별 Scope 타입별 총계 조회", description = "선택된 연도/월의 각 Scope 타입별 총 배출량을 조회합니다.")
  @GetMapping("/emissions/summary/year/{year}/month/{month}")
  public ResponseEntity<ApiResponse<Map<String, BigDecimal>>> getScopeSummaryByYearAndMonth(
      @PathVariable Integer year,
      @PathVariable Integer month,
      @RequestHeader(value = "X-USER-TYPE", required = false) String userType,
      @RequestHeader(value = "X-HEADQUARTERS-ID", required = false) String headquartersId,
      @RequestHeader(value = "X-PARTNER-ID", required = false) String partnerId,
      @RequestHeader(value = "X-TREE-PATH", required = false) String treePath) {

    log.info("연도/월별 Scope 타입별 총계 조회 요청: year={}, month={}, userType={}", year, month, userType);
    logHeaders("연도/월별 Scope 타입별 총계 조회", userType, headquartersId, partnerId, treePath);

    try {
      Map<String, BigDecimal> response = scopeEmissionService.getScopeSummaryByYearAndMonth(
          year, month, userType, headquartersId, partnerId, treePath);
      return ResponseEntity.ok(ApiResponse.success(response,
          String.format("%d년 %d월 Scope 타입별 배출량 총계를 조회했습니다.", year, month)));
    } catch (IllegalArgumentException e) {
      log.error("연도/월별 Scope 타입별 총계 조회 실패: {}", e.getMessage());
      return ResponseEntity.badRequest()
          .body(ApiResponse.error(e.getMessage(), "INVALID_REQUEST"));
    } catch (Exception e) {
      log.error("연도/월별 Scope 타입별 총계 조회 중 오류 발생", e);
      return ResponseEntity.internalServerError()
          .body(ApiResponse.error("Scope 타입별 총계 조회 중 오류가 발생했습니다.", "SCOPE_SUMMARY_FETCH_ERROR"));
    }
  }

  /**
   * 특정 Scope의 카테고리별 총계 조회
   */
  @Operation(summary = "Scope 카테고리별 총계 조회", description = "특정 Scope의 카테고리별 총 배출량을 조회합니다.")
  @GetMapping("/emissions/summary/scope/{scopeType}/year/{year}/month/{month}")
  public ResponseEntity<ApiResponse<Map<Integer, BigDecimal>>> getCategorySummaryByScope(
      @PathVariable ScopeType scopeType,
      @PathVariable Integer year,
      @PathVariable Integer month,
      @RequestHeader(value = "X-USER-TYPE", required = false) String userType,
      @RequestHeader(value = "X-HEADQUARTERS-ID", required = false) String headquartersId,
      @RequestHeader(value = "X-PARTNER-ID", required = false) String partnerId,
      @RequestHeader(value = "X-TREE-PATH", required = false) String treePath) {

    log.info("Scope {} 카테고리별 총계 조회 요청: year={}, month={}, userType={}", scopeType, year, month, userType);
    logHeaders("Scope 카테고리별 총계 조회", userType, headquartersId, partnerId, treePath);

    try {
      Map<Integer, BigDecimal> response = scopeEmissionService.getCategorySummaryByScope(
          scopeType, year, month, userType, headquartersId, partnerId, treePath);
      return ResponseEntity.ok(ApiResponse.success(response,
          String.format("%d년 %d월 %s 카테고리별 배출량 총계를 조회했습니다.",
              year, month, scopeType.getDescription())));
    } catch (IllegalArgumentException e) {
      log.error("Scope {} 카테고리별 총계 조회 실패: {}", scopeType, e.getMessage());
      return ResponseEntity.badRequest()
          .body(ApiResponse.error(e.getMessage(), "INVALID_REQUEST"));
    } catch (Exception e) {
      log.error("Scope {} 카테고리별 총계 조회 중 오류 발생", scopeType, e);
      return ResponseEntity.internalServerError()
          .body(ApiResponse.error("카테고리별 총계 조회 중 오류가 발생했습니다.", "CATEGORY_SUMMARY_FETCH_ERROR"));
    }
  }

  // ========================================================================
  // 업데이트 API (Update APIs)
  // ========================================================================

  /**
   * Scope 배출량 데이터 수정
   */
  @Operation(summary = "Scope 배출량 데이터 수정", description = "ID로 Scope 배출량 데이터를 수정합니다. 제품 코드는 선택적 업데이트.")
  @PutMapping("/emissions/{id}")
  public ResponseEntity<ApiResponse<ScopeEmissionResponse>> updateScopeEmission(
      @PathVariable Long id,
      @Valid @RequestBody ScopeEmissionUpdateRequest request,
      @RequestHeader(value = "X-USER-TYPE", required = false) String userType,
      @RequestHeader(value = "X-HEADQUARTERS-ID", required = false) String headquartersId,
      @RequestHeader(value = "X-PARTNER-ID", required = false) String partnerId,
      @RequestHeader(value = "X-TREE-PATH", required = false) String treePath) {

    log.info("Scope 배출량 업데이트 요청: id={}, userType={}, productCode={}",
        id, userType, request.getCompanyProductCode());
    logHeaders("Scope 배출량 업데이트", userType, headquartersId, partnerId, treePath);

    try {
      // 제품 코드 업데이트 로깅
      if (request.getCompanyProductCode() != null || request.getProductName() != null) {
        log.info("제품 코드 정보 업데이트: productCode={}, productName={}",
            request.getCompanyProductCode(), request.getProductName());
      }

      ScopeEmissionResponse response = scopeEmissionService.updateScopeEmission(
          id, request, userType, headquartersId, partnerId, treePath);
      return ResponseEntity.ok(ApiResponse.success(response, "Scope 배출량 데이터를 수정했습니다."));
    } catch (IllegalArgumentException e) {
      log.error("Scope 배출량 업데이트 실패: {}", e.getMessage());
      return ResponseEntity.badRequest()
          .body(ApiResponse.error(e.getMessage(), "INVALID_UPDATE_REQUEST"));
    } catch (Exception e) {
      log.error("Scope 배출량 업데이트 중 오류 발생", e);
      return ResponseEntity.internalServerError()
          .body(ApiResponse.error("배출량 데이터 수정 중 오류가 발생했습니다.", "UPDATE_ERROR"));
    }
  }

  // ========================================================================
  // 삭제 API (Delete APIs)
  // ========================================================================

  /**
   * Scope 배출량 데이터 삭제
   */
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
      return ResponseEntity.badRequest()
          .body(ApiResponse.error(e.getMessage(), "INVALID_DELETE_REQUEST"));
    } catch (Exception e) {
      log.error("Scope 배출량 삭제 중 오류 발생", e);
      return ResponseEntity.internalServerError()
          .body(ApiResponse.error("배출량 데이터 삭제 중 오류가 발생했습니다.", "DELETE_ERROR"));
    }
  }
}