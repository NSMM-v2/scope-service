package com.nsmm.esg.scope_service.repository;

import com.nsmm.esg.scope_service.entity.ScopeEmission;
import com.nsmm.esg.scope_service.entity.ScopeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

/**
 * 통합 Scope 배출량 리포지토리 - Scope 1, 2, 3 통합 지원
 * 
 * 주요 기능:
 * - 모든 Scope 타입 배출량 데이터 접근
 * - 권한 기반 데이터 조회 (본사/협력사 구분)
 * - TreePath 기반 계층 구조 관리
 * - 연도/월별 데이터 조회 및 집계
 * - 중복 데이터 검증 쿼리
 * 
 * 비즈니스 규칙:
 * - 본사: 모든 하위 조직 데이터 접근 가능
 * - 협력사: 본인 및 하위 조직 데이터만 접근 가능
 * - Scope 타입별 필터링 지원
 * - 제품 코드/제품명 기반 검색 (Scope 1, 2)
 * 
 * @author ESG Project Team
 * @version 1.0
 * @since 2024
 */
@Repository
public interface ScopeEmissionRepository extends JpaRepository<ScopeEmission, Long> {

        // ========================================================================
        // 본사용 기본 쿼리 메서드 (Headquarters Basic Query Methods)
        // ========================================================================

        /**
         * 본사 ID로 모든 배출량 데이터 조회
         */
        List<ScopeEmission> findByHeadquartersId(Long headquartersId);

        /**
         * 본사 ID로 모든 배출량 데이터 조회 (페이지네이션)
         */
        Page<ScopeEmission> findByHeadquartersId(Long headquartersId, Pageable pageable);

        /**
         * 본사 ID와 Scope 타입으로 배출량 데이터 조회
         */
        List<ScopeEmission> findByHeadquartersIdAndScopeType(Long headquartersId, ScopeType scopeType);

        /**
         * 본사 ID와 연도로 배출량 데이터 조회
         */
        List<ScopeEmission> findByHeadquartersIdAndReportingYear(Long headquartersId, Integer year);

        /**
         * 본사 ID와 연도/월로 배출량 데이터 조회
         */
        List<ScopeEmission> findByHeadquartersIdAndReportingYearAndReportingMonth(
                        Long headquartersId, Integer year, Integer month);

        /**
         * 본사 ID, Scope 타입, 연도로 배출량 데이터 조회
         */
        List<ScopeEmission> findByHeadquartersIdAndScopeTypeAndReportingYear(
                        Long headquartersId, ScopeType scopeType, Integer year);

        // ========================================================================
        // 협력사용 기본 쿼리 메서드 (Partner Basic Query Methods)
        // ========================================================================

        /**
         * 협력사 ID와 TreePath로 배출량 데이터 조회
         */
        List<ScopeEmission> findByPartnerIdAndTreePathStartingWith(Long partnerId, String treePath);

        /**
         * 협력사 ID와 TreePath로 배출량 데이터 조회 (페이지네이션)
         */
        Page<ScopeEmission> findByPartnerIdAndTreePathStartingWith(Long partnerId, String treePath, Pageable pageable);

        /**
         * 협력사 ID, TreePath, Scope 타입으로 배출량 데이터 조회
         */
        List<ScopeEmission> findByPartnerIdAndTreePathStartingWithAndScopeType(
                        Long partnerId, String treePath, ScopeType scopeType);

        /**
         * 협력사 ID, TreePath, 연도로 배출량 데이터 조회
         */
        List<ScopeEmission> findByPartnerIdAndTreePathStartingWithAndReportingYear(
                        Long partnerId, String treePath, Integer year);

        /**
         * 협력사 ID, TreePath, 연도/월로 배출량 데이터 조회
         */
        List<ScopeEmission> findByPartnerIdAndTreePathStartingWithAndReportingYearAndReportingMonth(
                        Long partnerId, String treePath, Integer year, Integer month);

        /**
         * 협력사 ID, TreePath, Scope 타입, 연도로 배출량 데이터 조회
         */
        List<ScopeEmission> findByPartnerIdAndTreePathStartingWithAndScopeTypeAndReportingYear(
                        Long partnerId, String treePath, ScopeType scopeType, Integer year);

        /**
         * 협력사 ID, TreePath, Scope 타입, 연도/월로 배출량 데이터 조회
         */
        List<ScopeEmission> findByPartnerIdAndTreePathStartingWithAndScopeTypeAndReportingYearAndReportingMonth(
                        Long partnerId, String treePath, ScopeType scopeType, Integer year, Integer month);

        // ========================================================================
        // 제품 기반 쿼리 메서드 (Product-based Query Methods) - Scope 1, 2 전용
        // ========================================================================

        /**
         * 본사 ID와 제품 코드로 배출량 데이터 조회
         */
        List<ScopeEmission> findByHeadquartersIdAndCompanyProductCode(Long headquartersId, String companyProductCode);

        /**
         * 본사 ID와 제품명으로 배출량 데이터 조회
         */
        List<ScopeEmission> findByHeadquartersIdAndProductNameContaining(Long headquartersId, String productName);

        /**
         * 협력사 ID, TreePath, 제품 코드로 배출량 데이터 조회
         */
        List<ScopeEmission> findByPartnerIdAndTreePathStartingWithAndCompanyProductCode(
                        Long partnerId, String treePath, String companyProductCode);

        /**
         * 협력사 ID, TreePath, 제품명으로 배출량 데이터 조회
         */
        List<ScopeEmission> findByPartnerIdAndTreePathStartingWithAndProductNameContaining(
                        Long partnerId, String treePath, String productName);

        // ========================================================================
        // 카테고리 기반 쿼리 메서드 (Category-based Query Methods)
        // ========================================================================

        /**
         * 본사 ID와 Scope 1 카테고리 번호로 배출량 데이터 조회
         */
        List<ScopeEmission> findByHeadquartersIdAndScope1CategoryNumber(Long headquartersId,
                        Integer scope1CategoryNumber);

        /**
         * 본사 ID와 Scope 2 카테고리 번호로 배출량 데이터 조회
         */
        List<ScopeEmission> findByHeadquartersIdAndScope2CategoryNumber(Long headquartersId,
                        Integer scope2CategoryNumber);

        /**
         * 본사 ID와 Scope 3 카테고리 번호로 배출량 데이터 조회
         */
        List<ScopeEmission> findByHeadquartersIdAndScope3CategoryNumber(Long headquartersId,
                        Integer scope3CategoryNumber);

        /**
         * 협력사 ID, TreePath, Scope 1 카테고리 번호로 배출량 데이터 조회
         */
        List<ScopeEmission> findByPartnerIdAndTreePathStartingWithAndScope1CategoryNumber(
                        Long partnerId, String treePath, Integer scope1CategoryNumber);

        /**
         * 협력사 ID, TreePath, Scope 2 카테고리 번호로 배출량 데이터 조회
         */
        List<ScopeEmission> findByPartnerIdAndTreePathStartingWithAndScope2CategoryNumber(
                        Long partnerId, String treePath, Integer scope2CategoryNumber);

        /**
         * 협력사 ID, TreePath, Scope 3 카테고리 번호로 배출량 데이터 조회
         */
        List<ScopeEmission> findByPartnerIdAndTreePathStartingWithAndScope3CategoryNumber(
                        Long partnerId, String treePath, Integer scope3CategoryNumber);

        // ========================================================================
        // 중복 데이터 검증 쿼리 메서드 (Duplicate Validation Query Methods)
        // ========================================================================

        /**
         * 본사 기준 기본 중복 데이터 존재 여부 확인
         */
        boolean existsByHeadquartersIdAndScopeTypeAndReportingYearAndReportingMonth(
                        Long headquartersId, ScopeType scopeType, Integer reportingYear, Integer reportingMonth);

        /**
         * 협력사 기준 기본 중복 데이터 존재 여부 확인
         */
        boolean existsByPartnerIdAndScopeTypeAndReportingYearAndReportingMonth(
                        Long partnerId, ScopeType scopeType, Integer reportingYear, Integer reportingMonth);

        /**
         * 본사 기준 중복 데이터 조회 (수정용 - 특정 ID 제외)
         */
        List<ScopeEmission> findByHeadquartersIdAndScopeTypeAndReportingYearAndReportingMonth(
                        Long headquartersId, ScopeType scopeType, Integer reportingYear, Integer reportingMonth);

        /**
         * 협력사 기준 중복 데이터 조회 (수정용 - 특정 ID 제외)
         */
        List<ScopeEmission> findByPartnerIdAndScopeTypeAndReportingYearAndReportingMonth(
                        Long partnerId, ScopeType scopeType, Integer reportingYear, Integer reportingMonth);

        /**
         * 본사 기준 Scope 3 상세 중복 검증 (카테고리, 원재료 포함)
         */
        boolean existsByHeadquartersIdAndScopeTypeAndReportingYearAndReportingMonthAndScope3CategoryNumberAndMajorCategoryAndSubcategoryAndRawMaterial(
                        Long headquartersId, ScopeType scopeType, Integer reportingYear, Integer reportingMonth,
                        Integer scope3CategoryNumber, String majorCategory, String subcategory, String rawMaterial);

        /**
         * 협력사 기준 Scope 3 상세 중복 검증 (카테고리, 원재료 포함)
         */
        boolean existsByPartnerIdAndScopeTypeAndReportingYearAndReportingMonthAndScope3CategoryNumberAndMajorCategoryAndSubcategoryAndRawMaterial(
                        Long partnerId, ScopeType scopeType, Integer reportingYear, Integer reportingMonth,
                        Integer scope3CategoryNumber, String majorCategory, String subcategory, String rawMaterial);

        /**
         * 본사 기준 Scope 1, 2 제품 기반 중복 검증
         */
        boolean existsByHeadquartersIdAndScopeTypeAndReportingYearAndReportingMonthAndCompanyProductCode(
                        Long headquartersId, ScopeType scopeType, Integer reportingYear, Integer reportingMonth,
                        String companyProductCode);

        /**
         * 협력사 기준 Scope 1, 2 제품 기반 중복 검증
         */
        boolean existsByPartnerIdAndScopeTypeAndReportingYearAndReportingMonthAndCompanyProductCode(
                        Long partnerId, ScopeType scopeType, Integer reportingYear, Integer reportingMonth,
                        String companyProductCode);

        // ========================================================================
        // TreePath 기반 쿼리 메서드 (TreePath-based Query Methods)
        // ========================================================================

        /**
         * TreePath로 시작하는 모든 배출량 데이터 조회
         */
        @Query("SELECT s FROM ScopeEmission s WHERE s.treePath LIKE CONCAT(:treePath, '%')")
        List<ScopeEmission> findByTreePathStartingWith(@Param("treePath") String treePath);

        /**
         * TreePath로 시작하는 모든 배출량 데이터 조회 (페이지네이션)
         */
        @Query("SELECT s FROM ScopeEmission s WHERE s.treePath LIKE CONCAT(:treePath, '%') " +
                        "ORDER BY s.reportingYear DESC, s.reportingMonth DESC")
        Page<ScopeEmission> findByTreePathStartingWith(@Param("treePath") String treePath, Pageable pageable);

        /**
         * TreePath와 Scope 타입으로 배출량 데이터 조회
         */
        @Query("SELECT s FROM ScopeEmission s WHERE s.treePath LIKE CONCAT(:treePath, '%') " +
                        "AND s.scopeType = :scopeType")
        List<ScopeEmission> findByTreePathStartingWithAndScopeType(
                        @Param("treePath") String treePath, @Param("scopeType") ScopeType scopeType);

        // ========================================================================
        // 집계 쿼리 메서드 (Aggregation Query Methods)
        // ========================================================================

        /**
         * 본사 기준 Scope 타입별 연도별 총 배출량 집계
         */
        @Query("SELECT COALESCE(SUM(s.totalEmission), 0) FROM ScopeEmission s " +
                        "WHERE s.headquartersId = :headquartersId " +
                        "AND s.scopeType = :scopeType " +
                        "AND s.reportingYear = :year")
        BigDecimal sumTotalEmissionByScopeTypeAndYearForHeadquarters(
                        @Param("headquartersId") Long headquartersId,
                        @Param("scopeType") ScopeType scopeType,
                        @Param("year") Integer year);

        /**
         * 협력사 기준 Scope 타입별 연도별 총 배출량 집계
         */
        @Query("SELECT COALESCE(SUM(s.totalEmission), 0) FROM ScopeEmission s " +
                        "WHERE s.partnerId = :partnerId " +
                        "AND s.treePath LIKE CONCAT(:treePath, '%') " +
                        "AND s.scopeType = :scopeType " +
                        "AND s.reportingYear = :year")
        BigDecimal sumTotalEmissionByScopeTypeAndYearForPartner(
                        @Param("partnerId") Long partnerId,
                        @Param("treePath") String treePath,
                        @Param("scopeType") ScopeType scopeType,
                        @Param("year") Integer year);

        /**
         * 본사 기준 연도별 모든 Scope 타입 총 배출량 집계
         */
        @Query("SELECT s.scopeType, COALESCE(SUM(s.totalEmission), 0) FROM ScopeEmission s " +
                        "WHERE s.headquartersId = :headquartersId " +
                        "AND s.reportingYear = :year " +
                        "GROUP BY s.scopeType")
        List<Object[]> sumTotalEmissionByAllScopeTypesAndYearForHeadquarters(
                        @Param("headquartersId") Long headquartersId,
                        @Param("year") Integer year);

        /**
         * 협력사 기준 연도별 모든 Scope 타입 총 배출량 집계
         */
        @Query("SELECT s.scopeType, COALESCE(SUM(s.totalEmission), 0) FROM ScopeEmission s " +
                        "WHERE s.partnerId = :partnerId " +
                        "AND s.treePath LIKE CONCAT(:treePath, '%') " +
                        "AND s.reportingYear = :year " +
                        "GROUP BY s.scopeType")
        List<Object[]> sumTotalEmissionByAllScopeTypesAndYearForPartner(
                        @Param("partnerId") Long partnerId,
                        @Param("treePath") String treePath,
                        @Param("year") Integer year);

        /**
         * 본사 기준 연도/월별 Scope 타입별 총 배출량 집계
         */
        @Query("SELECT COALESCE(SUM(s.totalEmission), 0) FROM ScopeEmission s " +
                        "WHERE s.headquartersId = :headquartersId " +
                        "AND s.scopeType = :scopeType " +
                        "AND s.reportingYear = :year " +
                        "AND s.reportingMonth = :month")
        BigDecimal sumTotalEmissionByScopeTypeAndYearAndMonthForHeadquarters(
                        @Param("headquartersId") Long headquartersId,
                        @Param("scopeType") ScopeType scopeType,
                        @Param("year") Integer year,
                        @Param("month") Integer month);

        /**
         * 협력사 기준 연도/월별 Scope 타입별 총 배출량 집계
         */
        @Query("SELECT COALESCE(SUM(s.totalEmission), 0) FROM ScopeEmission s " +
                        "WHERE s.partnerId = :partnerId " +
                        "AND s.treePath LIKE CONCAT(:treePath, '%') " +
                        "AND s.scopeType = :scopeType " +
                        "AND s.reportingYear = :year " +
                        "AND s.reportingMonth = :month")
        BigDecimal sumTotalEmissionByScopeTypeAndYearAndMonthForPartner(
                        @Param("partnerId") Long partnerId,
                        @Param("treePath") String treePath,
                        @Param("scopeType") ScopeType scopeType,
                        @Param("year") Integer year,
                        @Param("month") Integer month);

        /**
         * 본사 기준 Scope 3 카테고리별 연도별 총 배출량 집계
         */
        @Query("SELECT s.scope3CategoryNumber, COALESCE(SUM(s.totalEmission), 0) FROM ScopeEmission s " +
                        "WHERE s.headquartersId = :headquartersId " +
                        "AND s.scopeType = 'SCOPE3' " +
                        "AND s.reportingYear = :year " +
                        "GROUP BY s.scope3CategoryNumber")
        List<Object[]> sumTotalEmissionByScope3CategoryAndYearForHeadquarters(
                        @Param("headquartersId") Long headquartersId,
                        @Param("year") Integer year);

        /**
         * 협력사 기준 Scope 3 카테고리별 연도별 총 배출량 집계
         */
        @Query("SELECT s.scope3CategoryNumber, COALESCE(SUM(s.totalEmission), 0) FROM ScopeEmission s " +
                        "WHERE s.partnerId = :partnerId " +
                        "AND s.treePath LIKE CONCAT(:treePath, '%') " +
                        "AND s.scopeType = 'SCOPE3' " +
                        "AND s.reportingYear = :year " +
                        "GROUP BY s.scope3CategoryNumber")
        List<Object[]> sumTotalEmissionByScope3CategoryAndYearForPartner(
                        @Param("partnerId") Long partnerId,
                        @Param("treePath") String treePath,
                        @Param("year") Integer year);

        /**
         * 본사 기준 제품별 연도별 총 배출량 집계 (Scope 1, 2 전용)
         */
        @Query("SELECT s.companyProductCode, s.productName, COALESCE(SUM(s.totalEmission), 0) FROM ScopeEmission s " +
                        "WHERE s.headquartersId = :headquartersId " +
                        "AND s.scopeType IN ('SCOPE1', 'SCOPE2') " +
                        "AND s.reportingYear = :year " +
                        "AND s.companyProductCode IS NOT NULL " +
                        "GROUP BY s.companyProductCode, s.productName")
        List<Object[]> sumTotalEmissionByProductAndYearForHeadquarters(
                        @Param("headquartersId") Long headquartersId,
                        @Param("year") Integer year);

        /**
         * 협력사 기준 제품별 연도별 총 배출량 집계 (Scope 1, 2 전용)
         */
        @Query("SELECT s.companyProductCode, s.productName, COALESCE(SUM(s.totalEmission), 0) FROM ScopeEmission s " +
                        "WHERE s.partnerId = :partnerId " +
                        "AND s.treePath LIKE CONCAT(:treePath, '%') " +
                        "AND s.scopeType IN ('SCOPE1', 'SCOPE2') " +
                        "AND s.reportingYear = :year " +
                        "AND s.companyProductCode IS NOT NULL " +
                        "GROUP BY s.companyProductCode, s.productName")
        List<Object[]> sumTotalEmissionByProductAndYearForPartner(
                        @Param("partnerId") Long partnerId,
                        @Param("treePath") String treePath,
                        @Param("year") Integer year);

        // ========================================================================
        // 통계 및 분석 쿼리 메서드 (Statistics and Analysis Query Methods)
        // ========================================================================

        /**
         * 본사 기준 연도별 월별 배출량 추이 (모든 Scope)
         */
        @Query("SELECT s.reportingMonth, s.scopeType, COALESCE(SUM(s.totalEmission), 0) FROM ScopeEmission s " +
                        "WHERE s.headquartersId = :headquartersId " +
                        "AND s.reportingYear = :year " +
                        "GROUP BY s.reportingMonth, s.scopeType " +
                        "ORDER BY s.reportingMonth, s.scopeType")
        List<Object[]> getMonthlyEmissionTrendByYearForHeadquarters(
                        @Param("headquartersId") Long headquartersId,
                        @Param("year") Integer year);

        /**
         * 협력사 기준 연도별 월별 배출량 추이 (모든 Scope)
         */
        @Query("SELECT s.reportingMonth, s.scopeType, COALESCE(SUM(s.totalEmission), 0) FROM ScopeEmission s " +
                        "WHERE s.partnerId = :partnerId " +
                        "AND s.treePath LIKE CONCAT(:treePath, '%') " +
                        "AND s.reportingYear = :year " +
                        "GROUP BY s.reportingMonth, s.scopeType " +
                        "ORDER BY s.reportingMonth, s.scopeType")
        List<Object[]> getMonthlyEmissionTrendByYearForPartner(
                        @Param("partnerId") Long partnerId,
                        @Param("treePath") String treePath,
                        @Param("year") Integer year);

        /**
         * 본사 기준 데이터 입력 현황 조회 (수동/자동 입력 구분)
         */
        @Query("SELECT s.scopeType, s.isManualInput, COUNT(s) FROM ScopeEmission s " +
                        "WHERE s.headquartersId = :headquartersId " +
                        "AND s.reportingYear = :year " +
                        "GROUP BY s.scopeType, s.isManualInput")
        List<Object[]> getDataInputStatusByYearForHeadquarters(
                        @Param("headquartersId") Long headquartersId,
                        @Param("year") Integer year);

        /**
         * 협력사 기준 데이터 입력 현황 조회 (수동/자동 입력 구분)
         */
        @Query("SELECT s.scopeType, s.isManualInput, COUNT(s) FROM ScopeEmission s " +
                        "WHERE s.partnerId = :partnerId " +
                        "AND s.treePath LIKE CONCAT(:treePath, '%') " +
                        "AND s.reportingYear = :year " +
                        "GROUP BY s.scopeType, s.isManualInput")
        List<Object[]> getDataInputStatusByYearForPartner(
                        @Param("partnerId") Long partnerId,
                        @Param("treePath") String treePath,
                        @Param("year") Integer year);
}