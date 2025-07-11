package com.nsmm.esg.scope_service.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nsmm.esg.scope_service.dto.ApiResponse;
import com.nsmm.esg.scope_service.dto.response.MonthlyEmissionSummary;
import com.nsmm.esg.scope_service.dto.response.CategoryYearlyEmission;
import com.nsmm.esg.scope_service.dto.response.CategoryMonthlyEmission;
import com.nsmm.esg.scope_service.enums.ScopeType;
import com.nsmm.esg.scope_service.service.ScopeAggregationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Scope 배출량 집계 API 컨트롤러
 * 
 * 주요 기능:
 * - 계층적 배출량 집계 (tree_path 기반)
 * - Scope 3 카테고리별 특수 집계 (Cat.1, Cat.2, Cat.4, Cat.5)
 * - 제품별 배출량 집계 (company_product_code 기준)
 * - 종합 집계 결과 제공
 * 
 */
@RestController
@RequestMapping("/api/v1/scope/aggregation")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Scope 배출량 집계 API", description = "Scope 1,2,3 배출량의 계층적 집계 및 특수 집계 기능을 제공합니다")
public class ScopeAggregationController {

  private final ScopeAggregationService scopeAggregationService;


  /**
   * 협력사 대시보드 배출량 조회
   * 협력사별 월별 배출량 집계
   * 지정된 협력사의 연도별 각 월(1월~현재월)의 Scope 1,2,3 배출량 총계 조회
   * 차트 및 테이블 데이터 표시용
   */
  @Operation(summary = "협력사별 월별 배출량 집계", description = "지정된 협력사의 연도별 각 월(1월~현재월)의 Scope 1,2,3 배출량 총계를 조회합니다. " +
      "차트 및 테이블 데이터 표시에 사용됩니다.")
  @GetMapping("/partner/{partnerId}/year/{year}/monthly-summary")
  public ResponseEntity<ApiResponse<List<MonthlyEmissionSummary>>> getPartnerMonthlyEmissionSummary(
      @Parameter(description = "협력사 ID", example = "2") @PathVariable Long partnerId,
      @Parameter(description = "보고 연도", example = "2024") @PathVariable Integer year,
      @Parameter(description = "본사 ID", example = "1") @RequestHeader("X-HEADQUARTERS-ID") String headquartersId,
      @Parameter(description = "사용자 타입", example = "HEADQUARTERS") @RequestHeader("X-USER-TYPE") String userType,
      @Parameter(description = "요청자 협력사 ID (협력사인 경우)", example = "2") @RequestHeader(value = "X-PARTNER-ID", required = false) String requestPartnerid,
      @Parameter(description = "트리 경로", example = "/1/L1-001/") @RequestHeader(value = "X-TREE-PATH", required = false) String treePath,
      @Parameter(description = "계층 레벨", example = "1") @RequestHeader(value = "X-LEVEL", required = false) String level) {

    try {
      log.info("협력사별 월별 집계 요청 - 대상협력사ID: {}, 본사ID: {}, 사용자타입: {}, 요청자협력사ID: {}, 연도: {}",
          partnerId, headquartersId, userType, requestPartnerid, year);

      List<MonthlyEmissionSummary> response = scopeAggregationService
          .getPartnerMonthlyEmissionSummary(
              partnerId,
              year,
              Long.parseLong(headquartersId),
              userType,
              requestPartnerid != null ? Long.parseLong(requestPartnerid) : null,
              treePath);

      log.info("협력사별 월별 집계 완료 - 대상협력사ID: {}, 월별 데이터 수: {}", partnerId, response.size());

      return ResponseEntity.ok(ApiResponse.success(response, "협력사별 월별 집계 결과가 성공적으로 조회되었습니다"));

    } catch (NumberFormatException e) {
      log.warn("잘못된 숫자 형식 - 본사ID: {}, 협력사ID: {}, 요청자협력사ID: {}, 레벨: {}", headquartersId, partnerId, requestPartnerid, level);
      return ResponseEntity.badRequest()
          .body(ApiResponse.error("ID 또는 레벨은 숫자여야 합니다", "INVALID_NUMERIC_FORMAT"));
    } catch (Exception e) {
      log.error("협력사별 월별 집계 중 오류 발생: {}", e.getMessage(), e);
      return ResponseEntity.internalServerError()
          .body(ApiResponse.error("협력사별 월별 집계 처리 중 오류가 발생했습니다", "PARTNER_MONTHLY_AGGREGATION_ERROR"));
    }
  }

  /**
   * 카테고리별 연간 배출량 집계
   * 지정된 Scope 타입의 카테고리별 연간 총 배출량을 로그인된 사용자 기준으로 조회
   */
  @Operation(summary = "카테고리별 연간 배출량 집계", description = "지정된 Scope 타입의 카테고리별 연간 총 배출량을 로그인된 사용자 기준으로 조회합니다. " +
      "사용자의 계층 위치에 따라 적절한 범위의 데이터만 집계합니다.")
  @GetMapping("/category/{scopeType}/year/{year}")
  public ResponseEntity<ApiResponse<List<CategoryYearlyEmission>>> getCategoryYearlyEmissions(
      @Parameter(description = "Scope 타입", example = "SCOPE1") @PathVariable ScopeType scopeType,
      @Parameter(description = "보고 연도", example = "2024") @PathVariable Integer year,
      @Parameter(description = "본사 ID", example = "1") @RequestHeader("X-HEADQUARTERS-ID") String headquartersId,
      @Parameter(description = "사용자 타입", example = "HEADQUARTERS") @RequestHeader("X-USER-TYPE") String userType,
      @Parameter(description = "협력사 ID (협력사인 경우)", example = "2") @RequestHeader(value = "X-PARTNER-ID", required = false) String partnerId,
      @Parameter(description = "트리 경로", example = "/1/L1-001/") @RequestHeader(value = "X-TREE-PATH", required = false) String treePath,
      @Parameter(description = "계층 레벨", example = "1") @RequestHeader(value = "X-LEVEL", required = false) String level) {

    try {
      log.info("카테고리별 연간 집계 요청 - Scope: {}, 연도: {}, 본사ID: {}, 사용자타입: {}, 협력사ID: {}", 
          scopeType, year, headquartersId, userType, partnerId);

      List<CategoryYearlyEmission> response = scopeAggregationService
          .getCategoryYearlyEmissions(
              scopeType,
              year,
              Long.parseLong(headquartersId), 
              userType, 
              partnerId != null ? Long.parseLong(partnerId) : null,
              treePath,
              level != null ? Integer.parseInt(level) : null);

      log.info("카테고리별 연간 집계 완료 - Scope: {}, 카테고리 수: {}", scopeType, response.size());

      return ResponseEntity.ok(ApiResponse.success(response, 
          String.format("%s 카테고리별 %d년 연간 배출량 집계가 성공적으로 조회되었습니다", scopeType.getDescription(), year)));

    } catch (NumberFormatException e) {
      log.warn("잘못된 숫자 형식 - 본사ID: {}, 협력사ID: {}, 레벨: {}", headquartersId, partnerId, level);
      return ResponseEntity.badRequest()
          .body(ApiResponse.error("ID 또는 레벨은 숫자여야 합니다", "INVALID_NUMERIC_FORMAT"));
    } catch (Exception e) {
      log.error("카테고리별 연간 집계 중 오류 발생: {}", e.getMessage(), e);
      return ResponseEntity.internalServerError()
          .body(ApiResponse.error("카테고리별 연간 집계 처리 중 오류가 발생했습니다", "CATEGORY_YEARLY_AGGREGATION_ERROR"));
    }
  }

  /**
   * 카테고리별 월간 배출량 집계
   * 지정된 Scope 타입의 카테고리별 월간 배출량을 로그인된 사용자 기준으로 조회 (연도의 모든 월)
   */
  @Operation(summary = "카테고리별 월간 배출량 집계", description = "지정된 Scope 타입의 카테고리별 월간 배출량을 로그인된 사용자 기준으로 조회합니다. " +
      "연도의 모든 월(1~12월) 데이터를 제공하며, 사용자의 계층 위치에 따라 적절한 범위의 데이터만 집계합니다.")
  @GetMapping("/category/{scopeType}/year/{year}/monthly")
  public ResponseEntity<ApiResponse<List<CategoryMonthlyEmission>>> getCategoryMonthlyEmissions(
      @Parameter(description = "Scope 타입", example = "SCOPE1") @PathVariable ScopeType scopeType,
      @Parameter(description = "보고 연도", example = "2024") @PathVariable Integer year,
      @Parameter(description = "본사 ID", example = "1") @RequestHeader("X-HEADQUARTERS-ID") String headquartersId,
      @Parameter(description = "사용자 타입", example = "HEADQUARTERS") @RequestHeader("X-USER-TYPE") String userType,
      @Parameter(description = "협력사 ID (협력사인 경우)", example = "2") @RequestHeader(value = "X-PARTNER-ID", required = false) String partnerId,
      @Parameter(description = "트리 경로", example = "/1/L1-001/") @RequestHeader(value = "X-TREE-PATH", required = false) String treePath,
      @Parameter(description = "계층 레벨", example = "1") @RequestHeader(value = "X-LEVEL", required = false) String level) {

    try {
      log.info("카테고리별 월간 집계 요청 - Scope: {}, 연도: {}, 본사ID: {}, 사용자타입: {}, 협력사ID: {}", 
          scopeType, year, headquartersId, userType, partnerId);

      List<CategoryMonthlyEmission> response = scopeAggregationService
          .getCategoryMonthlyEmissions(
              scopeType,
              year,
              Long.parseLong(headquartersId), 
              userType, 
              partnerId != null ? Long.parseLong(partnerId) : null,
              treePath,
              level != null ? Integer.parseInt(level) : null);

      log.info("카테고리별 월간 집계 완료 - Scope: {}, 데이터 수: {}", scopeType, response.size());

      return ResponseEntity.ok(ApiResponse.success(response, 
          String.format("%s 카테고리별 %d년 월간 배출량 집계가 성공적으로 조회되었습니다", scopeType.getDescription(), year)));

    } catch (NumberFormatException e) {
      log.warn("잘못된 숫자 형식 - 본사ID: {}, 협력사ID: {}, 레벨: {}", headquartersId, partnerId, level);
      return ResponseEntity.badRequest()
          .body(ApiResponse.error("ID 또는 레벨은 숫자여야 합니다", "INVALID_NUMERIC_FORMAT"));
    } catch (Exception e) {
      log.error("카테고리별 월간 집계 중 오류 발생: {}", e.getMessage(), e);
      return ResponseEntity.internalServerError()
          .body(ApiResponse.error("카테고리별 월간 집계 처리 중 오류가 발생했습니다", "CATEGORY_MONTHLY_AGGREGATION_ERROR"));
    }
  }

}