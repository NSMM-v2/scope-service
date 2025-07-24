package com.nsmm.esg.scope_service.repository;

import com.nsmm.esg.scope_service.entity.MaterialAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * MaterialAssignment 엔티티의 데이터베이스 접근 레포지토리
 * 
 * 주요 기능:
 * - 자재코드 할당 CRUD 작업
 * - 협력사별 할당 조회
 * - 할당 체인 추적
 * - 권한 기반 필터링
 */
@Repository
public interface MaterialAssignmentRepository extends JpaRepository<MaterialAssignment, Long> {

    /**
     * 할당받는 협력사별 활성 할당 조회
     */
    @Query("SELECT a FROM MaterialAssignment a WHERE a.toPartnerId = :partnerId AND a.isActive = true")
    List<MaterialAssignment> findActiveByToPartnerId(@Param("partnerId") String partnerId);

    /**
     * 본사별 모든 할당 조회 (계층 구조 포함)
     */
    @Query("SELECT a FROM MaterialAssignment a WHERE a.headquartersId = :headquartersId AND a.isActive = true ORDER BY a.toLevel, a.toPartnerId")
    List<MaterialAssignment> findByHeadquartersId(@Param("headquartersId") Long headquartersId);

    /**
     * 자재코드로 할당 조회 (중복 체크용)
     */
    @Query("SELECT a FROM MaterialAssignment a WHERE a.materialCode = :materialCode AND a.toPartnerId = :partnerId AND a.isActive = true")
    Optional<MaterialAssignment> findByMaterialCodeAndToPartnerId(@Param("materialCode") String materialCode, @Param("partnerId") String partnerId);

    /**
     * 레벨별 맵핑된 자재 정보 조회 (자재코드, 자재명, 자재설명)
     * isMapped = true이고 isActive = true인 항목만 조회
     */
    @Query("SELECT DISTINCT ma.materialCode, ma.materialName, ma.materialDescription " +
           "FROM MaterialAssignment ma " +
           "WHERE ma.headquartersId = :headquartersId " +
           "AND ma.toLevel = :targetLevel " +
           "AND ma.isMapped = true " +
           "AND ma.isActive = true " +
           "ORDER BY ma.materialCode")
    List<Object[]> findMappedMaterialsByLevel(
            @Param("headquartersId") Long headquartersId,
            @Param("targetLevel") Integer targetLevel);

}