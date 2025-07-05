package com.nsmm.esg.scope_service.controller;

import com.nsmm.esg.scope_service.dto.request.ScopeEmissionRequest;
import com.nsmm.esg.scope_service.dto.request.ScopeEmissionUpdateRequest;
import com.nsmm.esg.scope_service.dto.response.ApiResponse;
import com.nsmm.esg.scope_service.dto.response.ScopeEmissionResponse;
import com.nsmm.esg.scope_service.entity.ScopeType;
import com.nsmm.esg.scope_service.service.ScopeEmissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Scope 배출량 통합 REST API 컨트롤러
 * 
 * 특징:
 * - Scope 1, 2, 3 모든 타입 처리 (scopeType 파라미터로 구분)
 * - 제품 정보는 Scope 1, 2에서만 사용 (Scope 3은 null)
 * - TreePath 기반 권한 제어
 * - 사용자 직접 입력 기반
 * - CRUD 작업 완전 지원
 * 
 * @author ESG Project Team
 * @version 1.0
 */
@Tag(name = "ScopeEmission", description = "Scope 1/2/3 배출량 통합 관리 API")
@Slf4j
@RestController
@RequestMapping("/api/v1/scope-emissions")
@RequiredArgsConstructor
public class ScopeEmissionController {

  private final ScopeEmissionService scopeEmissionService;

  // ========================================================================
  // 통합 CRUD API (Core APIs)
  // ========================================================================

  /**
   * Scope 배출량 데이터 생성
   */
  @Operation(summary = "Scope 배출량 데이터 생성", description = "Scope 1, 2, 3 배출량 데이터를 신규로 등록합니다. scopeType 파라미터로 구분합니다.")
  @PostMapping
  public ResponseEntity<ApiResponse<ScopeEmissionResponse>> create(
      @Valid @RequestBody ScopeEmissionRequest request,
      @RequestHeader(value = "X-USER-TYPE", required = false) String userType,
      @RequestHeader(value = "X-HEADQUARTERS-ID", required = false) String headquartersId,
      @RequestHeader(value = "X-PARTNER-ID", required = false) String partnerId,
      @RequestHeader(value = "X-TREE-PATH", required = false) String treePath) {

    log.info("Scope 배출량 생성 요청: scopeType={}, categoryNumber={}, userType={}",
        request.getScopeType(), request.getCategoryNumber(), userType);

    try {
      ScopeEmissionResponse response = scopeEmissionService.create(
          request, userType, headquartersId, partnerId, treePath);

      return ResponseEntity.status(HttpStatus.CREATED)
          .body(ApiResponse.success(response, "배출량 데이터가 성공적으로 생성되었습니다."));
    } catch (IllegalArgumentException e) {
      log.error("배출량 생성 실패: {}", e.getMessage());
      return ResponseEntity.badRequest()
          .body(ApiResponse.error(e.getMessage(), "INVALID_REQUEST"));
    } catch (Exception e) {
      log.error("배출량 생성 중 오류 발생", e);
      return ResponseEntity.internalServerError()
          .body(ApiResponse.error("서버 내부 오류가 발생했습니다.", "INTERNAL_SERVER_ERROR"));
    }
  }

  /**
   * 배출량 데이터 단건 조회
   */
  @Operation(summary = "배출량 데이터 단건 조회", description = "ID로 특정 배출량 데이터를 조회합니다.")
  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<ScopeEmissionResponse>> getById(
      @Parameter(description = "배출량 ID", example = "1") @PathVariable Long id,
      @RequestHeader("X-USER-ID") String userId,
      @RequestHeader("X-USER-TYPE") String userType,
      @RequestHeader("X-TREE-PATH") String treePath) {

    log.info("배출량 단건 조회 요청: id={}, userType={}", id, userType);

    try {
      ScopeEmissionResponse response = scopeEmissionService.getById(id, userId, userType, treePath);
      return ResponseEntity.ok(ApiResponse.success(response, "배출량 데이터를 조회했습니다."));
    } catch (IllegalArgumentException e) {
      log.error("배출량 조회 실패: {}", e.getMessage());
      return ResponseEntity.badRequest()
          .body(ApiResponse.error(e.getMessage(), "INVALID_REQUEST"));
    } catch (Exception e) {
      log.error("배출량 조회 중 오류 발생", e);
      return ResponseEntity.internalServerError()
          .body(ApiResponse.error("배출량 데이터 조회 중 오류가 발생했습니다.", "FETCH_ERROR"));
    }
  }

  /**
   * 배출량 데이터 목록 조회 (조건별)
   */
  @Operation(summary = "배출량 데이터 목록 조회", description = "조건에 따라 배출량 데이터 목록을 조회합니다. scopeType, year, month, categoryNumber 등 파라미터로 필터링.")
  @GetMapping
  public ResponseEntity<ApiResponse<List<ScopeEmissionResponse>>> getList(
      @Parameter(description = "Scope 타입", example = "SCOPE1") @RequestParam(required = false) ScopeType scopeType,
      @Parameter(description = "보고 연도", example = "2024") @RequestParam(required = false) Integer year,
      @Parameter(description = "보고 월", example = "12") @RequestParam(required = false) Integer month,
      @Parameter(description = "카테고리 번호", example = "1") @RequestParam(required = false) Integer categoryNumber,
      @RequestHeader("X-USER-TYPE") String userType,
      @RequestHeader("X-HEADQUARTERS-ID") String headquartersId,
      @RequestHeader(value = "X-PARTNER-ID", required = false) String partnerId,
      @RequestHeader("X-TREE-PATH") String treePath) {

    log.info("배출량 목록 조회 요청: scopeType={}, year={}, month={}, categoryNumber={}, userType={}",
        scopeType, year, month, categoryNumber, userType);

    try {
      List<ScopeEmissionResponse> response = scopeEmissionService.getList(
          scopeType, year, month, categoryNumber, userType, headquartersId, partnerId, treePath);

      return ResponseEntity.ok(ApiResponse.success(response, "배출량 목록을 조회했습니다."));
    } catch (Exception e) {
      log.error("배출량 목록 조회 중 오류 발생", e);
      return ResponseEntity.internalServerError()
          .body(ApiResponse.error("배출량 목록 조회 중 오류가 발생했습니다.", "LIST_FETCH_ERROR"));
    }
  }

  /**
   * 배출량 데이터 페이징 조회
   */
  @Operation(summary = "배출량 데이터 페이징 조회", description = "페이징을 적용하여 배출량 데이터를 조회합니다.")
  @GetMapping("/page")
  public ResponseEntity<ApiResponse<Page<ScopeEmissionResponse>>> getPage(
      @Parameter(description = "Scope 타입", example = "SCOPE1") @RequestParam(required = false) ScopeType scopeType,
      @RequestHeader("X-USER-TYPE") String userType,
      @RequestHeader("X-HEADQUARTERS-ID") String headquartersId,
      @RequestHeader("X-TREE-PATH") String treePath,
      Pageable pageable) {

    log.info("배출량 페이징 조회 요청: scopeType={}, userType={}, page={}, size={}",
        scopeType, userType, pageable.getPageNumber(), pageable.getPageSize());

    try {
      Page<ScopeEmissionResponse> response = scopeEmissionService.getPage(
          scopeType, userType, headquartersId, treePath, pageable);

      return ResponseEntity.ok(ApiResponse.success(response, "배출량 페이징 조회를 완료했습니다."));
    } catch (Exception e) {
      log.error("배출량 페이징 조회 중 오류 발생", e);
      return ResponseEntity.internalServerError()
          .body(ApiResponse.error("배출량 페이징 조회 중 오류가 발생했습니다.", "PAGE_FETCH_ERROR"));
    }
  }

  /**
   * 배출량 데이터 수정
   */
  @Operation(summary = "배출량 데이터 수정", description = "기존 배출량 데이터를 수정합니다. scopeType에 따라 제품 정보 처리.")
  @PutMapping("/{id}")
  public ResponseEntity<ApiResponse<ScopeEmissionResponse>> update(
      @Parameter(description = "배출량 ID", example = "1") @PathVariable Long id,
      @Valid @RequestBody ScopeEmissionUpdateRequest request,
      @RequestHeader("X-USER-TYPE") String userType,
      @RequestHeader("X-HEADQUARTERS-ID") String headquartersId,
      @RequestHeader(value = "X-PARTNER-ID", required = false) String partnerId,
      @RequestHeader("X-TREE-PATH") String treePath) {

    log.info("배출량 수정 요청: id={}, userType={}", id, userType);

    try {
      ScopeEmissionResponse response = scopeEmissionService.update(
          id, request, userType, headquartersId, partnerId, treePath);

      return ResponseEntity.ok(ApiResponse.success(response, "배출량 데이터가 성공적으로 수정되었습니다."));
    } catch (IllegalArgumentException e) {
      log.error("배출량 수정 실패: {}", e.getMessage());
      return ResponseEntity.badRequest()
          .body(ApiResponse.error(e.getMessage(), "INVALID_REQUEST"));
    } catch (Exception e) {
      log.error("배출량 수정 중 오류 발생", e);
      return ResponseEntity.internalServerError()
          .body(ApiResponse.error("배출량 데이터 수정 중 오류가 발생했습니다.", "UPDATE_ERROR"));
    }
  }

  /**
   * 배출량 데이터 삭제
   */
  @Operation(summary = "배출량 데이터 삭제", description = "배출량 데이터를 삭제합니다.")
  @DeleteMapping("/{id}")
  public ResponseEntity<ApiResponse<String>> delete(
      @Parameter(description = "배출량 ID", example = "1") @PathVariable Long id,
      @RequestHeader("X-USER-TYPE") String userType,
      @RequestHeader("X-HEADQUARTERS-ID") String headquartersId,
      @RequestHeader(value = "X-PARTNER-ID", required = false) String partnerId,
      @RequestHeader("X-TREE-PATH") String treePath) {

    log.info("배출량 삭제 요청: id={}, userType={}", id, userType);

    try {
      scopeEmissionService.delete(id, userType, headquartersId, partnerId, treePath);
      return ResponseEntity.ok(ApiResponse.success("삭제 완료", "배출량 데이터가 성공적으로 삭제되었습니다."));
    } catch (IllegalArgumentException e) {
      log.error("배출량 삭제 실패: {}", e.getMessage());
      return ResponseEntity.badRequest()
          .body(ApiResponse.error(e.getMessage(), "INVALID_REQUEST"));
    } catch (Exception e) {
      log.error("배출량 삭제 중 오류 발생", e);
      return ResponseEntity.internalServerError()
          .body(ApiResponse.error("배출량 데이터 삭제 중 오류가 발생했습니다.", "DELETE_ERROR"));
    }
  }

  // ========================================================================
  // 집계 API (Aggregation APIs)
  // ========================================================================

  /**
   * 연도별 총 배출량 집계
   */
  @Operation(summary = "연도별 총 배출량 집계", description = "특정 연도의 모든 Scope 배출량을 집계합니다.")
  @GetMapping("/aggregation/year/{year}")
  public ResponseEntity<ApiResponse<Object>> getTotalEmissionByYear(
      @RequestHeader("X-HEADQUARTERS-ID") String headquartersId,
      @Parameter(description = "보고 연도", example = "2024") @PathVariable Integer year) {

    log.info("연도별 총 배출량 집계 요청: year={}, headquartersId={}", year, headquartersId);

    try {
      BigDecimal totalEmission = scopeEmissionService.getTotalEmissionByYear(
          Long.parseLong(headquartersId), year);

      return ResponseEntity.ok(ApiResponse.success(
          Map.of("year", year, "totalEmission", totalEmission),
          "연도별 총 배출량 집계를 완료했습니다."));
    } catch (Exception e) {
      log.error("연도별 총 배출량 집계 중 오류 발생", e);
      return ResponseEntity.internalServerError()
          .body(ApiResponse.error("연도별 총 배출량 집계 중 오류가 발생했습니다.", "AGGREGATION_ERROR"));
    }
  }

  /**
   * Scope 타입별 총 배출량 집계
   */
  @Operation(summary = "Scope 타입별 총 배출량 집계", description = "특정 연도의 Scope 타입별 배출량을 집계합니다.")
  @GetMapping("/aggregation/scope/{scopeType}/year/{year}")
  public ResponseEntity<ApiResponse<Object>> getTotalEmissionByScopeType(
      @RequestHeader("X-HEADQUARTERS-ID") String headquartersId,
      @Parameter(description = "Scope 타입", example = "SCOPE1") @PathVariable ScopeType scopeType,
      @Parameter(description = "보고 연도", example = "2024") @PathVariable Integer year) {

    log.info("Scope 타입별 총 배출량 집계 요청: scopeType={}, year={}, headquartersId={}",
        scopeType, year, headquartersId);

    try {
      BigDecimal totalEmission = scopeEmissionService.getTotalEmissionByScopeType(
          Long.parseLong(headquartersId), year, scopeType);

      return ResponseEntity.ok(ApiResponse.success(
          Map.of("scopeType", scopeType, "year", year, "totalEmission", totalEmission),
          "Scope 타입별 총 배출량 집계를 완료했습니다."));
    } catch (Exception e) {
      log.error("Scope 타입별 총 배출량 집계 중 오류 발생", e);
      return ResponseEntity.internalServerError()
          .body(ApiResponse.error("Scope 타입별 총 배출량 집계 중 오류가 발생했습니다.", "AGGREGATION_ERROR"));
    }
  }

  /**
   * TreePath 기반 하위 조직 배출량 집계
   */
  @Operation(summary = "TreePath 기반 하위 조직 배출량 집계", description = "특정 TreePath 하위의 모든 조직 배출량을 집계합니다.")
  @GetMapping("/aggregation/tree-path")
  public ResponseEntity<ApiResponse<Object>> getTotalEmissionByTreePath(
      @RequestHeader("X-TREE-PATH") String treePath,
      @Parameter(description = "보고 연도", example = "2024") @RequestParam Integer year,
      @Parameter(description = "보고 월", example = "12") @RequestParam Integer month) {

    log.info("TreePath 기반 하위 조직 배출량 집계 요청: treePath={}, year={}, month={}",
        treePath, year, month);

    try {
      BigDecimal totalEmission = scopeEmissionService.getTotalEmissionByTreePath(treePath, year, month);

      return ResponseEntity.ok(ApiResponse.success(
          Map.of("treePath", treePath, "year", year, "month", month, "totalEmission", totalEmission),
          "TreePath 기반 하위 조직 배출량 집계를 완료했습니다."));
    } catch (Exception e) {
      log.error("TreePath 기반 하위 조직 배출량 집계 중 오류 발생", e);
      return ResponseEntity.internalServerError()
          .body(ApiResponse.error("TreePath 기반 하위 조직 배출량 집계 중 오류가 발생했습니다.", "AGGREGATION_ERROR"));
    }
  }
}