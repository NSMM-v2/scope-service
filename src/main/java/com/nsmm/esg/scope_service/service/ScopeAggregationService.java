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
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ScopeAggregationService {

  private final ScopeEmissionRepository scopeEmissionRepository;

  // ========================================================================
  // 협력사별 월별 집계 메서드 (Partner Monthly Aggregation)
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

}