package com.nsmm.esg.scope_service.repository;

import com.nsmm.esg.scope_service.entity.ScopeEmission;
import com.nsmm.esg.scope_service.enums.ScopeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

/**
 * ScopeEmission 엔티티의 데이터베이스 접근 레포지토리
 *
 * 주요 기능:
 * - 기본 CRUD 작업
 * - 계층적 배출량 집계 쿼리
 * - Scope별/카테고리별 집계 쿼리
 * - 제품별 집계 쿼리
 * - 그룹별 집계 쿼리 (Scope 3 특수 집계용)
 *
 * @author ESG Project Team
 * @version 1.0
 * @since 2024
 */
@Repository
public interface ScopeEmissionRepository extends JpaRepository<ScopeEmission, Long> {

        // 본사 본인 데이터만 조회 (협력사 데이터 제외)
        List<ScopeEmission> findByHeadquartersIdAndPartnerIdIsNullAndScopeType(Long headquartersId, ScopeType scopeType);
        
        // 특정 협력사 데이터만 조회
        List<ScopeEmission> findByPartnerIdAndScopeType(Long partnerId, ScopeType scopeType);


        // ========================================================================
        // 기본 집계 쿼리 (Basic Aggregation Queries)
        // ========================================================================

        // 본사 기준 ScopeType/연/월별 총 배출량 합계
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

        // 협력사 기준 ScopeType/연/월별 총 배출량 합계 (트리 경로 기반)
        @Query("SELECT COALESCE(SUM(s.totalEmission), 0) FROM ScopeEmission s " +
                        "WHERE s.headquartersId = :headquartersId " +
                        "AND s.treePath LIKE CONCAT(:treePath, '%') " +
                        "AND s.scopeType = :scopeType " +
                        "AND s.reportingYear = :year " +
                        "AND s.reportingMonth = :month")
        BigDecimal sumTotalEmissionByScopeTypeAndTreePathForPartner(
                        @Param("headquartersId") Long headquartersId,
                        @Param("treePath") String treePath,
                        @Param("scopeType") ScopeType scopeType,
                        @Param("year") Integer year,
                        @Param("month") Integer month);

        // ========================================================================
        // Scope 3 카테고리별 집계 쿼리 (Scope 3 Category Aggregation)
        // ========================================================================

        // 본사 기준 Scope3 카테고리별 연간 배출량 합계 (카테고리별 그룹)
        @Query("SELECT s.scope3CategoryNumber, COALESCE(SUM(s.totalEmission), 0) FROM ScopeEmission s " +
                        "WHERE s.headquartersId = :headquartersId " +
                        "AND s.scopeType = 'SCOPE3' " +
                        "AND s.reportingYear = :year " +
                        "GROUP BY s.scope3CategoryNumber")
        List<Object[]> sumTotalEmissionByScope3CategoryAndYearForHeadquarters(
                        @Param("headquartersId") Long headquartersId,
                        @Param("year") Integer year);

        // 협력사 기준 Scope3 카테고리별 연간 배출량 합계 (카테고리별 그룹)
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



        // ========================================================================
        // 카테고리별 연간/월간 집계 쿼리 (Category Yearly/Monthly Aggregation)
        // ========================================================================

        /**
         * Scope1 카테고리별 연간 배출량 집계 (본사 기준)
         * 반환값: [categoryNumber, totalEmission, dataCount]
         */
        @Query("SELECT s.scope1CategoryNumber, " +
               "COALESCE(SUM(s.totalEmission), 0), " +
               "COUNT(s) " +
               "FROM ScopeEmission s " +
               "WHERE s.headquartersId = :headquartersId " +
               "AND s.scopeType = 'SCOPE1' " +
               "AND s.reportingYear = :year " +
               "AND s.scope1CategoryNumber IS NOT NULL " +
               "GROUP BY s.scope1CategoryNumber " +
               "ORDER BY s.scope1CategoryNumber")
        List<Object[]> sumScope1EmissionByYearAndCategoryForHeadquarters(
                @Param("headquartersId") Long headquartersId,
                @Param("year") Integer year);

        /**
         * Scope1 카테고리별 연간 배출량 집계 (협력사 기준)
         * 반환값: [categoryNumber, totalEmission, dataCount]
         */
        @Query("SELECT s.scope1CategoryNumber, " +
               "COALESCE(SUM(s.totalEmission), 0), " +
               "COUNT(s) " +
               "FROM ScopeEmission s " +
               "WHERE s.headquartersId = :headquartersId " +
               "AND s.treePath LIKE CONCAT(:treePath, '%') " +
               "AND s.scopeType = 'SCOPE1' " +
               "AND s.reportingYear = :year " +
               "AND s.scope1CategoryNumber IS NOT NULL " +
               "GROUP BY s.scope1CategoryNumber " +
               "ORDER BY s.scope1CategoryNumber")
        List<Object[]> sumScope1EmissionByYearAndCategoryForPartner(
                @Param("headquartersId") Long headquartersId,
                @Param("treePath") String treePath,
                @Param("year") Integer year);

        /**
         * Scope2 카테고리별 연간 배출량 집계 (본사 기준)
         * 반환값: [categoryNumber, totalEmission, dataCount]
         */
        @Query("SELECT s.scope2CategoryNumber, " +
               "COALESCE(SUM(s.totalEmission), 0), " +
               "COUNT(s) " +
               "FROM ScopeEmission s " +
               "WHERE s.headquartersId = :headquartersId " +
               "AND s.scopeType = 'SCOPE2' " +
               "AND s.reportingYear = :year " +
               "AND s.scope2CategoryNumber IS NOT NULL " +
               "GROUP BY s.scope2CategoryNumber " +
               "ORDER BY s.scope2CategoryNumber")
        List<Object[]> sumScope2EmissionByYearAndCategoryForHeadquarters(
                @Param("headquartersId") Long headquartersId,
                @Param("year") Integer year);

        /**
         * Scope2 카테고리별 연간 배출량 집계 (협력사 기준)
         * 반환값: [categoryNumber, totalEmission, dataCount]
         */
        @Query("SELECT s.scope2CategoryNumber, " +
               "COALESCE(SUM(s.totalEmission), 0), " +
               "COUNT(s) " +
               "FROM ScopeEmission s " +
               "WHERE s.headquartersId = :headquartersId " +
               "AND s.treePath LIKE CONCAT(:treePath, '%') " +
               "AND s.scopeType = 'SCOPE2' " +
               "AND s.reportingYear = :year " +
               "AND s.scope2CategoryNumber IS NOT NULL " +
               "GROUP BY s.scope2CategoryNumber " +
               "ORDER BY s.scope2CategoryNumber")
        List<Object[]> sumScope2EmissionByYearAndCategoryForPartner(
                @Param("headquartersId") Long headquartersId,
                @Param("treePath") String treePath,
                @Param("year") Integer year);

        /**
         * Scope3 카테고리별 연간 배출량 집계 (본사 기준)
         * 반환값: [categoryNumber, totalEmission, dataCount]
         */
        @Query("SELECT s.scope3CategoryNumber, " +
               "COALESCE(SUM(s.totalEmission), 0), " +
               "COUNT(s) " +
               "FROM ScopeEmission s " +
               "WHERE s.headquartersId = :headquartersId " +
               "AND s.scopeType = 'SCOPE3' " +
               "AND s.reportingYear = :year " +
               "AND s.scope3CategoryNumber IS NOT NULL " +
               "GROUP BY s.scope3CategoryNumber " +
               "ORDER BY s.scope3CategoryNumber")
        List<Object[]> sumScope3EmissionByYearAndCategoryForHeadquarters(
                @Param("headquartersId") Long headquartersId,
                @Param("year") Integer year);

        /**
         * Scope3 카테고리별 연간 배출량 집계 (협력사 기준)
         * 반환값: [categoryNumber, totalEmission, dataCount]
         */
        @Query("SELECT s.scope3CategoryNumber, " +
               "COALESCE(SUM(s.totalEmission), 0), " +
               "COUNT(s) " +
               "FROM ScopeEmission s " +
               "WHERE s.headquartersId = :headquartersId " +
               "AND s.treePath LIKE CONCAT(:treePath, '%') " +
               "AND s.scopeType = 'SCOPE3' " +
               "AND s.reportingYear = :year " +
               "AND s.scope3CategoryNumber IS NOT NULL " +
               "GROUP BY s.scope3CategoryNumber " +
               "ORDER BY s.scope3CategoryNumber")
        List<Object[]> sumScope3EmissionByYearAndCategoryForPartner(
                @Param("headquartersId") Long headquartersId,
                @Param("treePath") String treePath,
                @Param("year") Integer year);

        /**
         * Scope1 카테고리별 월간 배출량 집계 (본사 기준) - 연도의 모든 월
         * 반환값: [categoryNumber, month, totalEmission, dataCount]
         */
        @Query("SELECT s.scope1CategoryNumber, " +
               "s.reportingMonth, " +
               "COALESCE(SUM(s.totalEmission), 0), " +
               "COUNT(s) " +
               "FROM ScopeEmission s " +
               "WHERE s.headquartersId = :headquartersId " +
               "AND s.scopeType = 'SCOPE1' " +
               "AND s.reportingYear = :year " +
               "AND s.scope1CategoryNumber IS NOT NULL " +
               "GROUP BY s.scope1CategoryNumber, s.reportingMonth " +
               "ORDER BY s.scope1CategoryNumber, s.reportingMonth")
        List<Object[]> sumScope1EmissionByYearAndMonthAndCategoryForHeadquarters(
                @Param("headquartersId") Long headquartersId,
                @Param("year") Integer year);

        /**
         * Scope1 카테고리별 월간 배출량 집계 (협력사 기준) - 연도의 모든 월
         * 반환값: [categoryNumber, month, totalEmission, dataCount]
         */
        @Query("SELECT s.scope1CategoryNumber, " +
               "s.reportingMonth, " +
               "COALESCE(SUM(s.totalEmission), 0), " +
               "COUNT(s) " +
               "FROM ScopeEmission s " +
               "WHERE s.headquartersId = :headquartersId " +
               "AND s.treePath LIKE CONCAT(:treePath, '%') " +
               "AND s.scopeType = 'SCOPE1' " +
               "AND s.reportingYear = :year " +
               "AND s.scope1CategoryNumber IS NOT NULL " +
               "GROUP BY s.scope1CategoryNumber, s.reportingMonth " +
               "ORDER BY s.scope1CategoryNumber, s.reportingMonth")
        List<Object[]> sumScope1EmissionByYearAndMonthAndCategoryForPartner(
                @Param("headquartersId") Long headquartersId,
                @Param("treePath") String treePath,
                @Param("year") Integer year);

        /**
         * Scope2 카테고리별 월간 배출량 집계 (본사 기준) - 연도의 모든 월
         * 반환값: [categoryNumber, month, totalEmission, dataCount]
         */
        @Query("SELECT s.scope2CategoryNumber, " +
               "s.reportingMonth, " +
               "COALESCE(SUM(s.totalEmission), 0), " +
               "COUNT(s) " +
               "FROM ScopeEmission s " +
               "WHERE s.headquartersId = :headquartersId " +
               "AND s.scopeType = 'SCOPE2' " +
               "AND s.reportingYear = :year " +
               "AND s.scope2CategoryNumber IS NOT NULL " +
               "GROUP BY s.scope2CategoryNumber, s.reportingMonth " +
               "ORDER BY s.scope2CategoryNumber, s.reportingMonth")
        List<Object[]> sumScope2EmissionByYearAndMonthAndCategoryForHeadquarters(
                @Param("headquartersId") Long headquartersId,
                @Param("year") Integer year);

        /**
         * Scope2 카테고리별 월간 배출량 집계 (협력사 기준) - 연도의 모든 월
         * 반환값: [categoryNumber, month, totalEmission, dataCount]
         */
        @Query("SELECT s.scope2CategoryNumber, " +
               "s.reportingMonth, " +
               "COALESCE(SUM(s.totalEmission), 0), " +
               "COUNT(s) " +
               "FROM ScopeEmission s " +
               "WHERE s.headquartersId = :headquartersId " +
               "AND s.treePath LIKE CONCAT(:treePath, '%') " +
               "AND s.scopeType = 'SCOPE2' " +
               "AND s.reportingYear = :year " +
               "AND s.scope2CategoryNumber IS NOT NULL " +
               "GROUP BY s.scope2CategoryNumber, s.reportingMonth " +
               "ORDER BY s.scope2CategoryNumber, s.reportingMonth")
        List<Object[]> sumScope2EmissionByYearAndMonthAndCategoryForPartner(
                @Param("headquartersId") Long headquartersId,
                @Param("treePath") String treePath,
                @Param("year") Integer year);

        /**
         * Scope3 카테고리별 월간 배출량 집계 (본사 기준) - 연도의 모든 월
         * 반환값: [categoryNumber, month, totalEmission, dataCount]
         */
        @Query("SELECT s.scope3CategoryNumber, " +
               "s.reportingMonth, " +
               "COALESCE(SUM(s.totalEmission), 0), " +
               "COUNT(s) " +
               "FROM ScopeEmission s " +
               "WHERE s.headquartersId = :headquartersId " +
               "AND s.scopeType = 'SCOPE3' " +
               "AND s.reportingYear = :year " +
               "AND s.scope3CategoryNumber IS NOT NULL " +
               "GROUP BY s.scope3CategoryNumber, s.reportingMonth " +
               "ORDER BY s.scope3CategoryNumber, s.reportingMonth")
        List<Object[]> sumScope3EmissionByYearAndMonthAndCategoryForHeadquarters(
                @Param("headquartersId") Long headquartersId,
                @Param("year") Integer year);

        /**
         * Scope3 카테고리별 월간 배출량 집계 (협력사 기준) - 연도의 모든 월
         * 반환값: [categoryNumber, month, totalEmission, dataCount]
         */
        @Query("SELECT s.scope3CategoryNumber, " +
               "s.reportingMonth, " +
               "COALESCE(SUM(s.totalEmission), 0), " +
               "COUNT(s) " +
               "FROM ScopeEmission s " +
               "WHERE s.headquartersId = :headquartersId " +
               "AND s.treePath LIKE CONCAT(:treePath, '%') " +
               "AND s.scopeType = 'SCOPE3' " +
               "AND s.reportingYear = :year " +
               "AND s.scope3CategoryNumber IS NOT NULL " +
               "GROUP BY s.scope3CategoryNumber, s.reportingMonth " +
               "ORDER BY s.scope3CategoryNumber, s.reportingMonth")
        List<Object[]> sumScope3EmissionByYearAndMonthAndCategoryForPartner(
                @Param("headquartersId") Long headquartersId,
                @Param("treePath") String treePath,
                @Param("year") Integer year);

        // ========================================================================
        // 협력사별 월별 집계 쿼리 (Partner Monthly Aggregation)
        // ========================================================================

        /**
         * 협력사별 ScopeType/연/월별 총 배출량 합계
         */
        @Query("SELECT COALESCE(SUM(s.totalEmission), 0) FROM ScopeEmission s " +
                        "WHERE s.headquartersId = :headquartersId " +
                        "AND s.partnerId = :partnerId " +
                        "AND s.scopeType = :scopeType " +
                        "AND s.reportingYear = :year " +
                        "AND s.reportingMonth = :month")
        BigDecimal sumTotalEmissionByScopeTypeAndPartnerAndYearAndMonth(
                        @Param("headquartersId") Long headquartersId,
                        @Param("partnerId") Long partnerId,
                        @Param("scopeType") ScopeType scopeType,
                        @Param("year") Integer year,
                        @Param("month") Integer month);

        /**
         * 협력사별 연/월별 배출량 데이터 건수
         */
        @Query("SELECT COUNT(s) FROM ScopeEmission s " +
                        "WHERE s.headquartersId = :headquartersId " +
                        "AND s.partnerId = :partnerId " +
                        "AND s.reportingYear = :year " +
                        "AND s.reportingMonth = :month")
        Long countEmissionsByPartnerAndYearAndMonth(
                        @Param("headquartersId") Long headquartersId,
                        @Param("partnerId") Long partnerId,
                        @Param("year") Integer year,
                        @Param("month") Integer month);

        /**
         * 본사 연/월별 배출량 데이터 건수
         */
        @Query("SELECT COUNT(s) FROM ScopeEmission s " +
                        "WHERE s.headquartersId = :headquartersId " +
                        "AND s.reportingYear = :year " +
                        "AND s.reportingMonth = :month")
        Long countEmissionsByHeadquartersAndYearAndMonth(
                        @Param("headquartersId") Long headquartersId,
                        @Param("year") Integer year,
                        @Param("month") Integer month);
}