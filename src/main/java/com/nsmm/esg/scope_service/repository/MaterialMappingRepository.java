package com.nsmm.esg.scope_service.repository;

import com.nsmm.esg.scope_service.entity.MaterialMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * MaterialMapping 엔티티의 데이터베이스 접근 레포지토리
 * 
 * 주요 기능:
 * - 자재코드 매핑 CRUD 작업
 * - 협력사별 매핑 조회
 * - 매핑 체인 추적
 * - 하위 할당 관리
 */
@Repository
public interface MaterialMappingRepository extends JpaRepository<MaterialMapping, Long> {

    /**
     * 협력사별 활성 매핑 조회
     */
    @Query("SELECT m FROM MaterialMapping m WHERE m.partnerId = :partnerId AND m.isActive = true AND m.isDeleted = false")
    List<MaterialMapping> findActiveByPartnerId(@Param("partnerId") Long partnerId);

    /**
     * 본사별 모든 매핑 조회 (계층 구조 포함)
     */
    @Query("SELECT m FROM MaterialMapping m WHERE m.headquartersId = :headquartersId AND m.isActive = true AND m.isDeleted = false ORDER BY m.partnerLevel, m.partnerId")
    List<MaterialMapping> findByHeadquartersId(@Param("headquartersId") Long headquartersId);

    /**
     * 상위 자재코드로 매핑 조회
     */
    @Query("SELECT m FROM MaterialMapping m WHERE m.upstreamMaterialCode = :upstreamCode AND m.isActive = true AND m.isDeleted = false")
    List<MaterialMapping> findByUpstreamMaterialCode(@Param("upstreamCode") String upstreamCode);

    /**
     * 내부 자재코드로 매핑 조회
     */
    @Query("SELECT m FROM MaterialMapping m WHERE m.internalMaterialCode = :internalCode AND m.partnerId = :partnerId AND m.isActive = true AND m.isDeleted = false")
    Optional<MaterialMapping> findByInternalMaterialCodeAndPartnerId(@Param("internalCode") String internalCode, @Param("partnerId") Long partnerId);

    /**
     * ScopeEmission ID로 매핑 조회
     */
    @Query("SELECT m FROM MaterialMapping m WHERE m.scopeEmissionId = :scopeEmissionId")
    Optional<MaterialMapping> findByScopeEmissionId(@Param("scopeEmissionId") Long scopeEmissionId);

    /**
     * 매핑 체인 추적 (본사 -> N차 협력사)
     */
    @Query("SELECT m FROM MaterialMapping m WHERE m.headquartersId = :headquartersId AND (m.upstreamMaterialCode = :materialCode OR m.internalMaterialCode = :materialCode) AND m.isActive = true AND m.isDeleted = false ORDER BY m.partnerLevel")
    List<MaterialMapping> findMappingChain(@Param("headquartersId") Long headquartersId, @Param("materialCode") String materialCode);

    /**
     * 하위 할당이 있는 매핑 조회
     */
    @Query("SELECT m FROM MaterialMapping m WHERE m.hasDownstreamAssignment = true AND m.partnerId = :partnerId AND m.isActive = true AND m.isDeleted = false")
    List<MaterialMapping> findWithDownstreamAssignments(@Param("partnerId") Long partnerId);
}