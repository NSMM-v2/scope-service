package com.nsmm.esg.scope_service.repository;

import com.nsmm.esg.scope_service.entity.ProductCodeMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 제품 코드 매핑 리포지토리 - Scope 1, 2 전용
 *
 * 주요 기능:
 * - 제품 코드와 제품명 매핑 관리
 * - 자동 제품명 설정 지원
 * - 제품 기반 배출량 계산 지원
 * - 계층 구조 및 권한 관리
 *
 * 비즈니스 규칙:
 * - 회사별 제품 코드는 고유값 (UNIQUE 제약조건)
 * - 제품명은 검색 가능 (LIKE 쿼리 지원)
 * - 본사/협력사 구분 관리
 * - 활성/비활성 상태 관리
 *
 * @author ESG Project Team
 * @version 1.0
 * @since 2024
 */
@Repository
public interface ProductCodeMappingRepository extends JpaRepository<ProductCodeMapping, Long> {

    // ========================================================================
    // 기본 조회 메서드 (Basic Query Methods)
    // ========================================================================

    /**
     * 회사별 제품 코드로 매핑 정보 조회
     *
     * @param companyProductCode 회사별 제품 코드
     * @return 매핑 정보 (Optional)
     */
    Optional<ProductCodeMapping> findByCompanyProductCode(String companyProductCode);

    /**
     * 제품명으로 매핑 정보 조회
     *
     * @param productName 제품명
     * @return 매핑 정보 (Optional)
     */
    Optional<ProductCodeMapping> findByProductName(String productName);

    /**
     * 제품명 부분 검색
     *
     * @param productName 제품명 (부분 검색)
     * @return 매핑 정보 목록
     */
    List<ProductCodeMapping> findByProductNameContaining(String productName);

    /**
     * 회사별 제품 코드 부분 검색
     *
     * @param companyProductCode 회사별 제품 코드 (부분 검색)
     * @return 매핑 정보 목록
     */
    List<ProductCodeMapping> findByCompanyProductCodeContaining(String companyProductCode);

    /**
     * 본사 제품 코드로 매핑 정보 조회
     *
     * @param headquartersProductCode 본사 제품 코드
     * @return 매핑 정보 목록
     */
    List<ProductCodeMapping> findByHeadquartersProductCode(String headquartersProductCode);

    // ========================================================================
    // 활성 상태 기반 조회 메서드 (Active Status Query Methods)
    // ========================================================================

    /**
     * 활성 상태 제품 목록 조회
     *
     * @param isActive 활성 상태 (true: 활성, false: 비활성)
     * @return 매핑 정보 목록
     */
    List<ProductCodeMapping> findByIsActive(Boolean isActive);

    /**
     * 활성 상태 제품 중 제품명 부분 검색
     *
     * @param productName 제품명 (부분 검색)
     * @param isActive    활성 상태
     * @return 매핑 정보 목록
     */
    List<ProductCodeMapping> findByProductNameContainingAndIsActive(String productName, Boolean isActive);

    /**
     * 활성 상태 제품 중 회사별 제품 코드 부분 검색
     *
     * @param companyProductCode 회사별 제품 코드 (부분 검색)
     * @param isActive           활성 상태
     * @return 매핑 정보 목록
     */
    List<ProductCodeMapping> findByCompanyProductCodeContainingAndIsActive(String companyProductCode, Boolean isActive);

    // ========================================================================
    // 조직별 조회 메서드 (Organization-based Query Methods)
    // ========================================================================

    /**
     * 본사 ID로 제품 목록 조회
     *
     * @param headquartersId 본사 ID
     * @return 매핑 정보 목록
     */
    List<ProductCodeMapping> findByHeadquartersId(Long headquartersId);

    /**
     * 본사 ID와 활성 상태로 제품 목록 조회
     *
     * @param headquartersId 본사 ID
     * @param isActive       활성 상태
     * @return 매핑 정보 목록
     */
    List<ProductCodeMapping> findByHeadquartersIdAndIsActive(Long headquartersId, Boolean isActive);

    /**
     * 협력사 ID로 제품 목록 조회
     *
     * @param partnerId 협력사 ID
     * @return 매핑 정보 목록
     */
    List<ProductCodeMapping> findByPartnerId(Long partnerId);

    /**
     * 협력사 ID와 활성 상태로 제품 목록 조회
     *
     * @param partnerId 협력사 ID
     * @param isActive  활성 상태
     * @return 매핑 정보 목록
     */
    List<ProductCodeMapping> findByPartnerIdAndIsActive(Long partnerId, Boolean isActive);

    // ========================================================================
    // 보고 기간 기반 조회 메서드 (Reporting Period Query Methods)
    // ========================================================================

    /**
     * 보고 연도별 제품 목록 조회
     *
     * @param reportingYear 보고 연도
     * @return 매핑 정보 목록
     */
    List<ProductCodeMapping> findByReportingYear(Integer reportingYear);

    /**
     * 보고 연도/월별 제품 목록 조회
     *
     * @param reportingYear  보고 연도
     * @param reportingMonth 보고 월
     * @return 매핑 정보 목록
     */
    List<ProductCodeMapping> findByReportingYearAndReportingMonth(Integer reportingYear, Integer reportingMonth);

    /**
     * 본사 ID, 보고 연도별 제품 목록 조회
     *
     * @param headquartersId 본사 ID
     * @param reportingYear  보고 연도
     * @return 매핑 정보 목록
     */
    List<ProductCodeMapping> findByHeadquartersIdAndReportingYear(Long headquartersId, Integer reportingYear);

    // ========================================================================
    // 중복 검증 메서드 (Duplicate Validation Methods)
    // ========================================================================

    /**
     * 회사별 제품 코드 중복 검증
     *
     * @param companyProductCode 회사별 제품 코드
     * @return 중복 여부
     */
    boolean existsByCompanyProductCode(String companyProductCode);

    /**
     * 제품명 중복 검증
     *
     * @param productName 제품명
     * @return 중복 여부
     */
    boolean existsByProductName(String productName);

    /**
     * 회사별 제품 코드 중복 검증 (특정 ID 제외)
     *
     * @param companyProductCode 회사별 제품 코드
     * @param excludeId          제외할 ID
     * @return 중복 여부
     */
    @Query("SELECT COUNT(p) > 0 FROM ProductCodeMapping p " +
            "WHERE p.companyProductCode = :companyProductCode " +
            "AND p.id != :excludeId")
    boolean existsByCompanyProductCodeAndIdNot(@Param("companyProductCode") String companyProductCode,
            @Param("excludeId") Long excludeId);

    /**
     * 제품명 중복 검증 (특정 ID 제외)
     *
     * @param productName 제품명
     * @param excludeId   제외할 ID
     * @return 중복 여부
     */
    @Query("SELECT COUNT(p) > 0 FROM ProductCodeMapping p " +
            "WHERE p.productName = :productName " +
            "AND p.id != :excludeId")
    boolean existsByProductNameAndIdNot(@Param("productName") String productName, @Param("excludeId") Long excludeId);

    // ========================================================================
    // 검색 및 필터링 메서드 (Search and Filter Methods)
    // ========================================================================

    /**
     * 키워드 통합 검색 (회사별 제품 코드, 본사 제품 코드, 제품명)
     *
     * @param keyword 검색 키워드
     * @return 매핑 정보 목록
     */
    @Query("SELECT p FROM ProductCodeMapping p " +
            "WHERE p.companyProductCode LIKE %:keyword% " +
            "OR p.headquartersProductCode LIKE %:keyword% " +
            "OR p.productName LIKE %:keyword%")
    List<ProductCodeMapping> searchByKeyword(@Param("keyword") String keyword);

    /**
     * 활성 상태 제품 중 키워드 통합 검색
     *
     * @param keyword  검색 키워드
     * @param isActive 활성 상태
     * @return 매핑 정보 목록
     */
    @Query("SELECT p FROM ProductCodeMapping p " +
            "WHERE p.isActive = :isActive " +
            "AND (p.companyProductCode LIKE %:keyword% " +
            "OR p.headquartersProductCode LIKE %:keyword% " +
            "OR p.productName LIKE %:keyword%)")
    List<ProductCodeMapping> searchByKeywordAndIsActive(
            @Param("keyword") String keyword,
            @Param("isActive") Boolean isActive);

    // ========================================================================
    // 통계 및 집계 메서드 (Statistics and Aggregation Methods)
    // ========================================================================

    /**
     * 활성 상태별 제품 수 집계
     *
     * @return 활성 상태별 제품 수 (Object[]: [isActive, count])
     */
    @Query("SELECT p.isActive, COUNT(p) FROM ProductCodeMapping p " +
            "GROUP BY p.isActive")
    List<Object[]> countByIsActive();

    /**
     * 본사별 제품 수 집계
     *
     * @return 본사별 제품 수 (Object[]: [headquartersId, count])
     */
    @Query("SELECT p.headquartersId, COUNT(p) FROM ProductCodeMapping p " +
            "GROUP BY p.headquartersId " +
            "ORDER BY COUNT(p) DESC")
    List<Object[]> countByHeadquarters();

    /**
     * 전체 제품 수 조회
     *
     * @return 전체 제품 수
     */
    @Query("SELECT COUNT(p) FROM ProductCodeMapping p")
    Long countAll();

    /**
     * 활성 제품 수 조회
     *
     * @return 활성 제품 수
     */
    @Query("SELECT COUNT(p) FROM ProductCodeMapping p WHERE p.isActive = true")
    Long countActive();

    // ========================================================================
    // 정렬 및 페이징 지원 메서드 (Sorting and Paging Support Methods)
    // ========================================================================

    /**
     * 회사별 제품 코드 순으로 정렬된 모든 제품 조회
     *
     * @return 매핑 정보 목록 (회사별 제품 코드 순)
     */
    List<ProductCodeMapping> findAllByOrderByCompanyProductCodeAsc();

    /**
     * 제품명 순으로 정렬된 모든 제품 조회
     *
     * @return 매핑 정보 목록 (제품명 순)
     */
    List<ProductCodeMapping> findAllByOrderByProductNameAsc();

    /**
     * 활성 상태 제품을 회사별 제품 코드 순으로 조회
     *
     * @param isActive 활성 상태
     * @return 매핑 정보 목록 (회사별 제품 코드 순)
     */
    List<ProductCodeMapping> findByIsActiveOrderByCompanyProductCodeAsc(Boolean isActive);

    // ========================================================================
    // 서비스 호환성 메서드 (Service Compatibility Methods)
    // ========================================================================

    /**
     * 서비스 호환성을 위한 제품 코드 조회 메서드
     * companyProductCode 필드를 사용하여 조회
     *
     * @param productCode 제품 코드 (실제로는 companyProductCode)
     * @return 매핑 정보 (Optional)
     */
    default Optional<ProductCodeMapping> findByProductCode(String productCode) {
        return findByCompanyProductCode(productCode);
    }
}