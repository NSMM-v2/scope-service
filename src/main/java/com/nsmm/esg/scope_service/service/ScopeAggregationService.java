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
   * 종합 집계 결과 조회 (사용자 컨텍스트 기반)
   * 모든 집계 로직이 포함된 종합 결과 제공
   * 사용자의 계층 위치에 따라 적절한 범위의 데이터만 집계
   */
  @Transactional
  public ScopeAggregationResponse getComprehensiveAggregation(
      Long headquartersId, 
      String userType, 
      Long partnerId, 
      String treePath, 
      Integer level,
      Integer year, 
      Integer month) {
    
    log.info("종합 집계 시작 - 본사ID: {}, 사용자타입: {}, 협력사ID: {}, 트리경로: {}, 레벨: {}, 연도: {}, 월: {}", 
        headquartersId, userType, partnerId, treePath, level, year, month);

    try {
      // 사용자 타입에 따른 기본 Scope별 합계 조회
      BigDecimal scope1Total, scope2Total, scope3Total;
      
      if ("HEADQUARTERS".equals(userType)) {
        // 본사: 전체 조직 데이터
        scope1Total = scopeEmissionRepository
            .sumTotalEmissionByScopeTypeAndYearAndMonthForHeadquarters(headquartersId, ScopeType.SCOPE1, year, month);
        scope2Total = scopeEmissionRepository
            .sumTotalEmissionByScopeTypeAndYearAndMonthForHeadquarters(headquartersId, ScopeType.SCOPE2, year, month);
        scope3Total = scopeEmissionRepository
            .sumTotalEmissionByScopeTypeAndYearAndMonthForHeadquarters(headquartersId, ScopeType.SCOPE3, year, month);
      } else {
        // 협력사: 자신의 트리 경로 하위 데이터만
        scope1Total = scopeEmissionRepository
            .sumTotalEmissionByScopeTypeAndTreePathForPartner(headquartersId, treePath, ScopeType.SCOPE1, year, month);
        scope2Total = scopeEmissionRepository
            .sumTotalEmissionByScopeTypeAndTreePathForPartner(headquartersId, treePath, ScopeType.SCOPE2, year, month);
        scope3Total = scopeEmissionRepository
            .sumTotalEmissionByScopeTypeAndTreePathForPartner(headquartersId, treePath, ScopeType.SCOPE3, year, month);
      }

      // Scope 3 카테고리별 특수 집계 (사용자 컨텍스트 적용)
      BigDecimal scope3Cat1 = aggregateScope3Category1(headquartersId, userType, treePath, year, month);
      BigDecimal scope3Cat2 = aggregateScope3Category2(headquartersId, userType, treePath, year, month);
      BigDecimal scope3Cat4 = aggregateScope3Category4(headquartersId, userType, treePath, year, month);
      BigDecimal scope3Cat5 = aggregateScope3Category5(headquartersId, userType, treePath, year, month);

      // 제품별 집계 (사용자 컨텍스트 적용)
      List<ProductEmissionSummary> productSummaries = getProductEmissionSummary(
          headquartersId, userType, partnerId, treePath, level, year, month);

      // 계층별 집계 (사용자 컨텍스트 적용)
      List<HierarchicalEmissionSummary> hierarchicalSummaries = getHierarchicalEmissionSummary(
          headquartersId, userType, partnerId, treePath, level, year, month);

      // 집계 상세 정보 (사용자 컨텍스트 적용)
      AggregationDetails details = createAggregationDetails(headquartersId, userType, treePath, year, month);

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

      log.info("종합 집계 완료 - 본사ID: {}, 사용자타입: {}, 총 배출량: {}", headquartersId, userType, response.getTotalEmission());
      return response;

    } catch (Exception e) {
      log.error("종합 집계 중 오류 발생 - 본사ID: {}, 사용자타입: {}, 오류: {}", headquartersId, userType, e.getMessage(), e);
      throw new RuntimeException("종합 집계 처리 중 오류가 발생했습니다", e);
    }
  }

  // ========================================================================
  // Scope 3 카테고리별 특수 집계 메서드 (Scope 3 Category Aggregation)
  // ========================================================================

  /**
   * Scope 3 Cat.1 집계 (사용자 컨텍스트 기반)
   * = (Scope1 factoryEnabled=false 비공장설비) + (Scope2 factoryEnabled=false 비공장설비) + Scope3 Cat.1
   */
  public BigDecimal aggregateScope3Category1(Long headquartersId, String userType, String treePath, Integer year, Integer month) {
    log.debug("Scope 3 Cat.1 집계 시작 - 본사ID: {}, 사용자타입: {}, 트리경로: {}", headquartersId, userType, treePath);

    BigDecimal scope1NonFactory, scope2NonFactory, scope3Cat1;

    if ("HEADQUARTERS".equals(userType)) {
      // 본사: 전체 조직 데이터
      scope1NonFactory = scopeEmissionRepository.sumScope1EmissionByFactoryDisabled(headquartersId, year, month);
      scope2NonFactory = scopeEmissionRepository.sumScope2EmissionByFactoryDisabled(headquartersId, year, month);
      scope3Cat1 = scopeEmissionRepository.sumScope3EmissionByCategory(headquartersId, 1, year, month);
    } else {
      // 협력사: 트리 경로 하위 데이터만
      scope1NonFactory = scopeEmissionRepository.sumScope1EmissionByFactoryDisabledForPartner(headquartersId, treePath, year, month);
      scope2NonFactory = scopeEmissionRepository.sumScope2EmissionByFactoryDisabledForPartner(headquartersId, treePath, year, month);
      scope3Cat1 = scopeEmissionRepository.sumScope3EmissionByCategoryForPartner(headquartersId, treePath, 1, year, month);
    }

    BigDecimal result = scope1NonFactory.add(scope2NonFactory).add(scope3Cat1);
    log.debug("Scope 3 Cat.1 집계 완료 - 사용자타입: {}, 결과: {}", userType, result);
    return result;
  }

  /**
   * Scope 3 Cat.2 집계 (사용자 컨텍스트 기반)
   * = Scope1 factoryEnabled=true 공장설비 + Scope2 factoryEnabled=true 공장설비 + Scope3 Cat.2
   */
  public BigDecimal aggregateScope3Category2(Long headquartersId, String userType, String treePath, Integer year, Integer month) {
    log.debug("Scope 3 Cat.2 집계 시작 - 본사ID: {}, 사용자타입: {}, 트리경로: {}", headquartersId, userType, treePath);

    BigDecimal scope1Factory, scope2Factory, scope3Cat2;

    if ("HEADQUARTERS".equals(userType)) {
      // 본사: 전체 조직 데이터
      scope1Factory = scopeEmissionRepository.sumScope1EmissionByFactoryEnabled(headquartersId, year, month);
      scope2Factory = scopeEmissionRepository.sumScope2EmissionByFactoryEnabled(headquartersId, year, month);
      scope3Cat2 = scopeEmissionRepository.sumScope3EmissionByCategory(headquartersId, 2, year, month);
    } else {
      // 협력사: 트리 경로 하위 데이터만
      scope1Factory = scopeEmissionRepository.sumScope1EmissionByFactoryEnabledForPartner(headquartersId, treePath, year, month);
      scope2Factory = scopeEmissionRepository.sumScope2EmissionByFactoryEnabledForPartner(headquartersId, treePath, year, month);
      scope3Cat2 = scopeEmissionRepository.sumScope3EmissionByCategoryForPartner(headquartersId, treePath, 2, year, month);
    }

    BigDecimal result = scope1Factory.add(scope2Factory).add(scope3Cat2);
    log.debug("Scope 3 Cat.2 집계 완료 - 사용자타입: {}, 결과: {}", userType, result);
    return result;
  }

  /**
   * Scope 3 Cat.4 집계 (사용자 컨텍스트 기반)
   * = Scope1 이동연소 + Scope3 Cat.4
   */
  public BigDecimal aggregateScope3Category4(Long headquartersId, String userType, String treePath, Integer year, Integer month) {
    log.debug("Scope 3 Cat.4 집계 시작 - 본사ID: {}, 사용자타입: {}, 트리경로: {}", headquartersId, userType, treePath);

    BigDecimal scope1Mobile, scope3Cat4;

    if ("HEADQUARTERS".equals(userType)) {
      // 본사: 전체 조직 데이터
      scope1Mobile = scopeEmissionRepository.sumScope1EmissionByGroup(headquartersId, "이동연소", year, month);
      scope3Cat4 = scopeEmissionRepository.sumScope3EmissionByCategory(headquartersId, 4, year, month);
    } else {
      // 협력사: 트리 경로 하위 데이터만
      scope1Mobile = scopeEmissionRepository.sumScope1EmissionByGroupForPartner(headquartersId, treePath, "이동연소", year, month);
      scope3Cat4 = scopeEmissionRepository.sumScope3EmissionByCategoryForPartner(headquartersId, treePath, 4, year, month);
    }

    BigDecimal result = scope1Mobile.add(scope3Cat4);
    log.debug("Scope 3 Cat.4 집계 완료 - 사용자타입: {}, 결과: {}", userType, result);
    return result;
  }

  /**
   * Scope 3 Cat.5 집계 (사용자 컨텍스트 기반)
   * = Scope1 폐수처리 + Scope3 Cat.5
   */
  public BigDecimal aggregateScope3Category5(Long headquartersId, String userType, String treePath, Integer year, Integer month) {
    log.debug("Scope 3 Cat.5 집계 시작 - 본사ID: {}, 사용자타입: {}, 트리경로: {}", headquartersId, userType, treePath);

    BigDecimal scope1Waste, scope3Cat5;

    if ("HEADQUARTERS".equals(userType)) {
      // 본사: 전체 조직 데이터
      scope1Waste = scopeEmissionRepository.sumScope1EmissionByGroup(headquartersId, "공정배출", year, month);
      scope3Cat5 = scopeEmissionRepository.sumScope3EmissionByCategory(headquartersId, 5, year, month);
    } else {
      // 협력사: 트리 경로 하위 데이터만
      scope1Waste = scopeEmissionRepository.sumScope1EmissionByGroupForPartner(headquartersId, treePath, "공정배출", year, month);
      scope3Cat5 = scopeEmissionRepository.sumScope3EmissionByCategoryForPartner(headquartersId, treePath, 5, year, month);
    }

    BigDecimal result = scope1Waste.add(scope3Cat5);
    log.debug("Scope 3 Cat.5 집계 완료 - 사용자타입: {}, 결과: {}", userType, result);
    return result;
  }

  // ========================================================================
  // 제품별 집계 메서드 (Product Aggregation)
  // ========================================================================

  /**
   * 제품별 배출량 집계 (사용자 컨텍스트 기반)
   * company_product_code 기준으로 제품별 총 배출량 집계
   */
  @Transactional
  public List<ProductEmissionSummary> getProductEmissionSummary(
      Long headquartersId, 
      String userType, 
      Long partnerId, 
      String treePath, 
      Integer level,
      Integer year, 
      Integer month) {
    
    log.debug("제품별 집계 시작 - 본사ID: {}, 사용자타입: {}, 트리경로: {}", headquartersId, userType, treePath);

    List<Object[]> queryResults;
    
    if ("HEADQUARTERS".equals(userType)) {
      // 본사: 전체 조직 데이터
      queryResults = scopeEmissionRepository.sumEmissionByProductCode(headquartersId, year, month);
    } else {
      // 협력사: 트리 경로 하위 데이터만
      queryResults = scopeEmissionRepository.sumEmissionByProductCodeForPartner(headquartersId, treePath, year, month);
    }

    List<ProductEmissionSummary> result = queryResults.stream()
        .map(ProductEmissionSummary::from)
        .collect(Collectors.toList());

    log.debug("제품별 집계 완료 - 본사ID: {}, 사용자타입: {}, 제품 수: {}", headquartersId, userType, result.size());
    return result;
  }

  // ========================================================================
  // 계층적 집계 메서드 (Hierarchical Aggregation)
  // ========================================================================

  /**
   * 계층적 배출량 집계 (사용자 컨텍스트 기반)
   * tree_path를 기반으로 하위 협력사의 배출량을 상위로 누적하여 집계
   */
  @Transactional
  public List<HierarchicalEmissionSummary> getHierarchicalEmissionSummary(
      Long headquartersId, 
      String userType,
      Long partnerId,
      String baseTreePath, 
      Integer level,
      Integer year, 
      Integer month) {

    log.debug("계층적 집계 시작 - 본사ID: {}, 사용자타입: {}, 기준경로: {}", headquartersId, userType, baseTreePath);

    // 사용자 타입에 따른 기준 경로 설정
    String effectiveTreePath;
    if ("HEADQUARTERS".equals(userType)) {
      // 본사: 전체 조직 또는 지정된 기준 경로
      effectiveTreePath = (baseTreePath != null && !baseTreePath.trim().isEmpty()) ? baseTreePath : "/";
    } else {
      // 협력사: 자신의 트리 경로 하위만
      effectiveTreePath = baseTreePath;
    }

    List<Object[]> queryResults = scopeEmissionRepository.sumEmissionByTreePath(
        headquartersId, effectiveTreePath, year, month);

    List<HierarchicalEmissionSummary> result = queryResults.stream()
        .map(this::convertToHierarchicalSummary)
        .collect(Collectors.toList());

    log.debug("계층적 집계 완료 - 본사ID: {}, 사용자타입: {}, 계층 수: {}", headquartersId, userType, result.size());
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
   * 집계 상세 정보 생성 (사용자 컨텍스트 기반)
   */
  private AggregationDetails createAggregationDetails(Long headquartersId, String userType, String treePath, Integer year, Integer month) {
    log.debug("집계 상세 정보 생성 시작 - 본사ID: {}, 사용자타입: {}, 트리경로: {}", headquartersId, userType, treePath);

    try {
      BigDecimal scope1Fixed, scope1Mobile, scope1Process, scope1Factory;
      BigDecimal scope1FactoryEnabled, scope1FactoryDisabled, scope2FactoryEnabled, scope2FactoryDisabled;
      BigDecimal scope2Total, scope3Cat1, scope3Cat2, scope3Cat4, scope3Cat5;

      if ("HEADQUARTERS".equals(userType)) {
        // 본사: 전체 조직 데이터
        scope1Fixed = scopeEmissionRepository.sumScope1EmissionByGroup(headquartersId, "고정연소", year, month);
        scope1Mobile = scopeEmissionRepository.sumScope1EmissionByGroup(headquartersId, "이동연소", year, month);
        scope1Process = scopeEmissionRepository.sumScope1EmissionByGroup(headquartersId, "공정배출", year, month);
        scope1Factory = scopeEmissionRepository.sumScope1EmissionByGroup(headquartersId, "공장설비", year, month);
        
        scope1FactoryEnabled = scopeEmissionRepository.sumScope1EmissionByFactoryEnabled(headquartersId, year, month);
        scope1FactoryDisabled = scopeEmissionRepository.sumScope1EmissionByFactoryDisabled(headquartersId, year, month);
        scope2FactoryEnabled = scopeEmissionRepository.sumScope2EmissionByFactoryEnabled(headquartersId, year, month);
        scope2FactoryDisabled = scopeEmissionRepository.sumScope2EmissionByFactoryDisabled(headquartersId, year, month);
        
        scope2Total = scopeEmissionRepository.sumScope2TotalEmission(headquartersId, year, month);
        scope3Cat1 = scopeEmissionRepository.sumScope3EmissionByCategory(headquartersId, 1, year, month);
        scope3Cat2 = scopeEmissionRepository.sumScope3EmissionByCategory(headquartersId, 2, year, month);
        scope3Cat4 = scopeEmissionRepository.sumScope3EmissionByCategory(headquartersId, 4, year, month);
        scope3Cat5 = scopeEmissionRepository.sumScope3EmissionByCategory(headquartersId, 5, year, month);
      } else {
        // 협력사: 트리 경로 하위 데이터만
        scope1Fixed = scopeEmissionRepository.sumScope1EmissionByGroupForPartner(headquartersId, treePath, "고정연소", year, month);
        scope1Mobile = scopeEmissionRepository.sumScope1EmissionByGroupForPartner(headquartersId, treePath, "이동연소", year, month);
        scope1Process = scopeEmissionRepository.sumScope1EmissionByGroupForPartner(headquartersId, treePath, "공정배출", year, month);
        scope1Factory = scopeEmissionRepository.sumScope1EmissionByGroupForPartner(headquartersId, treePath, "공장설비", year, month);
        
        scope1FactoryEnabled = scopeEmissionRepository.sumScope1EmissionByFactoryEnabledForPartner(headquartersId, treePath, year, month);
        scope1FactoryDisabled = scopeEmissionRepository.sumScope1EmissionByFactoryDisabledForPartner(headquartersId, treePath, year, month);
        scope2FactoryEnabled = scopeEmissionRepository.sumScope2EmissionByFactoryEnabledForPartner(headquartersId, treePath, year, month);
        scope2FactoryDisabled = scopeEmissionRepository.sumScope2EmissionByFactoryDisabledForPartner(headquartersId, treePath, year, month);
        
        scope2Total = scopeEmissionRepository.sumScope2TotalEmissionForPartner(headquartersId, treePath, year, month);
        scope3Cat1 = scopeEmissionRepository.sumScope3EmissionByCategoryForPartner(headquartersId, treePath, 1, year, month);
        scope3Cat2 = scopeEmissionRepository.sumScope3EmissionByCategoryForPartner(headquartersId, treePath, 2, year, month);
        scope3Cat4 = scopeEmissionRepository.sumScope3EmissionByCategoryForPartner(headquartersId, treePath, 4, year, month);
        scope3Cat5 = scopeEmissionRepository.sumScope3EmissionByCategoryForPartner(headquartersId, treePath, 5, year, month);
      }

      // Cat.1 계산을 위한 포함/제외 값 (factoryEnabled 기반)
      BigDecimal cat1Scope1Included = scope1FactoryDisabled;
      BigDecimal cat1Scope1Excluded = scope1FactoryEnabled.add(scope1Mobile).add(scope1Process);

      return AggregationDetails.builder()
          .cat1Scope1Included(cat1Scope1Included)
          .cat1Scope1Excluded(cat1Scope1Excluded)
          .cat1Scope2Included(scope2FactoryDisabled)
          .cat1Scope2Excluded(scope2FactoryEnabled)
          .cat1Scope3Original(scope3Cat1)
          .cat2Scope1Factory(scope1FactoryEnabled)
          .cat2Scope2Factory(scope2FactoryEnabled)
          .cat2Scope3Original(scope3Cat2)
          .cat4Scope1Mobile(scope1Mobile)
          .cat4Scope3Original(scope3Cat4)
          .cat5Scope1Waste(scope1Process)
          .cat5Scope3Original(scope3Cat5)
          .build();

    } catch (Exception e) {
      log.warn("집계 상세 정보 생성 중 일부 오류 발생 - 본사ID: {}, 사용자타입: {}, 오류: {}", headquartersId, userType, e.getMessage());

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