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


}