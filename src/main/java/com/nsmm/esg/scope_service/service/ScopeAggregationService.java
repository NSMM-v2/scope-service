package com.nsmm.esg.scope_service.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.nsmm.esg.scope_service.repository.ScopeEmissionRepository;
import com.nsmm.esg.scope_service.dto.response.ScopeAggregationResponse;
import com.nsmm.esg.scope_service.dto.response.ProductEmissionSummary;
import com.nsmm.esg.scope_service.dto.response.HierarchicalEmissionSummary;
import com.nsmm.esg.scope_service.dto.response.AggregationDetails;
import com.nsmm.esg.scope_service.enums.ScopeType;

/**
 * Scope 배출량 집계 서비스
 * 
 * 주요 기능:
 * - 계층적 배출량 집계 (tree_path 기반)
 * - Scope 3 카테고리별 특수 집계 (Cat.1, Cat.2, Cat.4, Cat.5)
 * - 제품별 배출량 집계 (company_product_code 기준)
 * - 종합 집계 결과 제공
 * 
 * 집계 규칙:
 * - Cat.1: (Scope1 전체 - 이동연소 - 공장설비 - 폐수처리) + (Scope2 - 공장설비) + Scope3 Cat.1
 * - Cat.2: Scope1 공장설비 + Scope2 공장설비 + Scope3 Cat.2
 * - Cat.4: Scope1 이동연소 + Scope3 Cat.4
 * - Cat.5: Scope1 폐수처리 + Scope3 Cat.5
 * 
 * @author ESG Project Team
 * @version 1.0
 * @since 2024
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ScopeAggregationService {

  private final ScopeEmissionRepository scopeEmissionRepository;

  // ========================================================================
  // 종합 집계 메서드 (Comprehensive Aggregation)
  // ========================================================================

  /**
   * 종합 집계 결과 조회
   * 모든 집계 로직이 포함된 종합 결과 제공
   */
  @Transactional
  public ScopeAggregationResponse getComprehensiveAggregation(Long headquartersId, Integer year, Integer month) {
    log.info("종합 집계 시작 - 본사ID: {}, 연도: {}, 월: {}", headquartersId, year, month);

    try {
      // 기본 Scope별 합계 조회
      BigDecimal scope1Total = scopeEmissionRepository
          .sumTotalEmissionByScopeTypeAndYearAndMonthForHeadquarters(headquartersId, ScopeType.SCOPE1, year, month);
      BigDecimal scope2Total = scopeEmissionRepository
          .sumTotalEmissionByScopeTypeAndYearAndMonthForHeadquarters(headquartersId, ScopeType.SCOPE2, year, month);
      BigDecimal scope3Total = scopeEmissionRepository
          .sumTotalEmissionByScopeTypeAndYearAndMonthForHeadquarters(headquartersId, ScopeType.SCOPE3, year, month);

      // Scope 3 카테고리별 특수 집계
      BigDecimal scope3Cat1 = aggregateScope3Category1(headquartersId, year, month);
      BigDecimal scope3Cat2 = aggregateScope3Category2(headquartersId, year, month);
      BigDecimal scope3Cat4 = aggregateScope3Category4(headquartersId, year, month);
      BigDecimal scope3Cat5 = aggregateScope3Category5(headquartersId, year, month);

      // 제품별 집계
      List<ProductEmissionSummary> productSummaries = getProductEmissionSummary(headquartersId, year, month);

      // 계층별 집계 (본사 전체)
      List<HierarchicalEmissionSummary> hierarchicalSummaries = getHierarchicalEmissionSummary(headquartersId, "/",
          year, month);

      // 집계 상세 정보
      AggregationDetails details = createAggregationDetails(headquartersId, year, month);

      ScopeAggregationResponse response = ScopeAggregationResponse.builder()
          .reportingYear(year)
          .reportingMonth(month)
          .scope1Total(scope1Total)
          .scope2Total(scope2Total)
          .scope3Total(scope3Total)
          .totalEmission(scope1Total.add(scope2Total).add(scope3Total))
          .scope3Category1Aggregated(scope3Cat1)
          .scope3Category2Aggregated(scope3Cat2)
          .scope3Category4Aggregated(scope3Cat4)
          .scope3Category5Aggregated(scope3Cat5)
          .productSummaries(productSummaries)
          .hierarchicalSummaries(hierarchicalSummaries)
          .aggregationDetails(details)
          .build();

      log.info("종합 집계 완료 - 본사ID: {}, 총 배출량: {}", headquartersId, response.getTotalEmission());
      return response;

    } catch (Exception e) {
      log.error("종합 집계 중 오류 발생 - 본사ID: {}, 오류: {}", headquartersId, e.getMessage(), e);
      throw new RuntimeException("종합 집계 처리 중 오류가 발생했습니다", e);
    }
  }

  // ========================================================================
  // Scope 3 카테고리별 특수 집계 메서드 (Scope 3 Category Aggregation)
  // ========================================================================

  /**
   * Scope 3 Cat.1 집계
   * = (Scope1 전체 - 이동연소 - 공장설비 - 폐수처리) + (Scope2 - 공장설비) + Scope3 Cat.1
   */
  public BigDecimal aggregateScope3Category1(Long headquartersId, Integer year, Integer month) {
    log.debug("Scope 3 Cat.1 집계 시작 - 본사ID: {}", headquartersId);

    // Scope 1에서 제외할 그룹들 (이동연소, 공장설비, 폐수처리)
    BigDecimal scope1Excluded = scopeEmissionRepository.sumScope1EmissionExcludingGroups(
        headquartersId, List.of("이동연소", "공장설비", "공정배출"), year, month);

    // Scope 2에서 공장설비 관련 제외 (현재는 모든 Scope2가 공장설비 관련이므로 별도 처리)
    BigDecimal scope2Excluded = scopeEmissionRepository.sumScope2EmissionExcludingFactory(
        headquartersId, year, month);

    // Scope 3 Cat.1 배출량
    BigDecimal scope3Cat1 = scopeEmissionRepository.sumScope3EmissionByCategory(
        headquartersId, 1, year, month);

    BigDecimal result = scope1Excluded.add(scope2Excluded).add(scope3Cat1);
    log.debug("Scope 3 Cat.1 집계 완료 - 결과: {}", result);
    return result;
  }

  /**
   * Scope 3 Cat.2 집계
   * = Scope1 공장설비 + Scope2 공장설비 + Scope3 Cat.2
   */
  public BigDecimal aggregateScope3Category2(Long headquartersId, Integer year, Integer month) {
    log.debug("Scope 3 Cat.2 집계 시작 - 본사ID: {}", headquartersId);

    // Scope 1 공장설비 그룹
    BigDecimal scope1Factory = scopeEmissionRepository.sumScope1EmissionByGroup(
        headquartersId, "공장설비", year, month);

    // Scope 2 공장설비 (전체 Scope2가 공장설비 관련)
    BigDecimal scope2Factory = scopeEmissionRepository.sumScope2TotalEmission(
        headquartersId, year, month);

    // Scope 3 Cat.2
    BigDecimal scope3Cat2 = scopeEmissionRepository.sumScope3EmissionByCategory(
        headquartersId, 2, year, month);

    BigDecimal result = scope1Factory.add(scope2Factory).add(scope3Cat2);
    log.debug("Scope 3 Cat.2 집계 완료 - 결과: {}", result);
    return result;
  }

  /**
   * Scope 3 Cat.4 집계
   * = Scope1 이동연소 + Scope3 Cat.4
   */
  public BigDecimal aggregateScope3Category4(Long headquartersId, Integer year, Integer month) {
    log.debug("Scope 3 Cat.4 집계 시작 - 본사ID: {}", headquartersId);

    BigDecimal scope1Mobile = scopeEmissionRepository.sumScope1EmissionByGroup(
        headquartersId, "이동연소", year, month);
    BigDecimal scope3Cat4 = scopeEmissionRepository.sumScope3EmissionByCategory(
        headquartersId, 4, year, month);

    BigDecimal result = scope1Mobile.add(scope3Cat4);
    log.debug("Scope 3 Cat.4 집계 완료 - 결과: {}", result);
    return result;
  }

  /**
   * Scope 3 Cat.5 집계
   * = Scope1 폐수처리 + Scope3 Cat.5
   */
  public BigDecimal aggregateScope3Category5(Long headquartersId, Integer year, Integer month) {
    log.debug("Scope 3 Cat.5 집계 시작 - 본사ID: {}", headquartersId);

    BigDecimal scope1Waste = scopeEmissionRepository.sumScope1EmissionByGroup(
        headquartersId, "공정배출", year, month);
    BigDecimal scope3Cat5 = scopeEmissionRepository.sumScope3EmissionByCategory(
        headquartersId, 5, year, month);

    BigDecimal result = scope1Waste.add(scope3Cat5);
    log.debug("Scope 3 Cat.5 집계 완료 - 결과: {}", result);
    return result;
  }

  // ========================================================================
  // 제품별 집계 메서드 (Product Aggregation)
  // ========================================================================

  /**
   * 제품별 배출량 집계
   * company_product_code 기준으로 제품별 총 배출량 집계
   */
  @Transactional
  public List<ProductEmissionSummary> getProductEmissionSummary(Long headquartersId, Integer year, Integer month) {
    log.debug("제품별 집계 시작 - 본사ID: {}", headquartersId);

    List<Object[]> queryResults = scopeEmissionRepository.sumEmissionByProductCode(headquartersId, year, month);

    List<ProductEmissionSummary> result = queryResults.stream()
        .map(ProductEmissionSummary::from)
        .collect(Collectors.toList());

    log.debug("제품별 집계 완료 - 본사ID: {}, 제품 수: {}", headquartersId, result.size());
    return result;
  }

  // ========================================================================
  // 계층적 집계 메서드 (Hierarchical Aggregation)
  // ========================================================================

  /**
   * 계층적 배출량 집계
   * tree_path를 기반으로 하위 협력사의 배출량을 상위로 누적하여 집계
   */
  @Transactional
  public List<HierarchicalEmissionSummary> getHierarchicalEmissionSummary(
      Long headquartersId, String baseTreePath, Integer year, Integer month) {

    log.debug("계층적 집계 시작 - 본사ID: {}, 기준경로: {}", headquartersId, baseTreePath);

    // baseTreePath가 null이면 본사 전체를 대상으로 설정
    String treePath = (baseTreePath != null && !baseTreePath.trim().isEmpty()) ? baseTreePath : "/";

    List<Object[]> queryResults = scopeEmissionRepository.sumEmissionByTreePath(
        headquartersId, treePath, year, month);

    List<HierarchicalEmissionSummary> result = queryResults.stream()
        .map(this::convertToHierarchicalSummary)
        .collect(Collectors.toList());

    log.debug("계층적 집계 완료 - 본사ID: {}, 계층 수: {}", headquartersId, result.size());
    return result;
  }

  // ========================================================================
  // 유틸리티 메서드 (Utility Methods)
  // ========================================================================

  /**
   * 쿼리 결과를 HierarchicalEmissionSummary로 변환
   */
  private HierarchicalEmissionSummary convertToHierarchicalSummary(Object[] queryResult) {
    String treePath = (String) queryResult[0];
    BigDecimal scope1 = (BigDecimal) queryResult[1];
    BigDecimal scope2 = (BigDecimal) queryResult[2];
    BigDecimal scope3 = (BigDecimal) queryResult[3];
    BigDecimal total = scope1.add(scope2).add(scope3);

    // tree_path에서 계층 레벨 계산 (슬래시 개수 - 1)
    int level = (int) treePath.chars().filter(ch -> ch == '/').count() - 1;

    // 회사명은 tree_path의 마지막 부분에서 추출 (간단화)
    String companyName = extractCompanyNameFromTreePath(treePath);

    return HierarchicalEmissionSummary.builder()
        .treePath(treePath)
        .companyName(companyName)
        .level(level)
        .scope1Emission(scope1)
        .scope2Emission(scope2)
        .scope3Emission(scope3)
        .totalEmission(total)
        .childCount(0) // 필요 시 별도 쿼리로 조회
        .build();
  }

  /**
   * tree_path에서 회사명 추출 (간단화된 버전)
   */
  private String extractCompanyNameFromTreePath(String treePath) {
    if (treePath == null || treePath.equals("/")) {
      return "본사";
    }

    String[] parts = treePath.split("/");
    if (parts.length > 1) {
      String lastPart = parts[parts.length - 1];
      return lastPart.isEmpty() ? (parts.length > 2 ? parts[parts.length - 2] : "본사") : lastPart;
    }

    return "알 수 없음";
  }

  /**
   * 집계 상세 정보 생성
   */
  private AggregationDetails createAggregationDetails(Long headquartersId, Integer year, Integer month) {
    log.debug("집계 상세 정보 생성 시작 - 본사ID: {}", headquartersId);

    try {
      // Scope 1 그룹별 세부 정보
      BigDecimal scope1Fixed = scopeEmissionRepository.sumScope1EmissionByGroup(
          headquartersId, "고정연소", year, month);
      BigDecimal scope1Mobile = scopeEmissionRepository.sumScope1EmissionByGroup(
          headquartersId, "이동연소", year, month);
      BigDecimal scope1Process = scopeEmissionRepository.sumScope1EmissionByGroup(
          headquartersId, "공정배출", year, month);
      BigDecimal scope1Factory = scopeEmissionRepository.sumScope1EmissionByGroup(
          headquartersId, "공장설비", year, month);

      // Scope 2 전체
      BigDecimal scope2Total = scopeEmissionRepository.sumScope2TotalEmission(
          headquartersId, year, month);

      // Scope 3 카테고리별
      BigDecimal scope3Cat1 = scopeEmissionRepository.sumScope3EmissionByCategory(
          headquartersId, 1, year, month);
      BigDecimal scope3Cat2 = scopeEmissionRepository.sumScope3EmissionByCategory(
          headquartersId, 2, year, month);
      BigDecimal scope3Cat4 = scopeEmissionRepository.sumScope3EmissionByCategory(
          headquartersId, 4, year, month);
      BigDecimal scope3Cat5 = scopeEmissionRepository.sumScope3EmissionByCategory(
          headquartersId, 5, year, month);

      // Cat.1 계산을 위한 포함/제외 값
      BigDecimal cat1Scope1Included = scope1Fixed.add(scopeEmissionRepository.sumScope1EmissionByGroup(
          headquartersId, "냉매누출", year, month));
      BigDecimal cat1Scope1Excluded = scope1Mobile.add(scope1Factory).add(scope1Process);

      return AggregationDetails.builder()
          .cat1Scope1Included(cat1Scope1Included)
          .cat1Scope1Excluded(cat1Scope1Excluded)
          .cat1Scope2Included(BigDecimal.ZERO) // Scope 2에서 공장설비 제외한 값
          .cat1Scope2Excluded(scope2Total) // 현재 모든 Scope2가 공장설비 관련
          .cat1Scope3Original(scope3Cat1)
          .cat2Scope1Factory(scope1Factory)
          .cat2Scope2Factory(scope2Total)
          .cat2Scope3Original(scope3Cat2)
          .cat4Scope1Mobile(scope1Mobile)
          .cat4Scope3Original(scope3Cat4)
          .cat5Scope1Waste(scope1Process)
          .cat5Scope3Original(scope3Cat5)
          .build();

    } catch (Exception e) {
      log.warn("집계 상세 정보 생성 중 일부 오류 발생 - 본사ID: {}, 오류: {}", headquartersId, e.getMessage());

      return AggregationDetails.builder()
          .cat1Scope1Included(BigDecimal.ZERO)
          .cat1Scope1Excluded(BigDecimal.ZERO)
          .cat1Scope2Included(BigDecimal.ZERO)
          .cat1Scope2Excluded(BigDecimal.ZERO)
          .cat1Scope3Original(BigDecimal.ZERO)
          .cat2Scope1Factory(BigDecimal.ZERO)
          .cat2Scope2Factory(BigDecimal.ZERO)
          .cat2Scope3Original(BigDecimal.ZERO)
          .cat4Scope1Mobile(BigDecimal.ZERO)
          .cat4Scope3Original(BigDecimal.ZERO)
          .cat5Scope1Waste(BigDecimal.ZERO)
          .cat5Scope3Original(BigDecimal.ZERO)
          .build();
    }
  }

}