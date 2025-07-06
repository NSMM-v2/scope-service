package com.nsmm.esg.scope_service.repository;

import com.nsmm.esg.scope_service.entity.ScopeEmission;
import com.nsmm.esg.scope_service.enums.ScopeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

/**
 * 통합 Scope 배출량 리포지토리
 * 
 * 주요 기능:
 * - 모든 Scope 타입 배출량 데이터 접근
 * - 권한 기반 데이터 조회 (본사/협력사 구분)
 * - TreePath 기반 계층 구조 관리
 * - 제품 코드 매핑 지원 (Scope 1, 2만 해당)
 * 
 * @author ESG Project Team
 * @version 2.0
 */
@Repository
public interface ScopeEmissionRepository extends JpaRepository<ScopeEmission, Long> {

        // ========================================================================
        // 본사용 기본 쿼리 메서드
        // ========================================================================

        List<ScopeEmission> findByHeadquartersId(Long headquartersId);

        Page<ScopeEmission> findByHeadquartersId(Long headquartersId, Pageable pageable);

        List<ScopeEmission> findByHeadquartersIdAndScopeType(Long headquartersId, ScopeType scopeType);

        List<ScopeEmission> findByHeadquartersIdAndReportingYearAndReportingMonth(
                        Long headquartersId, Integer year, Integer month);

        List<ScopeEmission> findByHeadquartersIdAndScopeTypeAndReportingYearAndReportingMonth(
                        Long headquartersId, ScopeType scopeType, Integer year, Integer month);

        // ========================================================================
        // 협력사용 기본 쿼리 메서드
        // ========================================================================

        List<ScopeEmission> findByPartnerIdAndTreePathStartingWith(Long partnerId, String treePath);

        Page<ScopeEmission> findByPartnerIdAndTreePathStartingWith(Long partnerId, String treePath, Pageable pageable);

        List<ScopeEmission> findByPartnerIdAndTreePathStartingWithAndScopeType(
                        Long partnerId, String treePath, ScopeType scopeType);

        List<ScopeEmission> findByPartnerIdAndTreePathStartingWithAndReportingYearAndReportingMonth(
                        Long partnerId, String treePath, Integer year, Integer month);

        List<ScopeEmission> findByPartnerIdAndTreePathStartingWithAndScopeTypeAndReportingYearAndReportingMonth(
                        Long partnerId, String treePath, ScopeType scopeType, Integer year, Integer month);

        // ========================================================================
        // 제품 코드 매핑 관련 쿼리 메서드 (Scope 1, 2 전용)
        // ========================================================================

        List<ScopeEmission> findByHeadquartersIdAndHasProductMappingTrueAndCompanyProductCode(
                        Long headquartersId, String companyProductCode);

        List<ScopeEmission> findByPartnerIdAndTreePathStartingWithAndHasProductMappingTrueAndCompanyProductCode(
                        Long partnerId, String treePath, String companyProductCode);

        // ========================================================================
        // 카테고리 기반 쿼리 메서드
        // ========================================================================

        List<ScopeEmission> findByHeadquartersIdAndScope1CategoryNumber(
                        Long headquartersId, Integer scope1CategoryNumber);

        List<ScopeEmission> findByHeadquartersIdAndScope2CategoryNumber(
                        Long headquartersId, Integer scope2CategoryNumber);

        List<ScopeEmission> findByHeadquartersIdAndScope3CategoryNumber(
                        Long headquartersId, Integer scope3CategoryNumber);

        List<ScopeEmission> findByPartnerIdAndTreePathStartingWithAndScope1CategoryNumber(
                        Long partnerId, String treePath, Integer scope1CategoryNumber);

        List<ScopeEmission> findByPartnerIdAndTreePathStartingWithAndScope2CategoryNumber(
                        Long partnerId, String treePath, Integer scope2CategoryNumber);

        List<ScopeEmission> findByPartnerIdAndTreePathStartingWithAndScope3CategoryNumber(
                        Long partnerId, String treePath, Integer scope3CategoryNumber);

        // ========================================================================
        // 중복 데이터 검증 쿼리 메서드
        // ========================================================================

        @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM ScopeEmission s " +
                        "WHERE s.headquartersId = :headquartersId " +
                        "AND s.scopeType = :scopeType " +
                        "AND s.reportingYear = :reportingYear " +
                        "AND s.reportingMonth = :reportingMonth " +
                        "AND (" +
                        "  (s.scopeType = 'SCOPE1' AND s.scope1CategoryNumber = :categoryNumber) OR " +
                        "  (s.scopeType = 'SCOPE2' AND s.scope2CategoryNumber = :categoryNumber) OR " +
                        "  (s.scopeType = 'SCOPE3' AND s.scope3CategoryNumber = :categoryNumber)" +
                        ")")
        boolean existsByHeadquartersIdAndScopeTypeAndCategoryNumberAndReportingYearAndReportingMonth(
                        @Param("headquartersId") Long headquartersId,
                        @Param("scopeType") ScopeType scopeType,
                        @Param("categoryNumber") Integer categoryNumber,
                        @Param("reportingYear") Integer reportingYear,
                        @Param("reportingMonth") Integer reportingMonth);

        @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM ScopeEmission s " +
                        "WHERE s.partnerId = :partnerId " +
                        "AND s.scopeType = :scopeType " +
                        "AND s.reportingYear = :reportingYear " +
                        "AND s.reportingMonth = :reportingMonth " +
                        "AND (" +
                        "  (s.scopeType = 'SCOPE1' AND s.scope1CategoryNumber = :categoryNumber) OR " +
                        "  (s.scopeType = 'SCOPE2' AND s.scope2CategoryNumber = :categoryNumber) OR " +
                        "  (s.scopeType = 'SCOPE3' AND s.scope3CategoryNumber = :categoryNumber)" +
                        ")")
        boolean existsByPartnerIdAndScopeTypeAndCategoryNumberAndReportingYearAndReportingMonth(
                        @Param("partnerId") Long partnerId,
                        @Param("scopeType") ScopeType scopeType,
                        @Param("categoryNumber") Integer categoryNumber,
                        @Param("reportingYear") Integer reportingYear,
                        @Param("reportingMonth") Integer reportingMonth);

        // ========================================================================
        // 집계 쿼리 메서드
        // ========================================================================

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

        @Query("SELECT s.scope3CategoryNumber, COALESCE(SUM(s.totalEmission), 0) FROM ScopeEmission s " +
                        "WHERE s.headquartersId = :headquartersId " +
                        "AND s.scopeType = 'SCOPE3' " +
                        "AND s.reportingYear = :year " +
                        "GROUP BY s.scope3CategoryNumber")
        List<Object[]> sumTotalEmissionByScope3CategoryAndYearForHeadquarters(
                        @Param("headquartersId") Long headquartersId,
                        @Param("year") Integer year);

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
        // 협력사용 메서드
        // ========================================================================

        List<ScopeEmission> findByPartnerIdAndScopeTypeAndReportingYearAndReportingMonth(
                        Long partnerId,
                        ScopeType scopeType,
                        Integer reportingYear,
                        Integer reportingMonth);

        // ScopeEmissionRepository.java에 추가
        List<ScopeEmission> findByTreePathStartingWithAndScopeType(String treePath, ScopeType scopeType);

}