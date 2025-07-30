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

        // 본사 직접 입력 데이터만 ScopeType/연/월별 총 배출량 합계
        @Query("SELECT COALESCE(SUM(s.totalEmission), 0) FROM ScopeEmission s " +
                        "WHERE s.headquartersId = :headquartersId " +
                        "AND s.partnerId IS NULL " +
                        "AND s.scopeType = :scopeType " +
                        "AND s.reportingYear = :year " +
                        "AND s.reportingMonth = :month")
        BigDecimal sumTotalEmissionByScopeTypeAndYearAndMonthForHeadquartersOnly(
                        @Param("headquartersId") Long headquartersId,
                        @Param("scopeType") ScopeType scopeType,
                        @Param("year") Integer year,
                        @Param("month") Integer month);


        // Scope1 카테고리별 연간 배출량 집계 - 본사 직접 입력 데이터만
        @Query("SELECT s.scope1CategoryNumber, " +
               "COALESCE(SUM(s.totalEmission), 0), " +
               "COUNT(s) " +
               "FROM ScopeEmission s " +
               "WHERE s.headquartersId = :headquartersId " +
               "AND s.partnerId IS NULL " +
               "AND s.scopeType = 'SCOPE1' " +
               "AND s.reportingYear = :year " +
               "AND s.scope1CategoryNumber IS NOT NULL " +
               "GROUP BY s.scope1CategoryNumber " +
               "ORDER BY s.scope1CategoryNumber")
        List<Object[]> sumScope1EmissionByYearAndCategoryForHeadquartersOnly(
                @Param("headquartersId") Long headquartersId,
                @Param("year") Integer year);

        // Scope1 카테고리별 연간 배출량 집계 - 특정 협력사 데이터만
        @Query("SELECT s.scope1CategoryNumber, " +
               "COALESCE(SUM(s.totalEmission), 0), " +
               "COUNT(s) " +
               "FROM ScopeEmission s " +
               "WHERE s.headquartersId = :headquartersId " +
               "AND s.partnerId = :partnerId " +
               "AND s.scopeType = 'SCOPE1' " +
               "AND s.reportingYear = :year " +
               "AND s.scope1CategoryNumber IS NOT NULL " +
               "GROUP BY s.scope1CategoryNumber " +
               "ORDER BY s.scope1CategoryNumber")
        List<Object[]> sumScope1EmissionByYearAndCategoryForSpecificPartner(
                @Param("headquartersId") Long headquartersId,
                @Param("partnerId") Long partnerId,
                @Param("year") Integer year);


        // Scope2 카테고리별 연간 배출량 집계 - 본사 직접 입력 데이터만
        @Query("SELECT s.scope2CategoryNumber, " +
               "COALESCE(SUM(s.totalEmission), 0), " +
               "COUNT(s) " +
               "FROM ScopeEmission s " +
               "WHERE s.headquartersId = :headquartersId " +
               "AND s.partnerId IS NULL " +
               "AND s.scopeType = 'SCOPE2' " +
               "AND s.reportingYear = :year " +
               "AND s.scope2CategoryNumber IS NOT NULL " +
               "GROUP BY s.scope2CategoryNumber " +
               "ORDER BY s.scope2CategoryNumber")
        List<Object[]> sumScope2EmissionByYearAndCategoryForHeadquartersOnly(
                @Param("headquartersId") Long headquartersId,
                @Param("year") Integer year);


        // Scope2 카테고리별 연간 배출량 집계 - 특정 협력사 데이터만
        @Query("SELECT s.scope2CategoryNumber, " +
               "COALESCE(SUM(s.totalEmission), 0), " +
               "COUNT(s) " +
               "FROM ScopeEmission s " +
               "WHERE s.headquartersId = :headquartersId " +
               "AND s.partnerId = :partnerId " +
               "AND s.scopeType = 'SCOPE2' " +
               "AND s.reportingYear = :year " +
               "AND s.scope2CategoryNumber IS NOT NULL " +
               "GROUP BY s.scope2CategoryNumber " +
               "ORDER BY s.scope2CategoryNumber")
        List<Object[]> sumScope2EmissionByYearAndCategoryForSpecificPartner(
                @Param("headquartersId") Long headquartersId,
                @Param("partnerId") Long partnerId,
                @Param("year") Integer year);


        // Scope3 카테고리별 연간 배출량 집계 - 본사 직접 입력 데이터만
        @Query("SELECT s.scope3CategoryNumber, " +
               "COALESCE(SUM(s.totalEmission), 0), " +
               "COUNT(s) " +
               "FROM ScopeEmission s " +
               "WHERE s.headquartersId = :headquartersId " +
               "AND s.partnerId IS NULL " +
               "AND s.scopeType = 'SCOPE3' " +
               "AND s.reportingYear = :year " +
               "AND s.scope3CategoryNumber IS NOT NULL " +
               "GROUP BY s.scope3CategoryNumber " +
               "ORDER BY s.scope3CategoryNumber")
        List<Object[]> sumScope3EmissionByYearAndCategoryForHeadquartersOnly(
                @Param("headquartersId") Long headquartersId,
                @Param("year") Integer year);


        // Scope3 카테고리별 연간 배출량 집계 - 특정 협력사 데이터만
        @Query("SELECT s.scope3CategoryNumber, " +
               "COALESCE(SUM(s.totalEmission), 0), " +
               "COUNT(s) " +
               "FROM ScopeEmission s " +
               "WHERE s.headquartersId = :headquartersId " +
               "AND s.partnerId = :partnerId " +
               "AND s.scopeType = 'SCOPE3' " +
               "AND s.reportingYear = :year " +
               "AND s.scope3CategoryNumber IS NOT NULL " +
               "GROUP BY s.scope3CategoryNumber " +
               "ORDER BY s.scope3CategoryNumber")
        List<Object[]> sumScope3EmissionByYearAndCategoryForSpecificPartner(
                @Param("headquartersId") Long headquartersId,
                @Param("partnerId") Long partnerId,
                @Param("year") Integer year);



        // Scope1 카테고리별 월간 배출량 집계 (본사 기준 - 본사 직접 입력 데이터만) - 연도의 모든 월
        @Query("SELECT s.scope1CategoryNumber, " +
               "s.reportingMonth, " +
               "COALESCE(SUM(s.totalEmission), 0), " +
               "COUNT(s) " +
               "FROM ScopeEmission s " +
               "WHERE s.headquartersId = :headquartersId " +
               "AND s.partnerId IS NULL " +
               "AND s.scopeType = 'SCOPE1' " +
               "AND s.reportingYear = :year " +
               "AND s.scope1CategoryNumber IS NOT NULL " +
               "GROUP BY s.scope1CategoryNumber, s.reportingMonth " +
               "ORDER BY s.scope1CategoryNumber, s.reportingMonth")
        List<Object[]> sumScope1EmissionByYearAndMonthAndCategoryForHeadquartersOnly(
                @Param("headquartersId") Long headquartersId,
                @Param("year") Integer year);

        // Scope1 카테고리별 월간 배출량 집계 - 본사 전체 데이터
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

        // Scope1 카테고리별 월간 배출량 집계 - 특정 협력사 데이터만
        @Query("SELECT s.scope1CategoryNumber, " +
               "s.reportingMonth, " +
               "COALESCE(SUM(s.totalEmission), 0), " +
               "COUNT(s) " +
               "FROM ScopeEmission s " +
               "WHERE s.headquartersId = :headquartersId " +
               "AND s.partnerId = :partnerId " +
               "AND s.scopeType = 'SCOPE1' " +
               "AND s.reportingYear = :year " +
               "AND s.scope1CategoryNumber IS NOT NULL " +
               "GROUP BY s.scope1CategoryNumber, s.reportingMonth " +
               "ORDER BY s.scope1CategoryNumber, s.reportingMonth")
        List<Object[]> sumScope1EmissionByYearAndMonthAndCategoryForSpecificPartner(
                @Param("headquartersId") Long headquartersId,
                @Param("partnerId") Long partnerId,
                @Param("year") Integer year);



        // Scope2 카테고리별 월간 배출량 집계 - 본사 직접 입력 데이터만
        @Query("SELECT s.scope2CategoryNumber, " +
               "s.reportingMonth, " +
               "COALESCE(SUM(s.totalEmission), 0), " +
               "COUNT(s) " +
               "FROM ScopeEmission s " +
               "WHERE s.headquartersId = :headquartersId " +
               "AND s.partnerId IS NULL " +
               "AND s.scopeType = 'SCOPE2' " +
               "AND s.reportingYear = :year " +
               "AND s.scope2CategoryNumber IS NOT NULL " +
               "GROUP BY s.scope2CategoryNumber, s.reportingMonth " +
               "ORDER BY s.scope2CategoryNumber, s.reportingMonth")
        List<Object[]> sumScope2EmissionByYearAndMonthAndCategoryForHeadquartersOnly(
                @Param("headquartersId") Long headquartersId,
                @Param("year") Integer year);


        // Scope2 카테고리별 월간 배출량 집계 - 특정 협력사 데이터만
        @Query("SELECT s.scope2CategoryNumber, " +
               "s.reportingMonth, " +
               "COALESCE(SUM(s.totalEmission), 0), " +
               "COUNT(s) " +
               "FROM ScopeEmission s " +
               "WHERE s.headquartersId = :headquartersId " +
               "AND s.partnerId = :partnerId " +
               "AND s.scopeType = 'SCOPE2' " +
               "AND s.reportingYear = :year " +
               "AND s.scope2CategoryNumber IS NOT NULL " +
               "GROUP BY s.scope2CategoryNumber, s.reportingMonth " +
               "ORDER BY s.scope2CategoryNumber, s.reportingMonth")
        List<Object[]> sumScope2EmissionByYearAndMonthAndCategoryForSpecificPartner(
                @Param("headquartersId") Long headquartersId,
                @Param("partnerId") Long partnerId,
                @Param("year") Integer year);


        // Scope3 카테고리별 월간 배출량 집계 - 본사 직접 입력 데이터만
        @Query("SELECT s.scope3CategoryNumber, " +
               "s.reportingMonth, " +
               "COALESCE(SUM(s.totalEmission), 0), " +
               "COUNT(s) " +
               "FROM ScopeEmission s " +
               "WHERE s.headquartersId = :headquartersId " +
               "AND s.partnerId IS NULL " +
               "AND s.scopeType = 'SCOPE3' " +
               "AND s.reportingYear = :year " +
               "AND s.scope3CategoryNumber IS NOT NULL " +
               "GROUP BY s.scope3CategoryNumber, s.reportingMonth " +
               "ORDER BY s.scope3CategoryNumber, s.reportingMonth")
        List<Object[]> sumScope3EmissionByYearAndMonthAndCategoryForHeadquartersOnly(
                @Param("headquartersId") Long headquartersId,
                @Param("year") Integer year);


        // Scope3 카테고리별 월간 배출량 집계 - 특정 협력사 데이터만
        @Query("SELECT s.scope3CategoryNumber, " +
               "s.reportingMonth, " +
               "COALESCE(SUM(s.totalEmission), 0), " +
               "COUNT(s) " +
               "FROM ScopeEmission s " +
               "WHERE s.headquartersId = :headquartersId " +
               "AND s.partnerId = :partnerId " +
               "AND s.scopeType = 'SCOPE3' " +
               "AND s.reportingYear = :year " +
               "AND s.scope3CategoryNumber IS NOT NULL " +
               "GROUP BY s.scope3CategoryNumber, s.reportingMonth " +
               "ORDER BY s.scope3CategoryNumber, s.reportingMonth")
        List<Object[]> sumScope3EmissionByYearAndMonthAndCategoryForSpecificPartner(
                @Param("headquartersId") Long headquartersId,
                @Param("partnerId") Long partnerId,
                @Param("year") Integer year);



        // ========================================================================
        // 협력사별 월별 집계 쿼리 (Partner Monthly Aggregation)
        // ========================================================================

        // 협력사별 ScopeType/연/월별 총 배출량 합계
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

        // 협력사별 연/월별 배출량 데이터 건수
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

        // 본사 연/월별 배출량 데이터 건수
        @Query("SELECT COUNT(s) FROM ScopeEmission s " +
                        "WHERE s.headquartersId = :headquartersId " +
                        "AND s.reportingYear = :year " +
                        "AND s.reportingMonth = :month")
        Long countEmissionsByHeadquartersAndYearAndMonth(
                        @Param("headquartersId") Long headquartersId,
                        @Param("year") Integer year,
                        @Param("month") Integer month);

        // 본사 직접 입력 데이터만 연/월별 배출량 데이터 건수
        @Query("SELECT COUNT(s) FROM ScopeEmission s " +
                        "WHERE s.headquartersId = :headquartersId " +
                        "AND s.partnerId IS NULL " +
                        "AND s.reportingYear = :year " +
                        "AND s.reportingMonth = :month")
        Long countEmissionsByHeadquartersOnlyAndYearAndMonth(
                        @Param("headquartersId") Long headquartersId,
                        @Param("year") Integer year,
                        @Param("month") Integer month);

        // ========================================================================
        // Scope 3 특수 집계용 그룹별/공장설비별 월별 집계 쿼리
        // ========================================================================

        // Scope1 이동연소 그룹 월별 집계 (본사 - 카테고리 4,5,6번)
        @Query("SELECT COALESCE(SUM(s.totalEmission), 0) FROM ScopeEmission s " +
               "WHERE s.headquartersId = :headquartersId " +
               "AND s.partnerId IS NULL " +
               "AND s.scopeType = 'SCOPE1' " +
               "AND s.scope1CategoryNumber IN (4, 5, 6) " +
               "AND s.reportingYear = :year " +
               "AND s.reportingMonth = :month")
        BigDecimal sumScope1MobileCombustionEmissionsByYearAndMonthForHeadquarters(
                @Param("headquartersId") Long headquartersId,
                @Param("year") Integer year,
                @Param("month") Integer month);

        // Scope1 이동연소 그룹 월별 집계 (특정 협력사 - 카테고리 4,5,6번)
        @Query("SELECT COALESCE(SUM(s.totalEmission), 0) FROM ScopeEmission s " +
               "WHERE s.headquartersId = :headquartersId " +
               "AND s.partnerId = :partnerId " +
               "AND s.scopeType = 'SCOPE1' " +
               "AND s.scope1CategoryNumber IN (4, 5, 6) " +
               "AND s.reportingYear = :year " +
               "AND s.reportingMonth = :month")
        BigDecimal sumScope1MobileCombustionEmissionsByYearAndMonthForPartner(
                @Param("headquartersId") Long headquartersId,
                @Param("partnerId") Long partnerId,
                @Param("year") Integer year,
                @Param("month") Integer month);

        // Scope1 폐수처리 월별 집계 (본사 - 카테고리 8번)
        @Query("SELECT COALESCE(SUM(s.totalEmission), 0) FROM ScopeEmission s " +
               "WHERE s.headquartersId = :headquartersId " +
               "AND s.partnerId IS NULL " +
               "AND s.scopeType = 'SCOPE1' " +
               "AND s.scope1CategoryNumber = 8 " +
               "AND s.reportingYear = :year " +
               "AND s.reportingMonth = :month")
        BigDecimal sumScope1WasteWaterTreatmentEmissionsByYearAndMonthForHeadquarters(
                @Param("headquartersId") Long headquartersId,
                @Param("year") Integer year,
                @Param("month") Integer month);

        // Scope1 폐수처리 월별 집계 (특정 협력사 - 카테고리 8번)
        @Query("SELECT COALESCE(SUM(s.totalEmission), 0) FROM ScopeEmission s " +
               "WHERE s.headquartersId = :headquartersId " +
               "AND s.partnerId = :partnerId " +
               "AND s.scopeType = 'SCOPE1' " +
               "AND s.scope1CategoryNumber = 8 " +
               "AND s.reportingYear = :year " +
               "AND s.reportingMonth = :month")
        BigDecimal sumScope1WasteWaterTreatmentEmissionsByYearAndMonthForPartner(
                @Param("headquartersId") Long headquartersId,
                @Param("partnerId") Long partnerId,
                @Param("year") Integer year,
                @Param("month") Integer month);

        // Scope1 공장설비 월별 집계 (본사 - factoryEnabled = true)
        @Query("SELECT COALESCE(SUM(s.totalEmission), 0) FROM ScopeEmission s " +
               "WHERE s.headquartersId = :headquartersId " +
               "AND s.partnerId IS NULL " +
               "AND s.scopeType = 'SCOPE1' " +
               "AND s.factoryEnabled = true " +
               "AND s.reportingYear = :year " +
               "AND s.reportingMonth = :month")
        BigDecimal sumScope1FactoryEmissionsByYearAndMonthForHeadquarters(
                @Param("headquartersId") Long headquartersId,
                @Param("year") Integer year,
                @Param("month") Integer month);

        // Scope1 공장설비 월별 집계 (특정 협력사 - factoryEnabled = true)
        @Query("SELECT COALESCE(SUM(s.totalEmission), 0) FROM ScopeEmission s " +
               "WHERE s.headquartersId = :headquartersId " +
               "AND s.partnerId = :partnerId " +
               "AND s.scopeType = 'SCOPE1' " +
               "AND s.factoryEnabled = true " +
               "AND s.reportingYear = :year " +
               "AND s.reportingMonth = :month")
        BigDecimal sumScope1FactoryEmissionsByYearAndMonthForPartner(
                @Param("headquartersId") Long headquartersId,
                @Param("partnerId") Long partnerId,
                @Param("year") Integer year,
                @Param("month") Integer month);

        // Scope2 공장설비 월별 집계 (본사 - factoryEnabled = true)
        @Query("SELECT COALESCE(SUM(s.totalEmission), 0) FROM ScopeEmission s " +
               "WHERE s.headquartersId = :headquartersId " +
               "AND s.partnerId IS NULL " +
               "AND s.scopeType = 'SCOPE2' " +
               "AND s.factoryEnabled = true " +
               "AND s.reportingYear = :year " +
               "AND s.reportingMonth = :month")
        BigDecimal sumScope2FactoryEmissionsByYearAndMonthForHeadquarters(
                @Param("headquartersId") Long headquartersId,
                @Param("year") Integer year,
                @Param("month") Integer month);

        // Scope2 공장설비 월별 집계 (특정 협력사 - factoryEnabled = true)
        @Query("SELECT COALESCE(SUM(s.totalEmission), 0) FROM ScopeEmission s " +
               "WHERE s.headquartersId = :headquartersId " +
               "AND s.partnerId = :partnerId " +
               "AND s.scopeType = 'SCOPE2' " +
               "AND s.factoryEnabled = true " +
               "AND s.reportingYear = :year " +
               "AND s.reportingMonth = :month")
        BigDecimal sumScope2FactoryEmissionsByYearAndMonthForPartner(
                @Param("headquartersId") Long headquartersId,
                @Param("partnerId") Long partnerId,
                @Param("year") Integer year,
                @Param("month") Integer month);

        // Scope3 특정 카테고리 월별 집계 (본사)
        @Query("SELECT COALESCE(SUM(s.totalEmission), 0) FROM ScopeEmission s " +
               "WHERE s.headquartersId = :headquartersId " +
               "AND s.partnerId IS NULL " +
               "AND s.scopeType = 'SCOPE3' " +
               "AND s.scope3CategoryNumber = :categoryNumber " +
               "AND s.reportingYear = :year " +
               "AND s.reportingMonth = :month")
        BigDecimal sumScope3CategoryEmissionsByYearAndMonthForHeadquarters(
                @Param("headquartersId") Long headquartersId,
                @Param("categoryNumber") Integer categoryNumber,
                @Param("year") Integer year,
                @Param("month") Integer month);

        // Scope3 특정 카테고리 월별 집계 (특정 협력사)
        @Query("SELECT COALESCE(SUM(s.totalEmission), 0) FROM ScopeEmission s " +
               "WHERE s.headquartersId = :headquartersId " +
               "AND s.partnerId = :partnerId " +
               "AND s.scopeType = 'SCOPE3' " +
               "AND s.scope3CategoryNumber = :categoryNumber " +
               "AND s.reportingYear = :year " +
               "AND s.reportingMonth = :month")
        BigDecimal sumScope3CategoryEmissionsByYearAndMonthForPartner(
                @Param("headquartersId") Long headquartersId,
                @Param("partnerId") Long partnerId,
                @Param("categoryNumber") Integer categoryNumber,
                @Param("year") Integer year,
                @Param("month") Integer month);

        // Scope1 전체 월별 집계 (본사)
        @Query("SELECT COALESCE(SUM(s.totalEmission), 0) FROM ScopeEmission s " +
               "WHERE s.headquartersId = :headquartersId " +
               "AND s.partnerId IS NULL " +
               "AND s.scopeType = 'SCOPE1' " +
               "AND s.reportingYear = :year " +
               "AND s.reportingMonth = :month")
        BigDecimal sumScope1TotalEmissionsByYearAndMonthForHeadquarters(
                @Param("headquartersId") Long headquartersId,
                @Param("year") Integer year,
                @Param("month") Integer month);

        // Scope1 전체 월별 집계 (특정 협력사)
        @Query("SELECT COALESCE(SUM(s.totalEmission), 0) FROM ScopeEmission s " +
               "WHERE s.headquartersId = :headquartersId " +
               "AND s.partnerId = :partnerId " +
               "AND s.scopeType = 'SCOPE1' " +
               "AND s.reportingYear = :year " +
               "AND s.reportingMonth = :month")
        BigDecimal sumScope1TotalEmissionsByYearAndMonthForPartner(
                @Param("headquartersId") Long headquartersId,
                @Param("partnerId") Long partnerId,
                @Param("year") Integer year,
                @Param("month") Integer month);

        // Scope2 전체 월별 집계 (본사)
        @Query("SELECT COALESCE(SUM(s.totalEmission), 0) FROM ScopeEmission s " +
               "WHERE s.headquartersId = :headquartersId " +
               "AND s.partnerId IS NULL " +
               "AND s.scopeType = 'SCOPE2' " +
               "AND s.reportingYear = :year " +
               "AND s.reportingMonth = :month")
        BigDecimal sumScope2TotalEmissionsByYearAndMonthForHeadquarters(
                @Param("headquartersId") Long headquartersId,
                @Param("year") Integer year,
                @Param("month") Integer month);

        // Scope2 전체 월별 집계 (특정 협력사)
        @Query("SELECT COALESCE(SUM(s.totalEmission), 0) FROM ScopeEmission s " +
               "WHERE s.headquartersId = :headquartersId " +
               "AND s.partnerId = :partnerId " +
               "AND s.scopeType = 'SCOPE2' " +
               "AND s.reportingYear = :year " +
               "AND s.reportingMonth = :month")
        BigDecimal sumScope2TotalEmissionsByYearAndMonthForPartner(
                @Param("headquartersId") Long headquartersId,
                @Param("partnerId") Long partnerId,
                @Param("year") Integer year,
                @Param("month") Integer month);


        // 전체 하위 조직들 파트너 ID 목록 조회 (모든 하위 레벨 포함)
        @Query("SELECT DISTINCT s.partnerId FROM ScopeEmission s " +
               "WHERE s.headquartersId = :headquartersId " +
               "AND s.partnerId IS NOT NULL " +
               "AND s.treePath LIKE CONCAT(:parentTreePath, '%') " +
               "AND s.treePath != :parentTreePath")
        List<Long> findAllChildPartnerIds(
                @Param("headquartersId") Long headquartersId,
                @Param("parentTreePath") String parentTreePath);


        // ========================================================================
        // 특정 월만 조회하는 카테고리별 배출량 집계 쿼리 (Single Month Category Aggregation)
        // ========================================================================

        // Scope3 카테고리별 특정 월 배출량 집계 - 본사 직접 입력 데이터만
        @Query("SELECT s.scope3CategoryNumber, " +
               "s.reportingMonth, " +
               "COALESCE(SUM(s.totalEmission), 0), " +
               "COUNT(s) " +
               "FROM ScopeEmission s " +
               "WHERE s.headquartersId = :headquartersId " +
               "AND s.partnerId IS NULL " +
               "AND s.scopeType = 'SCOPE3' " +
               "AND s.reportingYear = :year " +
               "AND s.reportingMonth = :month " +
               "AND s.scope3CategoryNumber IS NOT NULL " +
               "GROUP BY s.scope3CategoryNumber, s.reportingMonth " +
               "ORDER BY s.scope3CategoryNumber, s.reportingMonth")
        List<Object[]> sumScope3EmissionByYearAndSpecificMonthAndCategoryForHeadquartersOnly(
                @Param("headquartersId") Long headquartersId,
                @Param("year") Integer year,
                @Param("month") Integer month);

        // Scope3 카테고리별 특정 월 배출량 집계 - 특정 협력사 데이터만
        @Query("SELECT s.scope3CategoryNumber, " +
               "s.reportingMonth, " +
               "COALESCE(SUM(s.totalEmission), 0), " +
               "COUNT(s) " +
               "FROM ScopeEmission s " +
               "WHERE s.headquartersId = :headquartersId " +
               "AND s.partnerId = :partnerId " +
               "AND s.scopeType = 'SCOPE3' " +
               "AND s.reportingYear = :year " +
               "AND s.reportingMonth = :month " +
               "AND s.scope3CategoryNumber IS NOT NULL " +
               "GROUP BY s.scope3CategoryNumber, s.reportingMonth " +
               "ORDER BY s.scope3CategoryNumber, s.reportingMonth")
        List<Object[]> sumScope3EmissionByYearAndSpecificMonthAndCategoryForSpecificPartner(
                @Param("headquartersId") Long headquartersId,
                @Param("partnerId") Long partnerId,
                @Param("year") Integer year,
                @Param("month") Integer month);

        // ========================================================================
        // 레벨별 맵핑된 자재코드 조회 쿼리 (Level-based Mapped Material Aggregation)
        // ========================================================================

        // 특정 레벨의 맵핑된 자재코드별 Scope 1+2 통합 배출량 집계
        @Query("SELECT mm.internalMaterialCode, " +
               "mm.materialName, " +
               "mm.upstreamMaterialCode, " +
               "COALESCE(SUM(CASE WHEN s.scopeType = 'SCOPE1' THEN s.totalEmission ELSE 0 END), 0), " +
               "COALESCE(SUM(CASE WHEN s.scopeType = 'SCOPE2' THEN s.totalEmission ELSE 0 END), 0), " +
               "COALESCE(SUM(s.totalEmission), 0), " +
               "COUNT(s) " +
               "FROM ScopeEmission s " +
               "JOIN s.materialMapping mm " +
               "WHERE s.headquartersId = :headquartersId " +
               "AND mm.partnerLevel = :targetLevel " +
               "AND s.hasMaterialMapping = true " +
               "AND s.materialMapping IS NOT NULL " +
               "AND s.scopeType IN ('SCOPE1', 'SCOPE2') " +
               "AND s.reportingYear = :year " +
               "AND (:month IS NULL OR s.reportingMonth = :month) " +
               "GROUP BY mm.internalMaterialCode, mm.materialName, mm.upstreamMaterialCode " +
               "ORDER BY mm.internalMaterialCode")
        List<Object[]> findMappedMaterialEmissionsByLevel(
                @Param("headquartersId") Long headquartersId,
                @Param("targetLevel") Integer targetLevel,
                @Param("year") Integer year,
                @Param("month") Integer month);



}