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

        // ========================================================================
        // 기본 조회 메서드 (Basic Query Methods)
        // ========================================================================

        // 본사ID + ScopeType으로 전체 조회
        List<ScopeEmission> findByHeadquartersIdAndScopeType(Long headquartersId, ScopeType scopeType);

        // ========================================================================
        // 제품 매핑 관련 조회 메서드 (Product Mapping Query Methods)
        // ========================================================================

        // 본사ID + 제품코드로 제품 매핑 데이터 조회
        List<ScopeEmission> findByHeadquartersIdAndHasProductMappingTrueAndCompanyProductCode(
                        Long headquartersId, String companyProductCode);

        // 협력사ID + 트리경로 + 제품코드로 제품 매핑 데이터 조회
        List<ScopeEmission> findByPartnerIdAndTreePathStartingWithAndHasProductMappingTrueAndCompanyProductCode(
                        Long partnerId, String treePath, String companyProductCode);

        // ========================================================================
        // 카테고리별 조회 메서드 (Category-based Query Methods)
        // ========================================================================

        // 본사ID + Scope1 카테고리 번호로 조회
        List<ScopeEmission> findByHeadquartersIdAndScope1CategoryNumber(
                        Long headquartersId, Integer scope1CategoryNumber);

        // 본사ID + Scope2 카테고리 번호로 조회
        List<ScopeEmission> findByHeadquartersIdAndScope2CategoryNumber(
                        Long headquartersId, Integer scope2CategoryNumber);

        // 본사ID + Scope3 카테고리 번호로 조회
        List<ScopeEmission> findByHeadquartersIdAndScope3CategoryNumber(
                        Long headquartersId, Integer scope3CategoryNumber);

        // 협력사ID + 트리경로 + Scope1 카테고리 번호로 조회
        List<ScopeEmission> findByPartnerIdAndTreePathStartingWithAndScope1CategoryNumber(
                        Long partnerId, String treePath, Integer scope1CategoryNumber);

        // 협력사ID + 트리경로 + Scope2 카테고리 번호로 조회
        List<ScopeEmission> findByPartnerIdAndTreePathStartingWithAndScope2CategoryNumber(
                        Long partnerId, String treePath, Integer scope2CategoryNumber);

        // 협력사ID + 트리경로 + Scope3 카테고리 번호로 조회
        List<ScopeEmission> findByPartnerIdAndTreePathStartingWithAndScope3CategoryNumber(
                        Long partnerId, String treePath, Integer scope3CategoryNumber);

        // 트리경로 + ScopeType으로 전체 조회
        List<ScopeEmission> findByTreePathStartingWithAndScopeType(String treePath, ScopeType scopeType);

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

        // 특정 Scope 3 카테고리의 배출량 합계 (본사 전체)
        @Query("SELECT COALESCE(SUM(s.totalEmission), 0) FROM ScopeEmission s " +
                        "WHERE s.headquartersId = :headquartersId " +
                        "AND s.scopeType = 'SCOPE3' " +
                        "AND s.scope3CategoryNumber = :categoryNumber " +
                        "AND s.reportingYear = :year " +
                        "AND s.reportingMonth = :month")
        BigDecimal sumScope3EmissionByCategory(
                        @Param("headquartersId") Long headquartersId,
                        @Param("categoryNumber") Integer categoryNumber,
                        @Param("year") Integer year,
                        @Param("month") Integer month);

        // 특정 Scope 3 카테고리의 배출량 합계 (협력사 트리 경로 기반)
        @Query("SELECT COALESCE(SUM(s.totalEmission), 0) FROM ScopeEmission s " +
                        "WHERE s.headquartersId = :headquartersId " +
                        "AND s.treePath LIKE CONCAT(:treePath, '%') " +
                        "AND s.scopeType = 'SCOPE3' " +
                        "AND s.scope3CategoryNumber = :categoryNumber " +
                        "AND s.reportingYear = :year " +
                        "AND s.reportingMonth = :month")
        BigDecimal sumScope3EmissionByCategoryForPartner(
                        @Param("headquartersId") Long headquartersId,
                        @Param("treePath") String treePath,
                        @Param("categoryNumber") Integer categoryNumber,
                        @Param("year") Integer year,
                        @Param("month") Integer month);

        // ========================================================================
        // Scope 1 그룹별 집계 쿼리 (Scope 1 Group Aggregation)
        // ========================================================================

        /**
         * Scope 1 그룹별 배출량 합계 (본사 전체)
         * 특정 그룹(고정연소, 이동연소, 공정배출, 냉매누출, 공장설비)의 배출량 집계
         */
        @Query("SELECT COALESCE(SUM(s.totalEmission), 0) FROM ScopeEmission s " +
                        "WHERE s.headquartersId = :headquartersId " +
                        "AND s.scopeType = 'SCOPE1' " +
                        "AND s.scope1CategoryGroup = :groupName " +
                        "AND s.reportingYear = :year " +
                        "AND s.reportingMonth = :month")
        BigDecimal sumScope1EmissionByGroup(
                        @Param("headquartersId") Long headquartersId,
                        @Param("groupName") String groupName,
                        @Param("year") Integer year,
                        @Param("month") Integer month);

        /**
         * Scope 1 그룹별 배출량 합계 (협력사 트리 경로 기반)
         * 특정 그룹(고정연소, 이동연소, 공정배출, 냉매누출, 공장설비)의 배출량 집계
         */
        @Query("SELECT COALESCE(SUM(s.totalEmission), 0) FROM ScopeEmission s " +
                        "WHERE s.headquartersId = :headquartersId " +
                        "AND s.treePath LIKE CONCAT(:treePath, '%') " +
                        "AND s.scopeType = 'SCOPE1' " +
                        "AND s.scope1CategoryGroup = :groupName " +
                        "AND s.reportingYear = :year " +
                        "AND s.reportingMonth = :month")
        BigDecimal sumScope1EmissionByGroupForPartner(
                        @Param("headquartersId") Long headquartersId,
                        @Param("treePath") String treePath,
                        @Param("groupName") String groupName,
                        @Param("year") Integer year,
                        @Param("month") Integer month);

        /**
         * Scope 1에서 특정 그룹들 제외한 배출량 합계
         * Scope 3 Cat.1 집계용 - 이동연소, 공장설비, 폐수처리 그룹 제외
         */
        @Query("SELECT COALESCE(SUM(s.totalEmission), 0) FROM ScopeEmission s " +
                        "WHERE s.headquartersId = :headquartersId " +
                        "AND s.scopeType = 'SCOPE1' " +
                        "AND s.scope1CategoryGroup NOT IN :excludedGroups " +
                        "AND s.reportingYear = :year " +
                        "AND s.reportingMonth = :month")
        BigDecimal sumScope1EmissionExcludingGroups(
                        @Param("headquartersId") Long headquartersId,
                        @Param("excludedGroups") List<String> excludedGroups,
                        @Param("year") Integer year,
                        @Param("month") Integer month);

        // ========================================================================
        // Factory Enabled 기반 집계 쿼리 (Factory Enabled Based Aggregation)
        // ========================================================================

        /**
         * Scope 1에서 공장설비 플래그 기반 배출량 합계 (본사 전체)
         * factoryEnabled=true인 데이터만 집계 (공장설비 그룹)
         */
        @Query("SELECT COALESCE(SUM(s.totalEmission), 0) FROM ScopeEmission s " +
                        "WHERE s.headquartersId = :headquartersId " +
                        "AND s.scopeType = 'SCOPE1' " +
                        "AND s.factoryEnabled = true " +
                        "AND s.reportingYear = :year " +
                        "AND s.reportingMonth = :month")
        BigDecimal sumScope1EmissionByFactoryEnabled(
                        @Param("headquartersId") Long headquartersId,
                        @Param("year") Integer year,
                        @Param("month") Integer month);

        /**
         * Scope 1에서 공장설비 플래그 기반 배출량 합계 (협력사 트리 경로 기반)
         * factoryEnabled=true인 데이터만 집계 (공장설비 그룹)
         */
        @Query("SELECT COALESCE(SUM(s.totalEmission), 0) FROM ScopeEmission s " +
                        "WHERE s.headquartersId = :headquartersId " +
                        "AND s.treePath LIKE CONCAT(:treePath, '%') " +
                        "AND s.scopeType = 'SCOPE1' " +
                        "AND s.factoryEnabled = true " +
                        "AND s.reportingYear = :year " +
                        "AND s.reportingMonth = :month")
        BigDecimal sumScope1EmissionByFactoryEnabledForPartner(
                        @Param("headquartersId") Long headquartersId,
                        @Param("treePath") String treePath,
                        @Param("year") Integer year,
                        @Param("month") Integer month);

        /**
         * Scope 1에서 공장설비 플래그 기반 배출량 합계 (공장설비 제외, 본사 전체)
         * factoryEnabled=false인 데이터만 집계 (비공장설비 그룹)
         */
        @Query("SELECT COALESCE(SUM(s.totalEmission), 0) FROM ScopeEmission s " +
                        "WHERE s.headquartersId = :headquartersId " +
                        "AND s.scopeType = 'SCOPE1' " +
                        "AND s.factoryEnabled = false " +
                        "AND s.scope1CategoryGroup NOT IN ('이동연소', '공정배출') " +
                        "AND s.reportingYear = :year " +
                        "AND s.reportingMonth = :month")
        BigDecimal sumScope1EmissionByFactoryDisabled(
                        @Param("headquartersId") Long headquartersId,
                        @Param("year") Integer year,
                        @Param("month") Integer month);

        /**
         * Scope 1에서 공장설비 플래그 기반 배출량 합계 (공장설비 제외, 협력사 트리 경로 기반)
         * factoryEnabled=false인 데이터만 집계 (비공장설비 그룹)
         */
        @Query("SELECT COALESCE(SUM(s.totalEmission), 0) FROM ScopeEmission s " +
                        "WHERE s.headquartersId = :headquartersId " +
                        "AND s.treePath LIKE CONCAT(:treePath, '%') " +
                        "AND s.scopeType = 'SCOPE1' " +
                        "AND s.factoryEnabled = false " +
                        "AND s.scope1CategoryGroup NOT IN ('이동연소', '공정배출') " +
                        "AND s.reportingYear = :year " +
                        "AND s.reportingMonth = :month")
        BigDecimal sumScope1EmissionByFactoryDisabledForPartner(
                        @Param("headquartersId") Long headquartersId,
                        @Param("treePath") String treePath,
                        @Param("year") Integer year,
                        @Param("month") Integer month);

        /**
         * Scope 2에서 공장설비 플래그 기반 배출량 합계 (본사 전체)
         * factoryEnabled=true인 데이터만 집계 (공장설비 그룹)
         */
        @Query("SELECT COALESCE(SUM(s.totalEmission), 0) FROM ScopeEmission s " +
                        "WHERE s.headquartersId = :headquartersId " +
                        "AND s.scopeType = 'SCOPE2' " +
                        "AND s.factoryEnabled = true " +
                        "AND s.reportingYear = :year " +
                        "AND s.reportingMonth = :month")
        BigDecimal sumScope2EmissionByFactoryEnabled(
                        @Param("headquartersId") Long headquartersId,
                        @Param("year") Integer year,
                        @Param("month") Integer month);

        /**
         * Scope 2에서 공장설비 플래그 기반 배출량 합계 (협력사 트리 경로 기반)
         * factoryEnabled=true인 데이터만 집계 (공장설비 그룹)
         */
        @Query("SELECT COALESCE(SUM(s.totalEmission), 0) FROM ScopeEmission s " +
                        "WHERE s.headquartersId = :headquartersId " +
                        "AND s.treePath LIKE CONCAT(:treePath, '%') " +
                        "AND s.scopeType = 'SCOPE2' " +
                        "AND s.factoryEnabled = true " +
                        "AND s.reportingYear = :year " +
                        "AND s.reportingMonth = :month")
        BigDecimal sumScope2EmissionByFactoryEnabledForPartner(
                        @Param("headquartersId") Long headquartersId,
                        @Param("treePath") String treePath,
                        @Param("year") Integer year,
                        @Param("month") Integer month);

        /**
         * Scope 2에서 공장설비 플래그 기반 배출량 합계 (공장설비 제외, 본사 전체)
         * factoryEnabled=false인 데이터만 집계 (비공장설비 그룹)
         */
        @Query("SELECT COALESCE(SUM(s.totalEmission), 0) FROM ScopeEmission s " +
                        "WHERE s.headquartersId = :headquartersId " +
                        "AND s.scopeType = 'SCOPE2' " +
                        "AND s.factoryEnabled = false " +
                        "AND s.reportingYear = :year " +
                        "AND s.reportingMonth = :month")
        BigDecimal sumScope2EmissionByFactoryDisabled(
                        @Param("headquartersId") Long headquartersId,
                        @Param("year") Integer year,
                        @Param("month") Integer month);

        /**
         * Scope 2에서 공장설비 플래그 기반 배출량 합계 (공장설비 제외, 협력사 트리 경로 기반)
         * factoryEnabled=false인 데이터만 집계 (비공장설비 그룹)
         */
        @Query("SELECT COALESCE(SUM(s.totalEmission), 0) FROM ScopeEmission s " +
                        "WHERE s.headquartersId = :headquartersId " +
                        "AND s.treePath LIKE CONCAT(:treePath, '%') " +
                        "AND s.scopeType = 'SCOPE2' " +
                        "AND s.factoryEnabled = false " +
                        "AND s.reportingYear = :year " +
                        "AND s.reportingMonth = :month")
        BigDecimal sumScope2EmissionByFactoryDisabledForPartner(
                        @Param("headquartersId") Long headquartersId,
                        @Param("treePath") String treePath,
                        @Param("year") Integer year,
                        @Param("month") Integer month);

        // ========================================================================
        // Scope 2 집계 쿼리 (Scope 2 Aggregation)
        // ========================================================================

        /**
         *          * Scope 2 전체 배출량 합계 (본사 전체)
         * 현재 모든 Scope 2가 공장설비 관련이므로 전체 합계를 사용
         */
        @Query("SELECT COALESCE(SUM(s.totalEmission), 0) FROM ScopeEmission s " +
                        "WHERE s.headquartersId = :headquartersId " +
                        "AND s.scopeType = 'SCOPE2' " +
                        "AND s.reportingYear = :year " +
                        "AND s.reportingMonth = :month")
        BigDecimal sumScope2TotalEmission(
                        @Param("headquartersId") Long headquartersId,
                        @Param("year") Integer year,
                        @Param("month") Integer month);

        /**
         * Scope 2 전체 배출량 합계 (협력사 트리 경로 기반)
         * 현재 모든 Scope 2가 공장설비 관련이므로 전체 합계를 사용
         */
        @Query("SELECT COALESCE(SUM(s.totalEmission), 0) FROM ScopeEmission s " +
                        "WHERE s.headquartersId = :headquartersId " +
                        "AND s.treePath LIKE CONCAT(:treePath, '%') " +
                        "AND s.scopeType = 'SCOPE2' " +
                        "AND s.reportingYear = :year " +
                        "AND s.reportingMonth = :month")
        BigDecimal sumScope2TotalEmissionForPartner(
                        @Param("headquartersId") Long headquartersId,
                        @Param("treePath") String treePath,
                        @Param("year") Integer year,
                        @Param("month") Integer month);

        /**
         * Scope 2에서 공장설비와 관련 없는 배출량 (Cat.1용)
         * 현재는 구분이 없으므로 향후 확장 가능하도록 준비
         */
        @Query("SELECT COALESCE(SUM(s.totalEmission), 0) FROM ScopeEmission s " +
                        "WHERE s.headquartersId = :headquartersId " +
                        "AND s.scopeType = 'SCOPE2' " +
                        "AND (s.scope2CategoryName IS NULL OR s.scope2CategoryName != '공장설비') " +
                        "AND s.reportingYear = :year " +
                        "AND s.reportingMonth = :month")
        BigDecimal sumScope2EmissionExcludingFactory(
                        @Param("headquartersId") Long headquartersId,
                        @Param("year") Integer year,
                        @Param("month") Integer month);


        // ========================================================================
        // 계층적 집계 쿼리 (Hierarchical Aggregation)
        // ========================================================================

        /**
         * 계층적 집계 (tree_path 기반)
         * 반환값: [treePath, scope1Sum, scope2Sum, scope3Sum]
         */
        @Query("SELECT s.treePath, " +
                        "SUM(CASE WHEN s.scopeType = 'SCOPE1' THEN s.totalEmission ELSE 0 END), " +
                        "SUM(CASE WHEN s.scopeType = 'SCOPE2' THEN s.totalEmission ELSE 0 END), " +
                        "SUM(CASE WHEN s.scopeType = 'SCOPE3' THEN s.totalEmission ELSE 0 END) " +
                        "FROM ScopeEmission s " +
                        "WHERE s.headquartersId = :headquartersId " +
                        "AND s.treePath LIKE CONCAT(:baseTreePath, '%') " +
                        "AND s.reportingYear = :year " +
                        "AND s.reportingMonth = :month " +
                        "GROUP BY s.treePath " +
                        "ORDER BY s.treePath")
        List<Object[]> sumEmissionByTreePath(
                        @Param("headquartersId") Long headquartersId,
                        @Param("baseTreePath") String baseTreePath,
                        @Param("year") Integer year,
                        @Param("month") Integer month);



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