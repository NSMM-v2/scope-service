package com.nsmm.esg.scope_service.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.nsmm.esg.scope_service.dto.ApiResponse;
import com.nsmm.esg.scope_service.dto.response.ScopeAggregationResponse;
import com.nsmm.esg.scope_service.dto.response.ProductEmissionSummary;
import com.nsmm.esg.scope_service.dto.response.HierarchicalEmissionSummary;
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
 * @author ESG Project Team
 * @version 1.0
 * @since 2024
 */
@RestController
@RequestMapping("/api/v1/scope/aggregation")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Scope 배출량 집계 API", description = "Scope 1,2,3 배출량의 계층적 집계 및 특수 집계 기능을 제공합니다")
public class ScopeAggregationController {

  private final ScopeAggregationService scopeAggregationService;

  /**
   * 종합 집계 결과 조회 (모든 집계 로직 포함)
   * 기본 Scope별 합계 + Scope3 카테고리별 특수 집계 + 제품별 집계 + 계층별 집계
   */
  @Operation(summary = "종합 집계 결과 조회", description = "모든 집계 로직이 포함된 종합 결과를 제공합니다. " +
      "기본 Scope별 합계, Scope3 특수 집계, 제품별 집계, 계층별 집계가 포함됩니다.")
  @GetMapping("/comprehensive/{year}/{month}")
  public ResponseEntity<ApiResponse<ScopeAggregationResponse>> getComprehensiveAggregation(
      @Parameter(description = "보고 연도", example = "2024") @PathVariable Integer year,
      @Parameter(description = "보고 월", example = "12") @PathVariable Integer month,
      @Parameter(description = "본사 ID", example = "1") @RequestHeader("X-HEADQUARTERS-ID") String headquartersId) {

    try {
      log.info("종합 집계 요청 - 본사ID: {}, 연도: {}, 월: {}", headquartersId, year, month);

      ScopeAggregationResponse response = scopeAggregationService
          .getComprehensiveAggregation(Long.parseLong(headquartersId), year, month);

      log.info("종합 집계 완료 - 본사ID: {}, 결과: {}", headquartersId, response != null ? "성공" : "데이터 없음");

      return ResponseEntity.ok(ApiResponse.success(response, "종합 집계 결과가 성공적으로 조회되었습니다"));

    } catch (NumberFormatException e) {
      log.warn("잘못된 본사 ID 형식: {}", headquartersId);
      return ResponseEntity.badRequest()
          .body(ApiResponse.error("본사 ID는 숫자여야 합니다", "INVALID_HEADQUARTERS_ID"));
    } catch (Exception e) {
      log.error("종합 집계 중 오류 발생: {}", e.getMessage(), e);
      return ResponseEntity.internalServerError()
          .body(ApiResponse.error("집계 처리 중 오류가 발생했습니다", "AGGREGATION_ERROR"));
    }
  }

  /**
   * 제품별 배출량 집계
   * company_product_code 기준으로 제품별 총 배출량 집계
   */
  @Operation(summary = "제품별 배출량 집계", description = "company_product_code 기준으로 제품별 Scope 1,2,3 배출량을 집계합니다.")
  @GetMapping("/product/{year}/{month}")
  public ResponseEntity<ApiResponse<List<ProductEmissionSummary>>> getProductAggregation(
      @Parameter(description = "보고 연도", example = "2024") @PathVariable Integer year,
      @Parameter(description = "보고 월", example = "12") @PathVariable Integer month,
      @Parameter(description = "본사 ID", example = "1") @RequestHeader("X-HEADQUARTERS-ID") String headquartersId) {

    try {
      log.info("제품별 집계 요청 - 본사ID: {}, 연도: {}, 월: {}", headquartersId, year, month);

      List<ProductEmissionSummary> response = scopeAggregationService
          .getProductEmissionSummary(Long.parseLong(headquartersId), year, month);

      log.info("제품별 집계 완료 - 본사ID: {}, 제품 수: {}", headquartersId, response.size());

      return ResponseEntity.ok(ApiResponse.success(response, "제품별 집계 결과가 성공적으로 조회되었습니다"));

    } catch (NumberFormatException e) {
      log.warn("잘못된 본사 ID 형식: {}", headquartersId);
      return ResponseEntity.badRequest()
          .body(ApiResponse.error("본사 ID는 숫자여야 합니다", "INVALID_HEADQUARTERS_ID"));
    } catch (Exception e) {
      log.error("제품별 집계 중 오류 발생: {}", e.getMessage(), e);
      return ResponseEntity.internalServerError()
          .body(ApiResponse.error("제품별 집계 처리 중 오류가 발생했습니다", "PRODUCT_AGGREGATION_ERROR"));
    }
  }

  /**
   * 계층적 집계 (tree_path 기반)
   * 하위 협력사의 배출량 데이터를 상위로 누적하여 집계
   */
  @Operation(summary = "계층적 배출량 집계", description = "tree_path를 기반으로 하위 협력사의 배출량을 상위로 누적하여 집계합니다.")
  @GetMapping("/hierarchical/{year}/{month}")
  public ResponseEntity<ApiResponse<List<HierarchicalEmissionSummary>>> getHierarchicalAggregation(
      @Parameter(description = "보고 연도", example = "2024") @PathVariable Integer year,
      @Parameter(description = "보고 월", example = "12") @PathVariable Integer month,
      @Parameter(description = "본사 ID", example = "1") @RequestHeader("X-HEADQUARTERS-ID") String headquartersId,
      @Parameter(description = "기준 계층 경로 (선택사항)", example = "/1/") @RequestParam(required = false) String baseTreePath) {

    try {
      log.info("계층적 집계 요청 - 본사ID: {}, 연도: {}, 월: {}, 기준경로: {}",
          headquartersId, year, month, baseTreePath);

      List<HierarchicalEmissionSummary> response = scopeAggregationService
          .getHierarchicalEmissionSummary(Long.parseLong(headquartersId), baseTreePath, year, month);

      log.info("계층적 집계 완료 - 본사ID: {}, 계층 수: {}", headquartersId, response.size());

      return ResponseEntity.ok(ApiResponse.success(response, "계층적 집계 결과가 성공적으로 조회되었습니다"));

    } catch (NumberFormatException e) {
      log.warn("잘못된 본사 ID 형식: {}", headquartersId);
      return ResponseEntity.badRequest()
          .body(ApiResponse.error("본사 ID는 숫자여야 합니다", "INVALID_HEADQUARTERS_ID"));
    } catch (Exception e) {
      log.error("계층적 집계 중 오류 발생: {}", e.getMessage(), e);
      return ResponseEntity.internalServerError()
          .body(ApiResponse.error("계층적 집계 처리 중 오류가 발생했습니다", "HIERARCHICAL_AGGREGATION_ERROR"));
    }
  }
}