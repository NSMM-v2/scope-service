package com.nsmm.esg.scope_service.controller;

import com.nsmm.esg.scope_service.dto.request.Scope1EmissionRequest;
import com.nsmm.esg.scope_service.dto.response.Scope1EmissionResponse;
import com.nsmm.esg.scope_service.dto.request.Scope1EmissionUpdateRequest;
import com.nsmm.esg.scope_service.dto.ApiResponse;
import com.nsmm.esg.scope_service.service.Scope1EmissionService;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Scope 1 배출량 REST API 컨트롤러
 *
 * JWT 헤더 기반 인증:
 * - X-USER-ID: 사용자 UUID
 * - X-USER-TYPE: 사용자 타입 (HEADQUARTERS | PARTNER)
 * - X-TREE-PATH: 계층 경로
 * - X-HEADQUARTERS-ID: 소속 본사 ID
 *
 * @author ESG Project Team
 * @version 1.0
 */
@Tag(name = "Scope1Emission", description = "Scope 3 배출량 관리 API")
@Slf4j
@RestController
@RequestMapping("/api/v1/scope1")
@RequiredArgsConstructor
public class Scope1EmissionController {

    private final Scope1EmissionService scope1EmissionService;

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

    // ========================================================================
    // 생성 API (Creation APIs)
    // ========================================================================

    /**
     * Scope 3 배출량 데이터 생성
     */
    @Operation(summary = "Scope 3 배출량 데이터 생성", description = "Scope 3 배출량 데이터를 신규로 등록합니다. JWT 헤더 기반 인증 필요.")
    @PostMapping("/emissions")
    public ResponseEntity<ApiResponse<Scope1EmissionResponse>> createScope1Emission(
            @Valid @RequestBody Scope1EmissionRequest request,
            @RequestHeader(value = "X-USER-TYPE", required = false) String userType,
            @RequestHeader(value = "X-HEADQUARTERS-ID", required = false) String headquartersId,
            @RequestHeader(value = "X-PARTNER-ID", required = false) String partnerId,
            @RequestHeader(value = "X-TREE-PATH", required = false) String treePath) {

        log.info("Scope 3 배출량 생성 요청: userType={}, scope1CategoryNumber={}", userType, request.getScope1CategoryNumber());
        logHeaders("Scope 3 배출량 생성", userType, headquartersId, partnerId, treePath);

        try {
            Scope1EmissionResponse response = scope1EmissionService.createScope1Emission(
                    request, userType, headquartersId, partnerId, treePath);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(response, "Scope 3 배출량 데이터가 성공적으로 생성되었습니다."));
        } catch (IllegalArgumentException e) {
            log.error("Scope 3 배출량 생성 실패: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage(), "INVALID_REQUEST"));
        } catch (Exception e) {
            log.error("Scope 3 배출량 생성 중 오류 발생", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("서버 내부 오류가 발생했습니다.", "INTERNAL_SERVER_ERROR"));
        }
    }

    // ========================================================================
    // 조회 API (Query APIs)
    // ========================================================================

    /**
     * TreePath 기반 배출량 데이터 조회 (페이지네이션)
     * 본사(HEADQUARTERS): 본인 및 모든 하위 협력사 데이터 조회
     * 협력사(PARTNER): 본인 및 하위 조직 데이터만 조회
     */
    @Operation(summary = "Scope 3 배출량 목록 조회", description = "본사: 모든 하위 조직 데이터, 협력사: 본인 및 하위 데이터만 조회")
    @GetMapping("/emissions")
    public ResponseEntity<ApiResponse<Page<Scope1EmissionResponse>>> getScope1Emissions(
            @RequestHeader("X-USER-TYPE") String userType,
            @RequestHeader("X-TREE-PATH") String treePath,
            Pageable pageable) {

        log.info("Scope 3 배출량 조회 요청: userType={}, treePath={}", userType, treePath);
        logHeaders("Scope 3 배출량 조회", userType, null, null, treePath);

        try {
            // 본사: 자신의 TreePath로 시작하는 모든 데이터 조회
            // 협력사: 자신의 TreePath로 시작하는 데이터만 조회
            Page<Scope1EmissionResponse> response = scope1EmissionService.getScope1EmissionsByTreePath(treePath, pageable);
            return ResponseEntity.ok(ApiResponse.success(response, "Scope 3 배출량 목록을 조회했습니다."));
        } catch (Exception e) {
            log.error("Scope 3 배출량 조회 중 오류 발생", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("배출량 목록 조회 중 오류가 발생했습니다.", "LIST_FETCH_ERROR"));
        }
    }

    /**
     * 특정 배출량 데이터 조회
     */
    @Operation(summary = "Scope 3 배출량 단건 조회", description = "ID로 Scope 3 배출량 데이터를 조회합니다. TreePath 기반 권한 검증.")
    @GetMapping("/emissions/{id}")
    public ResponseEntity<ApiResponse<Scope1EmissionResponse>> getScope1EmissionById(
            @PathVariable Long id,
            @RequestHeader("X-USER-ID") String userId,
            @RequestHeader("X-USER-TYPE") String userType,
            @RequestHeader("X-TREE-PATH") String treePath) {

        log.info("Scope 3 배출량 상세 조회 요청: id={}, userId={}, userType={}", id, userId, userType);
        logHeaders("Scope 3 배출량 상세 조회", userType, null, null, treePath);

        try {
            Scope1EmissionResponse response = scope1EmissionService.getScope1EmissionById(id, userId, userType, treePath);
            return ResponseEntity.ok(ApiResponse.success(response, "Scope 3 배출량 데이터를 조회했습니다."));
        } catch (IllegalArgumentException e) {
            log.error("Scope 3 배출량 조회 실패: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage(), "INVALID_REQUEST"));
        } catch (Exception e) {
            log.error("Scope 3 배출량 조회 중 오류 발생", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("배출량 데이터 조회 중 오류가 발생했습니다.", "DATA_FETCH_ERROR"));
        }
    }

    /**
     * 회사별 배출량 데이터 조회
     */
    @Operation(summary = "Scope 3 배출량 회사별 조회", description = "회사 유형에 따라 본사/협력사 배출량 데이터를 조회합니다.")
    @GetMapping("/emissions/company")
    public ResponseEntity<ApiResponse<List<Scope1EmissionResponse>>> getScope1EmissionsByCompany(
            @RequestHeader(value = "X-USER-TYPE", required = false) String userType,
            @RequestHeader(value = "X-HEADQUARTERS-ID", required = false) String headquartersId,
            @RequestHeader(value = "X-PARTNER-ID", required = false) String partnerId,
            @RequestHeader(value = "X-TREE-PATH", required = false) String treePath) {

        log.info("회사별 Scope 3 배출량 조회 요청: userType={}", userType);
        logHeaders("회사별 Scope 3 배출량 조회", userType, headquartersId, partnerId, treePath);

        try {
            List<Scope1EmissionResponse> response = scope1EmissionService.getScope1EmissionsByCompany(
                    userType, headquartersId, partnerId, treePath);
            return ResponseEntity.ok(ApiResponse.success(response, "회사별 Scope 3 배출량 데이터를 조회했습니다."));
        } catch (IllegalArgumentException e) {
            log.error("회사별 Scope 3 배출량 조회 실패: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage(), "INVALID_REQUEST"));
        } catch (Exception e) {
            log.error("회사별 Scope 3 배출량 조회 중 오류 발생", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("회사별 배출량 데이터 조회 중 오류가 발생했습니다.", "COMPANY_DATA_FETCH_ERROR"));
        }
    }

    /**
     * 연도별 배출량 데이터 조회
     */
    @Operation(summary = "Scope 3 배출량 연도별 조회", description = "회사 유형과 연도로 Scope 3 배출량 데이터를 조회합니다.")
    @GetMapping("/emissions/year/{year}")
    public ResponseEntity<ApiResponse<List<Scope1EmissionResponse>>> getScope1EmissionsByYear(
            @PathVariable Integer year,
            @RequestHeader(value = "X-USER-TYPE", required = false) String userType,
            @RequestHeader(value = "X-HEADQUARTERS-ID", required = false) String headquartersId,
            @RequestHeader(value = "X-PARTNER-ID", required = false) String partnerId,
            @RequestHeader(value = "X-TREE-PATH", required = false) String treePath) {

        log.info("연도별 Scope 3 배출량 조회 요청: year={}, userType={}", year, userType);
        logHeaders("연도별 Scope 3 배출량 조회", userType, headquartersId, partnerId, treePath);

        try {
            List<Scope1EmissionResponse> response = scope1EmissionService.getScope1EmissionsByYear(
                    year, userType, headquartersId, partnerId, treePath);
            return ResponseEntity.ok(ApiResponse.success(response,
                    String.format("%d년도 Scope 3 배출량 데이터를 조회했습니다.", year)));
        } catch (IllegalArgumentException e) {
            log.error("연도별 Scope 3 배출량 조회 실패: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage(), "INVALID_REQUEST"));
        } catch (Exception e) {
            log.error("연도별 Scope 3 배출량 조회 중 오류 발생", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("연도별 배출량 데이터 조회 중 오류가 발생했습니다.", "YEARLY_DATA_FETCH_ERROR"));
        }
    }

    /**
     * 카테고리별 배출량 데이터 조회
     */
    @Operation(summary = "Scope 3 배출량 카테고리별 조회", description = "회사 유형과 카테고리 번호로 Scope 3 배출량 데이터를 조회합니다.")
    @GetMapping("/emissions/category/{scope1CategoryNumber}")
    public ResponseEntity<ApiResponse<List<Scope1EmissionResponse>>> getScope1EmissionsByCategory(
            @PathVariable Integer scope1CategoryNumber,
            @RequestHeader(value = "X-USER-TYPE", required = false) String userType,
            @RequestHeader(value = "X-HEADQUARTERS-ID", required = false) String headquartersId,
            @RequestHeader(value = "X-PARTNER-ID", required = false) String partnerId,
            @RequestHeader(value = "X-TREE-PATH", required = false) String treePath) {

        log.info("카테고리별 Scope 3 배출량 조회 요청: scope1CategoryNumber={}, userType={}", scope1CategoryNumber, userType);
        logHeaders("카테고리별 Scope 3 배출량 조회", userType, headquartersId, partnerId, treePath);

        try {
            List<Scope1EmissionResponse> response = scope1EmissionService.getScope1EmissionsByCategory(
                    scope1CategoryNumber, userType, headquartersId, partnerId, treePath);
            return ResponseEntity.ok(ApiResponse.success(response,
                    String.format("카테고리 %d번 Scope 3 배출량 데이터를 조회했습니다.", scope1CategoryNumber)));
        } catch (IllegalArgumentException e) {
            log.error("카테고리별 Scope 3 배출량 조회 실패: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage(), "INVALID_REQUEST"));
        } catch (Exception e) {
            log.error("카테고리별 Scope 3 배출량 조회 중 오류 발생", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("카테고리별 배출량 데이터 조회 중 오류가 발생했습니다.", "CATEGORY_DATA_FETCH_ERROR"));
        }
    }

    // ========================================================================
    // 프론트엔드 연동 API (Frontend Integration APIs)
    // ========================================================================

    /**
     * 연도/월별 전체 배출량 데이터 조회
     * 프론트엔드에서 연도/월 선택 시 해당 데이터 전체 조회
     */
    @Operation(summary = "연도/월별 전체 배출량 조회", description = "선택된 연도/월의 모든 카테고리 배출량 데이터를 조회합니다.")
    @GetMapping("/emissions/year/{year}/month/{month}")
    public ResponseEntity<ApiResponse<List<Scope1EmissionResponse>>> getScope1EmissionsByYearAndMonth(
            @PathVariable Integer year,
            @PathVariable Integer month,
            @RequestHeader(value = "X-USER-TYPE", required = false) String userType,
            @RequestHeader(value = "X-HEADQUARTERS-ID", required = false) String headquartersId,
            @RequestHeader(value = "X-PARTNER-ID", required = false) String partnerId,
            @RequestHeader(value = "X-TREE-PATH", required = false) String treePath) {

        log.info("연도/월별 Scope 3 배출량 조회 요청: year={}, month={}, userType={}", year, month, userType);
        logHeaders("연도/월별 배출량 조회", userType, headquartersId, partnerId, treePath);

        try {
            List<Scope1EmissionResponse> response = scope1EmissionService.getScope1EmissionsByYearAndMonth(
                    year, month, userType, headquartersId, partnerId, treePath);
            return ResponseEntity.ok(ApiResponse.success(response,
                    String.format("%d년 %d월 Scope 3 배출량 데이터를 조회했습니다.", year, month)));
        } catch (IllegalArgumentException e) {
            log.error("Scope 3 배출량 조회 실패: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage(), "INVALID_REQUEST"));
        } catch (Exception e) {
            log.error("연도/월별 Scope 3 배출량 조회 중 오류 발생", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("배출량 데이터 조회 중 오류가 발생했습니다.", "DATA_FETCH_ERROR"));
        }
    }

    /**
     * 연도/월/카테고리별 배출량 데이터 조회
     */
    @Operation(summary = "연도/월/카테고리별 배출량 조회", description = "선택된 연도/월의 특정 카테고리 배출량 데이터를 조회합니다.")
    @GetMapping("/emissions/year/{year}/month/{month}/category/{scope1CategoryNumber}")
    public ResponseEntity<ApiResponse<List<Scope1EmissionResponse>>> getScope1EmissionsByYearAndMonthAndCategory(
            @PathVariable Integer year,
            @PathVariable Integer month,
            @PathVariable Integer scope1CategoryNumber,
            @RequestHeader(value = "X-USER-TYPE", required = false) String userType,
            @RequestHeader(value = "X-HEADQUARTERS-ID", required = false) String headquartersId,
            @RequestHeader(value = "X-PARTNER-ID", required = false) String partnerId,
            @RequestHeader(value = "X-TREE-PATH", required = false) String treePath) {

        log.info("연도/월/카테고리별 Scope 3 배출량 조회 요청: year={}, month={}, category={}, userType={}",
                year, month, scope1CategoryNumber, userType);
        logHeaders("연도/월/카테고리별 Scope 3 배출량 조회", userType, headquartersId, partnerId, treePath);

        try {
            List<Scope1EmissionResponse> response = scope1EmissionService.getScope1EmissionsByYearAndMonthAndCategory(
                    year, month, scope1CategoryNumber, userType, headquartersId, partnerId, treePath);
            return ResponseEntity.ok(ApiResponse.success(response,
                    String.format("%d년 %d월 카테고리 %d번 배출량 데이터를 조회했습니다.", year, month, scope1CategoryNumber)));
        } catch (IllegalArgumentException e) {
            log.error("연도/월/카테고리별 Scope 3 배출량 조회 실패: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage(), "INVALID_REQUEST"));
        } catch (Exception e) {
            log.error("연도/월/카테고리별 Scope 3 배출량 조회 중 오류 발생", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("연도/월/카테고리별 배출량 데이터 조회 중 오류가 발생했습니다.", "DETAILED_DATA_FETCH_ERROR"));
        }
    }

    /**
     * 연도/월별 카테고리 총계 조회
     */
    @Operation(summary = "연도/월별 카테고리 총계 조회", description = "선택된 연도/월의 각 카테고리별 총 배출량을 조회합니다.")
    @GetMapping("/emissions/summary/year/{year}/month/{month}")
    public ResponseEntity<ApiResponse<Map<Integer, BigDecimal>>> getCategorySummaryByYearAndMonth(
            @PathVariable Integer year,
            @PathVariable Integer month,
            @RequestHeader(value = "X-USER-TYPE", required = false) String userType,
            @RequestHeader(value = "X-HEADQUARTERS-ID", required = false) String headquartersId,
            @RequestHeader(value = "X-PARTNER-ID", required = false) String partnerId,
            @RequestHeader(value = "X-TREE-PATH", required = false) String treePath) {

        log.info("연도/월별 카테고리 총계 조회 요청: year={}, month={}, userType={}", year, month, userType);
        logHeaders("연도/월별 카테고리 총계 조회", userType, headquartersId, partnerId, treePath);

        try {
            Map<Integer, BigDecimal> response = scope1EmissionService.getCategorySummaryByYearAndMonth(
                    year, month, userType, headquartersId, partnerId, treePath);
            return ResponseEntity.ok(ApiResponse.success(response,
                    String.format("%d년 %d월 카테고리별 배출량 총계를 조회했습니다.", year, month)));
        } catch (IllegalArgumentException e) {
            log.error("연도/월별 카테고리 총계 조회 실패: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage(), "INVALID_REQUEST"));
        } catch (Exception e) {
            log.error("연도/월별 카테고리 총계 조회 중 오류 발생", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("카테고리 총계 조회 중 오류가 발생했습니다.", "SUMMARY_FETCH_ERROR"));
        }
    }

    /**
     * 연도별 카테고리 총계 조회 (월 구분 없음)
     */
    @Operation(summary = "연도별 카테고리 총계 조회", description = "선택된 연도의 각 카테고리별 총 배출량을 조회합니다 (월 구분 없음).")
    @GetMapping("/emissions/summary/year/{year}")
    public ResponseEntity<ApiResponse<Map<Integer, BigDecimal>>> getCategorySummaryByYear(
            @PathVariable Integer year,
            @RequestHeader(value = "X-USER-TYPE", required = false) String userType,
            @RequestHeader(value = "X-HEADQUARTERS-ID", required = false) String headquartersId,
            @RequestHeader(value = "X-PARTNER-ID", required = false) String partnerId,
            @RequestHeader(value = "X-TREE-PATH", required = false) String treePath) {

        log.info("연도별 카테고리 총계 조회 요청: year={}, userType={}", year, userType);
        logHeaders("연도별 카테고리 총계 조회", userType, headquartersId, partnerId, treePath);

        try {
            Map<Integer, BigDecimal> response = scope1EmissionService.getCategorySummaryByYear(
                    year, userType, headquartersId, partnerId, treePath);
            return ResponseEntity.ok(ApiResponse.success(response,
                    String.format("%d년도 카테고리별 배출량 총계를 조회했습니다.", year)));
        } catch (IllegalArgumentException e) {
            log.error("연도별 카테고리 총계 조회 실패: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage(), "INVALID_REQUEST"));
        } catch (Exception e) {
            log.error("연도별 카테고리 총계 조회 중 오류 발생", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("연도별 카테고리 총계 조회 중 오류가 발생했습니다.", "YEARLY_SUMMARY_FETCH_ERROR"));
        }
    }

    // ========================================================================
    // 업데이트 API (Update APIs)
    // ========================================================================

    /**
     * Scope 3 배출량 데이터 수정
     */
    @Operation(summary = "Scope 3 배출량 데이터 수정", description = "ID로 Scope 3 배출량 데이터를 수정합니다. 권한 검증 포함.")
    @PutMapping("/emissions/{id}")
    public ResponseEntity<ApiResponse<Scope1EmissionResponse>> updateScope1Emission(
            @PathVariable Long id,
            @Valid @RequestBody Scope1EmissionUpdateRequest request,
            @RequestHeader(value = "X-USER-TYPE", required = false) String userType,
            @RequestHeader(value = "X-HEADQUARTERS-ID", required = false) String headquartersId,
            @RequestHeader(value = "X-PARTNER-ID", required = false) String partnerId,
            @RequestHeader(value = "X-TREE-PATH", required = false) String treePath) {

        log.info("Scope 3 배출량 업데이트 요청: id={}, userType={}", id, userType);
        logHeaders("Scope 3 배출량 업데이트", userType, headquartersId, partnerId, treePath);

        try {
            Scope1EmissionResponse response = scope1EmissionService.updateScope1Emission(
                    id, request, userType, headquartersId, partnerId, treePath);
            return ResponseEntity.ok(ApiResponse.success(response, "Scope 3 배출량 데이터를 수정했습니다."));
        } catch (IllegalArgumentException e) {
            log.error("Scope 3 배출량 업데이트 실패: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage(), "INVALID_UPDATE_REQUEST"));
        } catch (Exception e) {
            log.error("Scope 3 배출량 업데이트 중 오류 발생", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("배출량 데이터 수정 중 오류가 발생했습니다.", "UPDATE_ERROR"));
        }
    }

    // ========================================================================
    // 삭제 API (Delete APIs)
    // ========================================================================

    /**
     * Scope 3 배출량 데이터 삭제
     */
    @Operation(summary = "Scope 3 배출량 데이터 삭제", description = "ID로 Scope 3 배출량 데이터를 삭제합니다. 권한 검증 포함.")
    @DeleteMapping("/emissions/{id}")
    public ResponseEntity<ApiResponse<String>> deleteScope1Emission(
            @PathVariable Long id,
            @RequestHeader(value = "X-USER-TYPE", required = false) String userType,
            @RequestHeader(value = "X-HEADQUARTERS-ID", required = false) String headquartersId,
            @RequestHeader(value = "X-PARTNER-ID", required = false) String partnerId,
            @RequestHeader(value = "X-TREE-PATH", required = false) String treePath) {

        log.info("Scope 3 배출량 삭제 요청: id={}, userType={}", id, userType);
        logHeaders("Scope 3 배출량 삭제", userType, headquartersId, partnerId, treePath);

        try {
            scope1EmissionService.deleteScope1Emission(id, userType, headquartersId, partnerId, treePath);
            return ResponseEntity.ok(ApiResponse.success("삭제 완료", "Scope 3 배출량 데이터를 삭제했습니다."));
        } catch (IllegalArgumentException e) {
            log.error("Scope 3 배출량 삭제 실패: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage(), "INVALID_DELETE_REQUEST"));
        } catch (Exception e) {
            log.error("Scope 3 배출량 삭제 중 오류 발생", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("배출량 데이터 삭제 중 오류가 발생했습니다.", "DELETE_ERROR"));
        }
    }

    // ========================================================================
    // 집계 API (Aggregation APIs)
    // ========================================================================

    /**
     * 회사별 총 배출량 집계
     */
    @Operation(summary = "Scope 3 회사별 총 배출량 집계", description = "회사 유형과 연도로 Scope 3 총 배출량을 집계합니다.")
    @GetMapping("/emissions/total/company/{year}")
    public ResponseEntity<ApiResponse<BigDecimal>> getTotalEmissionByCompanyAndYear(
            @PathVariable Integer year,
            @RequestHeader(value = "X-USER-TYPE", required = false) String userType,
            @RequestHeader(value = "X-HEADQUARTERS-ID", required = false) String headquartersId,
            @RequestHeader(value = "X-PARTNER-ID", required = false) String partnerId,
            @RequestHeader(value = "X-TREE-PATH", required = false) String treePath) {

        log.info("회사별 총 배출량 집계 요청: userType={}, year={}", userType, year);
        logHeaders("회사별 총 배출량 집계", userType, headquartersId, partnerId, treePath);

        try {
            // TODO: 본사/협력사 구분에 따른 집계 로직 구현 필요
            BigDecimal totalEmission = BigDecimal.ZERO;
            return ResponseEntity.ok(ApiResponse.success(totalEmission,
                    String.format("%d년도 회사별 총 배출량을 집계했습니다.", year)));
        } catch (Exception e) {
            log.error("회사별 총 배출량 집계 중 오류 발생", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("회사별 총 배출량 집계 중 오류가 발생했습니다.", "COMPANY_TOTAL_ERROR"));
        }
    }

    /**
     * TreePath 기반 총 배출량 집계
     */
    @Operation(summary = "Scope 3 TreePath 기반 총 배출량 집계", description = "TreePath와 연도로 Scope 3 총 배출량을 집계합니다.")
    @GetMapping("/emissions/total/tree-path/{year}")
    public ResponseEntity<ApiResponse<BigDecimal>> getTotalEmissionByTreePathAndYear(
            @PathVariable Integer year,
            @RequestHeader(value = "X-USER-TYPE", required = false) String userType,
            @RequestHeader(value = "X-HEADQUARTERS-ID", required = false) String headquartersId,
            @RequestHeader(value = "X-PARTNER-ID", required = false) String partnerId,
            @RequestHeader(value = "X-TREE-PATH", required = false) String treePath) {

        log.info("TreePath 기반 총 배출량 집계 요청: userType={}, treePath={}, year={}", userType, treePath, year);
        logHeaders("TreePath 기반 총 배출량 집계", userType, headquartersId, partnerId, treePath);

        try {
            // TODO: 본사/협력사 구분에 따른 집계 로직 구현 필요
            BigDecimal totalEmission = BigDecimal.ZERO;
            return ResponseEntity.ok(ApiResponse.success(totalEmission,
                    String.format("%d년도 TreePath 기반 총 배출량을 집계했습니다.", year)));
        } catch (Exception e) {
            log.error("TreePath 기반 총 배출량 집계 중 오류 발생", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("TreePath 기반 총 배출량 집계 중 오류가 발생했습니다.", "TREEPATH_TOTAL_ERROR"));
        }
    }
}