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
import java.util.Optional;

/**
 * Scope 배출량 리포지토리 (Scope1/2/3 통합)
 * 
 * 특징:
 * - TreePath 기반 권한 제어
 * - 연도/월별 데이터 조회
 * - Scope 타입별 필터링
 * - 카테고리별 집계 지원
 * 
 * @author ESG Project Team
 * @version 1.0
 */
@Repository
public interface ScopeEmissionRepository extends JpaRepository<ScopeEmission, Long> {

        // ========================================================================
        // 기본 조회 메서드 (Basic Query Methods)
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
         * 본사 ID와 연도/월로 배출량 데이터 조회
         */
        List<ScopeEmission> findByHeadquartersIdAndReportingYearAndReportingMonth(
                        Long headquartersId, Integer year, Integer month);

        /**
         * TreePath로 배출량 데이터 조회 (권한 기반)
         */
        List<ScopeEmission> findByTreePathStartingWith(String treePath);

        // ========================================================================
        // Scope 타입별 조회 메서드 (Scope Type Specific Queries)
        // ========================================================================

        /**
         * 본사 ID와 Scope 타입으로 배출량 데이터 조회
         */
        List<ScopeEmission> findByHeadquartersIdAndScopeType(Long headquartersId, ScopeType scopeType);

        /**
         * 본사 ID, 연도, Scope 타입으로 배출량 데이터 조회
         */
        List<ScopeEmission> findByHeadquartersIdAndReportingYearAndScopeType(
                        Long headquartersId, Integer year, ScopeType scopeType);

        /**
         * 본사 ID, 연도, 월, Scope 타입으로 배출량 데이터 조회
         */
        List<ScopeEmission> findByHeadquartersIdAndReportingYearAndReportingMonthAndScopeType(
                        Long headquartersId, Integer year, Integer month, ScopeType scopeType);

        // ========================================================================
        // 카테고리별 조회 메서드 (Category Specific Queries)
        // ========================================================================

        /**
         * Scope 1 카테고리별 조회
         */
        List<ScopeEmission> findByHeadquartersIdAndScopeTypeAndScope1CategoryNumber(
                        Long headquartersId, ScopeType scopeType, Integer categoryNumber);

        /**
         * Scope 2 카테고리별 조회
         */
        List<ScopeEmission> findByHeadquartersIdAndScopeTypeAndScope2CategoryNumber(
                        Long headquartersId, ScopeType scopeType, Integer categoryNumber);

        /**
         * Scope 3 카테고리별 조회
         */
        List<ScopeEmission> findByHeadquartersIdAndScopeTypeAndScope3CategoryNumber(
                        Long headquartersId, ScopeType scopeType, Integer categoryNumber);

        // ========================================================================
        // 제품 코드별 조회 메서드 (Product Code Specific Queries)
        // ========================================================================

        /**
         * 제품 코드로 배출량 데이터 조회 (Scope 1, 2에서만 사용)
         */
        List<ScopeEmission> findByCompanyProductCodeAndReportingYearAndReportingMonth(
                        String companyProductCode, Integer year, Integer month);

        /**
         * 본사 ID와 제품 코드로 배출량 데이터 조회
         */
        List<ScopeEmission> findByHeadquartersIdAndCompanyProductCodeAndReportingYearAndReportingMonth(
                        Long headquartersId, String companyProductCode, Integer year, Integer month);

        // ========================================================================
        // 페이징 조회 메서드 (Paging Query Methods)
        // ========================================================================

        /**
         * 본사 ID로 페이징 조회
         */
        Page<ScopeEmission> findByHeadquartersId(Long headquartersId, Pageable pageable);

        /**
         * 본사 ID와 Scope 타입으로 페이징 조회
         */
        Page<ScopeEmission> findByHeadquartersIdAndScopeType(
                        Long headquartersId, ScopeType scopeType, Pageable pageable);

        // ========================================================================
        // 집계 쿼리 메서드 (Aggregation Query Methods)
        // ========================================================================

        /**
         * 본사 ID와 연도별 총 배출량 집계
         */
        @Query("SELECT SUM(s.totalEmission) FROM ScopeEmission s " +
                        "WHERE s.headquartersId = :headquartersId AND s.reportingYear = :year")
        Optional<BigDecimal> sumTotalEmissionByHeadquartersIdAndYear(
                        @Param("headquartersId") Long headquartersId,
                        @Param("year") Integer year);

        /**
         * 본사 ID, 연도, Scope 타입별 총 배출량 집계
         */
        @Query("SELECT SUM(s.totalEmission) FROM ScopeEmission s " +
                        "WHERE s.headquartersId = :headquartersId AND s.reportingYear = :year " +
                        "AND s.scopeType = :scopeType")
        Optional<BigDecimal> sumTotalEmissionByHeadquartersIdAndYearAndScopeType(
                        @Param("headquartersId") Long headquartersId,
                        @Param("year") Integer year,
                        @Param("scopeType") ScopeType scopeType);

        /**
         * TreePath 기반 하위 조직 배출량 집계
         */
        @Query("SELECT SUM(s.totalEmission) FROM ScopeEmission s " +
                        "WHERE s.treePath LIKE :treePath% AND s.reportingYear = :year " +
                        "AND s.reportingMonth = :month")
        Optional<BigDecimal> sumTotalEmissionByTreePathAndYearAndMonth(
                        @Param("treePath") String treePath,
                        @Param("year") Integer year,
                        @Param("month") Integer month);

        // ========================================================================
        // 중복 검증 메서드 (Duplicate Validation Methods)
        // ========================================================================

        /**
         * 동일 조건의 배출량 데이터 존재 여부 확인
         */
        boolean existsByHeadquartersIdAndScopeTypeAndScope1CategoryNumberAndReportingYearAndReportingMonth(
                        Long headquartersId, ScopeType scopeType, Integer scope1CategoryNumber, Integer reportingYear,
                        Integer reportingMonth);

        boolean existsByHeadquartersIdAndScopeTypeAndScope2CategoryNumberAndReportingYearAndReportingMonth(
                        Long headquartersId, ScopeType scopeType, Integer scope2CategoryNumber, Integer reportingYear,
                        Integer reportingMonth);

        boolean existsByHeadquartersIdAndScopeTypeAndScope3CategoryNumberAndReportingYearAndReportingMonth(
                        Long headquartersId, ScopeType scopeType, Integer scope3CategoryNumber, Integer reportingYear,
                        Integer reportingMonth);

        /**
         * 제품 코드별 중복 확인 (Scope 1, 2에서만 사용)
         */
        boolean existsByCompanyProductCodeAndReportingYearAndReportingMonth(
                        String companyProductCode, Integer year, Integer month);
}