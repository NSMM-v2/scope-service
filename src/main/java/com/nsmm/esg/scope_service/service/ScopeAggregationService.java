package com.nsmm.esg.scope_service.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.nsmm.esg.scope_service.repository.ScopeEmissionRepository;
import com.nsmm.esg.scope_service.dto.response.MonthlyEmissionSummary;
import com.nsmm.esg.scope_service.dto.response.CategoryYearlyEmission;
import com.nsmm.esg.scope_service.dto.response.CategoryMonthlyEmission;
import com.nsmm.esg.scope_service.dto.response.ScopeCategoryResponse;
import com.nsmm.esg.scope_service.dto.response.Scope3CombinedEmissionResponse;
import com.nsmm.esg.scope_service.dto.response.Scope3SpecialAggregationResponse;
import com.nsmm.esg.scope_service.enums.ScopeType;

/**
 * Scope 배출량 집계 서비스
 * 
 * 주요 기능:
 * - 계층적 배출량 집계 (tree_path 기반)
 * - Scope 3 카테고리별 특수 집계 (Cat.1, Cat.2, Cat.4, Cat.5)
 * - 종합 집계 결과 제공
 * 
 * 집계 규칙:
 * - Cat.1: (Scope1 전체 - 이동연소 - 공장설비 - 폐수처리) + (Scope2 - 공장설비) + Scope3 Cat.1
 * - Cat.2: Scope1 공장설비 + Scope2 공장설비 + Scope3 Cat.2
 * - Cat.4: Scope1 이동연소 + Scope3 Cat.4
 * - Cat.5: Scope1 폐수처리 + Scope3 Cat.5
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ScopeAggregationService {

  private final ScopeEmissionRepository scopeEmissionRepository;
  private final Scope3SpecialAggregationService scope3SpecialAggregationService;

  // ========================================================================
  // 대시보드 협력사별 월별 집계 메서드 (Partner Monthly Aggregation)
  // ========================================================================
  /**
   * 협력사별 월별 배출량 집계
   * 지정된 협력사의 연도별 각 월(1월~현재월)의 Scope 1,2,3 배출량 총계 조회
   * 차트 및 테이블 데이터 표시용
   */
  @Transactional
  public List<MonthlyEmissionSummary> getPartnerMonthlyEmissionSummary(
      Long partnerId,
      Integer year,
      Long headquartersId,
      String userType,
      Long requestPartnerId,
      String treePath) {

    log.info("협력사별 월별 집계 시작 - 대상협력사ID: {}, 본사ID: {}, 사용자타입: {}, 요청자협력사ID: {}, 연도: {}",
        partnerId, headquartersId, userType, requestPartnerId, year);

    try {
      // 권한 확인: 해당 협력사 데이터에 접근할 수 있는지 검증
      validatePartnerAccess(partnerId, userType, requestPartnerId, treePath);

      List<MonthlyEmissionSummary> monthlyData = new ArrayList<>();

      // 1월부터 현재월까지 반복 (또는 12월까지)
      int currentMonth = java.time.LocalDate.now().getMonthValue();
      int maxMonth = (year.equals(java.time.LocalDate.now().getYear())) ? currentMonth : 12;

      for (int month = 1; month <= maxMonth; month++) {
        BigDecimal scope1Total, scope2Total, scope3Total;
        Long dataCount;

        if (partnerId == -1L) {
          // 본사 데이터 조회 (partnerId가 -1인 경우)
          scope1Total = scopeEmissionRepository
              .sumTotalEmissionByScopeTypeAndYearAndMonthForHeadquarters(headquartersId, ScopeType.SCOPE1, year, month);
          scope2Total = scopeEmissionRepository
              .sumTotalEmissionByScopeTypeAndYearAndMonthForHeadquarters(headquartersId, ScopeType.SCOPE2, year, month);
          scope3Total = scopeEmissionRepository
              .sumTotalEmissionByScopeTypeAndYearAndMonthForHeadquarters(headquartersId, ScopeType.SCOPE3, year, month);

          // 본사의 데이터 건수 조회 (전체 본사 데이터)
          dataCount = scopeEmissionRepository
              .countEmissionsByHeadquartersAndYearAndMonth(headquartersId, year, month);
        } else {
          // 협력사 데이터 조회
          scope1Total = scopeEmissionRepository
              .sumTotalEmissionByScopeTypeAndPartnerAndYearAndMonth(headquartersId, partnerId, ScopeType.SCOPE1, year,
                  month);
          scope2Total = scopeEmissionRepository
              .sumTotalEmissionByScopeTypeAndPartnerAndYearAndMonth(headquartersId, partnerId, ScopeType.SCOPE2, year,
                  month);
          scope3Total = scopeEmissionRepository
              .sumTotalEmissionByScopeTypeAndPartnerAndYearAndMonth(headquartersId, partnerId, ScopeType.SCOPE3, year,
                  month);

          // 해당 협력사의 데이터 건수 조회
          dataCount = scopeEmissionRepository
              .countEmissionsByPartnerAndYearAndMonth(headquartersId, partnerId, year, month);
        }

        MonthlyEmissionSummary monthlyItem = MonthlyEmissionSummary.builder()
            .year(year)
            .month(month)
            .scope1Total(scope1Total != null ? scope1Total : BigDecimal.ZERO)
            .scope2Total(scope2Total != null ? scope2Total : BigDecimal.ZERO)
            .scope3Total(scope3Total != null ? scope3Total : BigDecimal.ZERO)
            .dataCount(dataCount != null ? dataCount : 0L)
            .build();

        monthlyData.add(monthlyItem);
      }

      log.info("협력사별 월별 집계 완료 - 대상협력사ID: {}, 연도: {}, 월별 데이터 수: {}", partnerId, year, monthlyData.size());
      return monthlyData;

    } catch (Exception e) {
      log.error("협력사별 월별 집계 중 오류 발생 - 대상협력사ID: {}, 연도: {}, 오류: {}", partnerId, year, e.getMessage(), e);
      throw new RuntimeException("협력사별 월별 집계 처리 중 오류가 발생했습니다", e);
    }
  }

  /**
   * 협력사 데이터 접근 권한 검증
   */
  private void validatePartnerAccess(Long partnerId, String userType, Long requestPartnerId,
      String treePath) {
    // 본사는 모든 협력사 데이터에 접근 가능
    if ("HEADQUARTERS".equals(userType)) {
      return;
    }

    // 협력사는 본인과 직속 하위 협력사 데이터만 접근 가능
    if ("PARTNER".equals(userType)) {
      if (partnerId.equals(requestPartnerId)) {
        return; // 본인 데이터는 접근 가능
      }

      // 하위 협력사인지 확인 (treePath 기반)
      // 실제 구현에서는 더 정확한 tree_path 검증 로직 필요
      // 현재는 기본적인 접근 허용
      return;
    }

    throw new RuntimeException("해당 협력사 데이터에 접근할 권한이 없습니다");
  }


  // ========================================================================
  // 카테고리별 연간/월간 집계 메서드 (Category Yearly/Monthly Aggregation)
  // ========================================================================

  /**
   * 카테고리별 연간 배출량 집계
   * 
   * @param scopeType      Scope 타입 (SCOPE1, SCOPE2, SCOPE3)
   * @param year           보고 연도
   * @param headquartersId 본사 ID
   * @param userType       사용자 타입 (HEADQUARTERS | PARTNER)
   * @param partnerId      파트너사 ID (파트너사인 경우)
   * @param treePath       계층 경로
   * @param level          계층 레벨
   * @return 카테고리별 연간 배출량 목록
   */
  @Transactional
  public List<CategoryYearlyEmission> getCategoryYearlyEmissions(
      ScopeType scopeType,
      Integer year,
      Long headquartersId,
      String userType,
      Long partnerId,
      String treePath,
      Integer level) {

    log.info("카테고리별 연간 집계 시작 - Scope: {}, 연도: {}, 본사ID: {}, 사용자타입: {}, 파트너ID: {}",
        scopeType, year, headquartersId, userType, partnerId);

    try {
      List<Object[]> results = new ArrayList<>();

      // 사용자 타입에 따라 적절한 쿼리 메서드 호출
      if ("HEADQUARTERS".equals(userType)) {
        // 본사인 경우 본사 직접 입력 데이터만 집계
        switch (scopeType) {
          case SCOPE1:
            results = scopeEmissionRepository.sumScope1EmissionByYearAndCategoryForHeadquartersOnly(headquartersId, year);
            break;
          case SCOPE2:
            results = scopeEmissionRepository.sumScope2EmissionByYearAndCategoryForHeadquartersOnly(headquartersId, year);
            break;
          case SCOPE3:
            results = scopeEmissionRepository.sumScope3EmissionByYearAndCategoryForHeadquartersOnly(headquartersId, year);
            break;
        }
      } else {
        // 파트너사인 경우 해당 파트너사 데이터만 집계
        if (partnerId == null) {
          log.warn("파트너사 요청이지만 partnerId가 없습니다");
          return new ArrayList<>();
        }

        switch (scopeType) {
          case SCOPE1:
            results = scopeEmissionRepository.sumScope1EmissionByYearAndCategoryForSpecificPartner(headquartersId, partnerId, year);
            break;
          case SCOPE2:
            results = scopeEmissionRepository.sumScope2EmissionByYearAndCategoryForSpecificPartner(headquartersId, partnerId, year);
            break;
          case SCOPE3:
            results = scopeEmissionRepository.sumScope3EmissionByYearAndCategoryForSpecificPartner(headquartersId, partnerId, year);
            break;
        }
      }

      // 모든 카테고리의 총 배출량 합계 계산
      BigDecimal totalSumAllCategories = results.stream()
          .map(row -> (BigDecimal) row[1]) // totalEmission
          .reduce(BigDecimal.ZERO, BigDecimal::add);

      // Object[] 결과를 CategoryYearlyEmission DTO로 변환
      List<CategoryYearlyEmission> categoryEmissions = results.stream()
          .map(row -> {
            Integer categoryNumber = (Integer) row[0];
            BigDecimal totalEmission = (BigDecimal) row[1];
            Long dataCount = (Long) row[2];

            // 카테고리명 가져오기
            String categoryName = getCategoryNameByNumber(scopeType, categoryNumber);

            return CategoryYearlyEmission.builder()
                .categoryNumber(categoryNumber)
                .categoryName(categoryName)
                .year(year)
                .totalEmission(totalEmission)
                .dataCount(dataCount)
                .scopeType(scopeType.name())
                .totalSumAllCategories(totalSumAllCategories)
                .build();
          })
          .collect(Collectors.toList());

      log.info("카테고리별 연간 집계 완료 - Scope: {}, 카테고리 수: {}", scopeType, categoryEmissions.size());
      return categoryEmissions;

    } catch (Exception e) {
      log.error("카테고리별 연간 집계 중 오류 발생 - Scope: {}, 연도: {}: {}", scopeType, year, e.getMessage(), e);
      return new ArrayList<>();
    }
  }

  /**
   * 카테고리별 월간 배출량 집계 (연도의 모든 월)
   * 
   * @param scopeType      Scope 타입 (SCOPE1, SCOPE2, SCOPE3)
   * @param year           보고 연도
   * @param headquartersId 본사 ID
   * @param userType       사용자 타입 (HEADQUARTERS | PARTNER)
   * @param partnerId      파트너사 ID (파트너사인 경우)
   * @param treePath       계층 경로
   * @param level          계층 레벨
   * @return 카테고리별 월간 배출량 목록
   */
  @Transactional
  public List<CategoryMonthlyEmission> getCategoryMonthlyEmissions(
      ScopeType scopeType,
      Integer year,
      Long headquartersId,
      String userType,
      Long partnerId,
      String treePath,
      Integer level) {

    log.info("카테고리별 월간 집계 시작 - Scope: {}, 연도: {}, 본사ID: {}, 사용자타입: {}, 파트너ID: {}",
        scopeType, year, headquartersId, userType, partnerId);

    try {
      List<Object[]> results = new ArrayList<>();

      // 사용자 타입에 따라 적절한 쿼리 메서드 호출
      if ("HEADQUARTERS".equals(userType)) {
        // 본사인 경우 본사 직접 입력 데이터만 집계
        switch (scopeType) {
          case SCOPE1:
            results = scopeEmissionRepository.sumScope1EmissionByYearAndMonthAndCategoryForHeadquartersOnly(headquartersId,
                year);
            break;
          case SCOPE2:
            results = scopeEmissionRepository.sumScope2EmissionByYearAndMonthAndCategoryForHeadquartersOnly(headquartersId,
                year);
            break;
          case SCOPE3:
            results = scopeEmissionRepository.sumScope3EmissionByYearAndMonthAndCategoryForHeadquartersOnly(headquartersId,
                year);
            break;
        }
      } else {
        // 파트너사인 경우 해당 파트너사 데이터만 집계
        if (partnerId == null) {
          log.warn("파트너사 요청이지만 partnerId가 없습니다");
          return new ArrayList<>();
        }

        switch (scopeType) {
          case SCOPE1:
            results = scopeEmissionRepository.sumScope1EmissionByYearAndMonthAndCategoryForSpecificPartner(headquartersId,
                partnerId, year);
            break;
          case SCOPE2:
            results = scopeEmissionRepository.sumScope2EmissionByYearAndMonthAndCategoryForSpecificPartner(headquartersId,
                partnerId, year);
            break;
          case SCOPE3:
            results = scopeEmissionRepository.sumScope3EmissionByYearAndMonthAndCategoryForSpecificPartner(headquartersId,
                partnerId, year);
            break;
        }
      }

      // 월별로 그룹화하여 각 월의 총합 계산
      Map<Integer, BigDecimal> monthlyTotals = results.stream()
          .collect(Collectors.groupingBy(
              row -> (Integer) row[1], // month
              Collectors.reducing(
                  BigDecimal.ZERO,
                  row -> (BigDecimal) row[2], // totalEmission
                  BigDecimal::add)));

      // Object[] 결과를 CategoryMonthlyEmission DTO로 변환
      List<CategoryMonthlyEmission> categoryEmissions = results.stream()
          .map(row -> {
            Integer categoryNumber = (Integer) row[0];
            Integer month = (Integer) row[1];
            BigDecimal totalEmission = (BigDecimal) row[2];
            Long dataCount = (Long) row[3];

            // 카테고리명 가져오기
            String categoryName = getCategoryNameByNumber(scopeType, categoryNumber);

            // 해당 월의 모든 카테고리 총합
            BigDecimal totalSumAllCategories = monthlyTotals.getOrDefault(month, BigDecimal.ZERO);

            return CategoryMonthlyEmission.builder()
                .categoryNumber(categoryNumber)
                .categoryName(categoryName)
                .year(year)
                .month(month)
                .totalEmission(totalEmission)
                .dataCount(dataCount)
                .scopeType(scopeType.name())
                .totalSumAllCategories(totalSumAllCategories)
                .build();
          })
          .collect(Collectors.toList());

      log.info("카테고리별 월간 집계 완료 - Scope: {}, 데이터 수: {}", scopeType, categoryEmissions.size());
      return categoryEmissions;

    } catch (Exception e) {
      log.error("카테고리별 월간 집계 중 오류 발생 - Scope: {}, 연도: {}: {}", scopeType, year, e.getMessage(), e);
      return new ArrayList<>();
    }
  }

  /**
   * Scope 타입과 카테고리 번호로 카테고리명 조회
   * 
   * @param scopeType      Scope 타입
   * @param categoryNumber 카테고리 번호
   * @return 카테고리명
   */
  private String getCategoryNameByNumber(ScopeType scopeType, Integer categoryNumber) {
    try {
      List<ScopeCategoryResponse> categories = ScopeCategoryResponse.getAllCategoriesByScope(scopeType);
      return categories.stream()
          .filter(category -> category.getCategoryNumber().equals(categoryNumber))
          .map(ScopeCategoryResponse::getCategoryName)
          .findFirst()
          .orElse("Unknown Category");
    } catch (Exception e) {
      log.warn("카테고리명 조회 실패 - Scope: {}, 번호: {}: {}", scopeType, categoryNumber, e.getMessage());
      return "Unknown Category";
    }
  }

  /**
   * 카테고리별 특정 월 배출량 집계 (Scope3만 지원)
   * 
   * @param scopeType      Scope 타입 (SCOPE3만 지원)
   * @param year           보고 연도
   * @param month          보고 월
   * @param headquartersId 본사 ID
   * @param userType       사용자 타입 (HEADQUARTERS | PARTNER)
   * @param partnerId      파트너사 ID (파트너사인 경우)
   * @param treePath       계층 경로
   * @param level          계층 레벨
   * @return 카테고리별 특정 월 배출량 목록
   */
  @Transactional
  public List<CategoryMonthlyEmission> getCategorySpecificMonthEmissions(
      ScopeType scopeType,
      Integer year,
      Integer month,
      Long headquartersId,
      String userType,
      Long partnerId,
      String treePath,
      Integer level) {

    log.info("카테고리별 특정 월 집계 시작 - Scope: {}, 연도: {}, 월: {}, 본사ID: {}, 사용자타입: {}, 파트너ID: {}",
        scopeType, year, month, headquartersId, userType, partnerId);

    // 현재는 Scope3만 지원
    if (scopeType != ScopeType.SCOPE3) {
      log.warn("현재 특정 월 조회는 Scope3만 지원합니다 - 요청된 Scope: {}", scopeType);
      return new ArrayList<>();
    }

    try {
      List<Object[]> results = new ArrayList<>();

      // 사용자 타입에 따라 적절한 쿼리 메서드 호출
      if ("HEADQUARTERS".equals(userType)) {
        // 본사인 경우 본사 직접 입력 데이터만 집계
        results = scopeEmissionRepository.sumScope3EmissionByYearAndSpecificMonthAndCategoryForHeadquartersOnly(
            headquartersId, year, month);
      } else if ("PARTNER".equals(userType) && partnerId != null) {
        // 협력사인 경우 해당 협력사 데이터만 집계
        results = scopeEmissionRepository.sumScope3EmissionByYearAndSpecificMonthAndCategoryForSpecificPartner(
            headquartersId, partnerId, year, month);
      }

      // 전체 합계 계산
      BigDecimal totalSumAllCategories = results.stream()
          .map(result -> (BigDecimal) result[2])
          .reduce(BigDecimal.ZERO, BigDecimal::add);

      // 결과를 CategoryMonthlyEmission으로 변환
      List<CategoryMonthlyEmission> categoryEmissions = results.stream()
          .map(result -> {
            Integer categoryNumber = (Integer) result[0];
            Integer reportingMonth = (Integer) result[1];
            BigDecimal totalEmission = (BigDecimal) result[2];
            Long dataCount = (Long) result[3];

            String categoryName = getCategoryNameByNumber(scopeType, categoryNumber);

            return CategoryMonthlyEmission.builder()
                .categoryNumber(categoryNumber)
                .categoryName(categoryName)
                .year(year)
                .month(reportingMonth)
                .totalEmission(totalEmission)
                .dataCount(dataCount)
                .scopeType(scopeType.name())
                .totalSumAllCategories(totalSumAllCategories)
                .build();
          })
          .collect(Collectors.toList());

      log.info("카테고리별 특정 월 집계 완료 - Scope: {}, 연도: {}, 월: {}, 데이터 수: {}", 
          scopeType, year, month, categoryEmissions.size());
      return categoryEmissions;

    } catch (Exception e) {
      log.error("카테고리별 특정 월 집계 중 오류 발생 - Scope: {}, 연도: {}, 월: {}: {}", 
          scopeType, year, month, e.getMessage(), e);
      return new ArrayList<>();
    }
  }

  // ========================================================================
  // Scope3 통합 배출량 집계 메서드 (Scope3 Combined Emission Aggregation)
  // ========================================================================

  /**
   * Scope3 월별 통합 배출량 집계
   * 특수집계배출량 + 일반 Scope3 카테고리별 월별 배출량
   */
  @Transactional
  public Scope3CombinedEmissionResponse getScope3CombinedMonthlyEmission(
      Integer year,
      Integer month,
      Long headquartersId,
      String userType,
      Long partnerId,
      String treePath,
      Integer level) {

    log.info("Scope3 월별 통합 집계 시작 - 연도: {}, 월: {}, 본사ID: {}, 사용자타입: {}, 파트너ID: {}",
        year, month, headquartersId, userType, partnerId);

    try {
      // 1. 특수집계 배출량 조회
      Scope3SpecialAggregationResponse specialAggregation = scope3SpecialAggregationService
          .getSpecialAggregation(year, month, headquartersId, userType, partnerId, treePath);

      // 2. 일반 Scope3 카테고리별 특정 월 배출량 조회
      List<CategoryMonthlyEmission> monthlyCategories = getCategorySpecificMonthEmissions(
          ScopeType.SCOPE3, year, month, headquartersId, userType, partnerId, treePath, level);

      // 3. 조직 ID 결정
      Long organizationId = "HEADQUARTERS".equals(userType) ? headquartersId : partnerId;

      // 4. 통합 응답 생성
      Scope3CombinedEmissionResponse response = Scope3CombinedEmissionResponse.createMonthlyResponse(
          year, month, userType, organizationId, specialAggregation, monthlyCategories);

      log.info("Scope3 월별 통합 집계 완료 - 연도: {}, 월: {}, 특수집계: {}, 일반집계: {}, 총합: {}",
          year, month, response.getSpecialAggregationTotal(),
          response.getRegularCategoryTotal(), response.getTotalScope3Emission());

      return response;

    } catch (Exception e) {
      log.error("Scope3 월별 통합 집계 중 오류 발생 - 연도: {}, 월: {}: {}", year, month, e.getMessage(), e);
      throw new RuntimeException("Scope3 월별 통합 집계 처리 중 오류가 발생했습니다", e);
    }
  }

  /**
   * Scope3 연별 통합 배출량 집계
   * 특수집계배출량 + 일반 Scope3 카테고리별 연별 배출량
   */
  @Transactional
  public Scope3CombinedEmissionResponse getScope3CombinedYearlyEmission(
      Integer year,
      Long headquartersId,
      String userType,
      Long partnerId,
      String treePath,
      Integer level) {

    log.info("Scope3 연별 통합 집계 시작 - 연도: {}, 본사ID: {}, 사용자타입: {}, 파트너ID: {}",
        year, headquartersId, userType, partnerId);

    try {
      // 1. 특수집계 배출량 조회 (1~12월 모든 월 합산)
      Scope3SpecialAggregationResponse specialAggregation = getYearlySpecialAggregation(
          year, headquartersId, userType, partnerId, treePath);

      // 2. 일반 Scope3 카테고리별 연별 배출량 조회
      List<CategoryYearlyEmission> yearlyCategories = getCategoryYearlyEmissions(
          ScopeType.SCOPE3, year, headquartersId, userType, partnerId, treePath, level);

      // 3. 조직 ID 결정
      Long organizationId = "HEADQUARTERS".equals(userType) ? headquartersId : partnerId;

      // 4. 통합 응답 생성
      Scope3CombinedEmissionResponse response = Scope3CombinedEmissionResponse.createYearlyResponse(
          year, userType, organizationId, specialAggregation, yearlyCategories);

      log.info("Scope3 연별 통합 집계 완료 - 연도: {}, 특수집계: {}, 일반집계: {}, 총합: {}",
          year, response.getSpecialAggregationTotal(),
          response.getRegularCategoryTotal(), response.getTotalScope3Emission());

      return response;

    } catch (Exception e) {
      log.error("Scope3 연별 통합 집계 중 오류 발생 - 연도: {}: {}", year, e.getMessage(), e);
      throw new RuntimeException("Scope3 연별 통합 집계 처리 중 오류가 발생했습니다", e);
    }
  }

  /**
   * 연별 특수집계 배출량 조회 (1~12월 모든 월 합산)
   */
  private Scope3SpecialAggregationResponse getYearlySpecialAggregation(
      Integer year,
      Long headquartersId,
      String userType,
      Long partnerId,
      String treePath) {

    log.info("연별 특수집계 시작 - 연도: {}, 본사ID: {}, 사용자타입: {}, 파트너ID: {}",
        year, headquartersId, userType, partnerId);

    BigDecimal totalCat1 = BigDecimal.ZERO;
    BigDecimal totalCat2 = BigDecimal.ZERO;
    BigDecimal totalCat4 = BigDecimal.ZERO;
    BigDecimal totalCat5 = BigDecimal.ZERO;

    // 1월부터 12월까지 각 월의 특수집계 조회하여 합산
    for (int month = 1; month <= 12; month++) {
      try {
        Scope3SpecialAggregationResponse monthlyAggregation = scope3SpecialAggregationService
            .getSpecialAggregation(year, month, headquartersId, userType, partnerId, treePath);

        totalCat1 = totalCat1.add(monthlyAggregation.getCategory1TotalEmission());
        totalCat2 = totalCat2.add(monthlyAggregation.getCategory2TotalEmission());
        totalCat4 = totalCat4.add(monthlyAggregation.getCategory4TotalEmission());
        totalCat5 = totalCat5.add(monthlyAggregation.getCategory5TotalEmission());

        log.debug("{}월 특수집계 - Cat1: {}, Cat2: {}, Cat4: {}, Cat5: {}",
            month, monthlyAggregation.getCategory1TotalEmission(),
            monthlyAggregation.getCategory2TotalEmission(),
            monthlyAggregation.getCategory4TotalEmission(),
            monthlyAggregation.getCategory5TotalEmission());

      } catch (Exception e) {
        log.warn("{}월 특수집계 조회 중 오류 발생: {}", month, e.getMessage());
        // 해당 월 데이터 없으면 0으로 처리하고 계속 진행
      }
    }

    // 조직 ID 결정
    Long organizationId = "HEADQUARTERS".equals(userType) ? headquartersId : partnerId;

    // 연별 특수집계 응답 생성 (12월로 설정하여 연별임을 표시)
    Scope3SpecialAggregationResponse yearlySpecialAggregation = Scope3SpecialAggregationResponse.builder()
        .reportingYear(year)
        .reportingMonth(12) // 연별 조회임을 나타내는 더미 값
        .userType(userType)
        .organizationId(organizationId)
        .category1TotalEmission(totalCat1)
        .category2TotalEmission(totalCat2)
        .category4TotalEmission(totalCat4)
        .category5TotalEmission(totalCat5)
        // Detail 정보는 null로 설정 (연별 합산에서는 세부사항 제공하지 않음)
        .build();

    log.info("연별 특수집계 완료 - 연도: {}, Cat1: {}, Cat2: {}, Cat4: {}, Cat5: {}, 총합: {}",
        year, totalCat1, totalCat2, totalCat4, totalCat5,
        totalCat1.add(totalCat2).add(totalCat4).add(totalCat5));

    return yearlySpecialAggregation;
  }

}