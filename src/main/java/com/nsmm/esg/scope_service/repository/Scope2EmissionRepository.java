package com.nsmm.esg.scope_service.repository;

import com.nsmm.esg.scope_service.entity.ScopeEmission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Scope 2 배출량 리포지토리
 *
 * 권한 기반 데이터 접근:
 * - 본사/협력사 구분 기반 권한 제어
 * - TreePath 기반 계층 구조 관리
 * - 연도/월별 데이터 조회
 *
 * @author ESG Project Team
 * @version 1.0
 */
@Repository
public interface Scope2EmissionRepository extends JpaRepository<ScopeEmission, Long> {

    // ========================================================================
    // 본사용 쿼리 메서드 (Headquarters Query Methods)
    // ========================================================================

    /**
     * 본사 ID로 모든 배출량 데이터 조회
     */
    List<ScopeEmission> findByHeadquartersId(Long headquartersId);

    /**
     * 본사 ID와 연도로 배출량 데이터 조회
     */
    List<ScopeEmission> findByHeadquartersIdAndReportingYear(Long headquartersId, Integer year);

    /**
     * 본사 ID와 카테고리 번호로 배출량 데이터 조회
     */
    List<ScopeEmission> findByHeadquartersIdAndScope2CategoryNumber(Long headquartersId, Integer scope2CategoryNumber);

    /**
     * 본사 ID와 제품 코드로 배출량 데이터 조회
     */
    List<ScopeEmission> findByHeadquartersIdAndCompanyProductCode(Long headquartersId, String companyProductCode);

    /**
     * 본사 ID와 연도/월/카테고리로 배출량 데이터 조회
     */
    List<ScopeEmission> findByHeadquartersIdAndReportingYearAndReportingMonthAndScope2CategoryNumber(
            Long headquartersId, Integer year, Integer month, Integer scope2CategoryNumber);

    /**
     * 본사 ID와 연도/월로 배출량 데이터 조회
     */
    List<ScopeEmission> findByHeadquartersIdAndReportingYearAndReportingMonth(
            Long headquartersId, Integer year, Integer month);

    /**
     * 본사 ID 기반 중복 데이터 검사
     */
    boolean existsByHeadquartersIdAndReportingYearAndReportingMonthAndCompanyProductCodeAndScope2CategoryNumberAndMajorCategoryAndSubcategoryAndRawMaterial(
            Long headquartersId,
            Integer reportingYear,
            Integer reportingMonth,
            String companyProductCode,
            Integer scope2CategoryNumber,
            String majorCategory,
            String subcategory,
            String rawMaterial);

    /**
     * 본사 ID 기반 중복 데이터 조회 (수정용 - 특정 ID 제외)
     */
    List<ScopeEmission> findByHeadquartersIdAndReportingYearAndReportingMonthAndCompanyProductCodeAndScope2CategoryNumberAndMajorCategoryAndSubcategoryAndRawMaterial(
            Long headquartersId,
            Integer reportingYear,
            Integer reportingMonth,
            String companyProductCode,
            Integer scope2CategoryNumber,
            String majorCategory,
            String subcategory,
            String rawMaterial);

    // ========================================================================
    // 협력사용 쿼리 메서드 (Partner Query Methods)
    // ========================================================================

    /**
     * 협력사 ID와 TreePath로 배출량 데이터 조회
     */
    List<ScopeEmission> findByPartnerIdAndTreePathStartingWith(Long partnerId, String treePath);

    /**
     * 협력사 ID, TreePath, 연도로 배출량 데이터 조회
     */
    List<ScopeEmission> findByPartnerIdAndTreePathStartingWithAndReportingYear(
            Long partnerId, String treePath, Integer year);

    /**
     * 협력사 ID, TreePath, 카테고리 번호로 배출량 데이터 조회
     */
    List<ScopeEmission> findByPartnerIdAndTreePathStartingWithAndScope2CategoryNumber(
            Long partnerId, String treePath, Integer scope2CategoryNumber);

    /**
     * 협력사 ID, TreePath, 연도/월/카테고리로 배출량 데이터 조회
     */
    List<ScopeEmission> findByPartnerIdAndTreePathStartingWithAndReportingYearAndReportingMonthAndScope2CategoryNumber(
            Long partnerId, String treePath, Integer year, Integer month, Integer scope2CategoryNumber);

    /**
     * 협력사 ID, TreePath, 연도/월로 배출량 데이터 조회
     */
    List<ScopeEmission> findByPartnerIdAndTreePathStartingWithAndReportingYearAndReportingMonth(
            Long partnerId, String treePath, Integer year, Integer month);

    /**
     * 협력사 ID 기반 중복 데이터 검사
     */
    boolean existsByPartnerIdAndReportingYearAndReportingMonthAndCompanyProductCodeAndScope2CategoryNumberAndMajorCategoryAndSubcategoryAndRawMaterial(
            Long partnerId,
            Integer reportingYear,
            Integer reportingMonth,
            String companyProductCode,
            Integer scope2CategoryNumber,
            String majorCategory,
            String subcategory,
            String rawMaterial);

    /**
     * 협력사 ID 기반 중복 데이터 조회 (수정용 - 특정 ID 제외)
     */
    List<ScopeEmission> findByPartnerIdAndReportingYearAndReportingMonthAndCompanyProductCodeAndScope2CategoryNumberAndMajorCategoryAndSubcategoryAndRawMaterial(
            Long partnerId,
            Integer reportingYear,
            Integer reportingMonth,
            String companyProductCode,
            Integer scope2CategoryNumber,
            String majorCategory,
            String subcategory,
            String rawMaterial);

    // ========================================================================
    // TreePath 기반 쿼리 메서드 (TreePath-based Query Methods)
    // ========================================================================

    /**
     * TreePath로 시작하는 모든 배출량 데이터 조회
     */
    @Query("SELECT s FROM ScopeEmission s WHERE s.treePath LIKE :treePath%")
    List<ScopeEmission> findByTreePathStartingWith(@Param("treePath") String treePath);

    /**
     * TreePath로 시작하는 모든 배출량 데이터 조회 (페이지네이션)
     */
    @Query("SELECT s FROM ScopeEmission s WHERE s.treePath LIKE :treePath% ORDER BY s.reportingYear DESC, s.reportingMonth DESC")
    Page<ScopeEmission> findByTreePathStartingWith(@Param("treePath") String treePath, Pageable pageable);

    // ========================================================================
    // 집계 쿼리 메서드 (Aggregation Query Methods)
    // ========================================================================

    /**
     * 본사 기준 연도/월별 카테고리 총계
     */
    @Query("SELECT e.scope2CategoryNumber, SUM(e.totalEmission) FROM ScopeEmission e " +
            "WHERE e.headquartersId = :headquartersId " +
            "AND e.reportingYear = :year " +
            "AND e.reportingMonth = :month " +
            "GROUP BY e.scope2CategoryNumber")
    List<Object[]> sumTotalEmissionByCategoryAndYearAndMonthForHeadquarters(
            @Param("headquartersId") Long headquartersId,
            @Param("year") Integer year,
            @Param("month") Integer month);

    /**
     * 본사 기준 연도별 카테고리 총계
     */
    @Query("SELECT e.scope2CategoryNumber, SUM(e.totalEmission) FROM ScopeEmission e " +
            "WHERE e.headquartersId = :headquartersId " +
            "AND e.reportingYear = :year " +
            "GROUP BY e.scope2CategoryNumber")
    List<Object[]> sumTotalEmissionByCategoryAndYearForHeadquarters(
            @Param("headquartersId") Long headquartersId,
            @Param("year") Integer year);

    /**
     * 협력사 기준 연도/월별 카테고리 총계
     */
    @Query("SELECT e.scope2CategoryNumber, SUM(e.totalEmission) FROM ScopeEmission e " +
            "WHERE e.partnerId = :partnerId " +
            "AND e.treePath LIKE CONCAT(:treePath, '%') " +
            "AND e.reportingYear = :year " +
            "AND e.reportingMonth = :month " +
            "GROUP BY e.scope2CategoryNumber")
    List<Object[]> sumTotalEmissionByCategoryAndYearAndMonthForPartner(
            @Param("partnerId") Long partnerId,
            @Param("treePath") String treePath,
            @Param("year") Integer year,
            @Param("month") Integer month);

    /**
     * 협력사 기준 연도별 카테고리 총계
     */
    @Query("SELECT e.scope2CategoryNumber, SUM(e.totalEmission) FROM ScopeEmission e " +
            "WHERE e.partnerId = :partnerId " +
            "AND e.treePath LIKE CONCAT(:treePath, '%') " +
            "AND e.reportingYear = :year " +
            "GROUP BY e.scope2CategoryNumber")
    List<Object[]> sumTotalEmissionByCategoryAndYearForPartner(
            @Param("partnerId") Long partnerId,
            @Param("treePath") String treePath,
            @Param("year") Integer year);
}