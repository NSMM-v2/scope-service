package com.nsmm.esg.scope_service.repository;

import com.nsmm.esg.scope_service.entity.ScopeEmission;
import com.nsmm.esg.scope_service.enums.ScopeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
// ScopeEmission 엔티티의 데이터베이스 접근 레포지토리
public interface ScopeEmissionRepository extends JpaRepository<ScopeEmission, Long> {

        // 본사ID + ScopeType으로 전체 조회
        List<ScopeEmission> findByHeadquartersIdAndScopeType(Long headquartersId, ScopeType scopeType);

        // 본사ID + 연도 + 월로 전체 조회
        List<ScopeEmission> findByHeadquartersIdAndReportingYearAndReportingMonth(
                        Long headquartersId, Integer year, Integer month);

        // 본사ID + ScopeType + 연도 + 월로 전체 조회
        List<ScopeEmission> findByHeadquartersIdAndScopeTypeAndReportingYearAndReportingMonth(
                        Long headquartersId, ScopeType scopeType, Integer year, Integer month);

        // 협력사ID + 트리경로 + 연도 + 월로 전체 조회
        List<ScopeEmission> findByPartnerIdAndTreePathStartingWithAndReportingYearAndReportingMonth(
                        Long partnerId, String treePath, Integer year, Integer month);

        // 협력사ID + 트리경로 + ScopeType + 연도 + 월로 전체 조회
        List<ScopeEmission> findByPartnerIdAndTreePathStartingWithAndScopeTypeAndReportingYearAndReportingMonth(
                        Long partnerId, String treePath, ScopeType scopeType, Integer year, Integer month);

        // 본사ID + 제품코드로 제품 매핑 데이터 조회
        List<ScopeEmission> findByHeadquartersIdAndHasProductMappingTrueAndCompanyProductCode(
                        Long headquartersId, String companyProductCode);

        // 협력사ID + 트리경로 + 제품코드로 제품 매핑 데이터 조회
        List<ScopeEmission> findByPartnerIdAndTreePathStartingWithAndHasProductMappingTrueAndCompanyProductCode(
                        Long partnerId, String treePath, String companyProductCode);

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

        // 협력사 기준 ScopeType/연/월별 총 배출량 합계
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

}