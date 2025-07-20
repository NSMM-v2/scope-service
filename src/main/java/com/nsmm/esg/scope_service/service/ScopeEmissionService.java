package com.nsmm.esg.scope_service.service;

import com.nsmm.esg.scope_service.dto.request.ScopeEmissionRequest;
import com.nsmm.esg.scope_service.dto.request.ScopeEmissionUpdateRequest;
import com.nsmm.esg.scope_service.dto.response.ScopeEmissionResponse;
import com.nsmm.esg.scope_service.entity.*;
import com.nsmm.esg.scope_service.enums.Scope1Category;
import com.nsmm.esg.scope_service.enums.Scope2Category;
import com.nsmm.esg.scope_service.enums.Scope3Category;
import com.nsmm.esg.scope_service.enums.ScopeType;
import com.nsmm.esg.scope_service.repository.ScopeEmissionRepository;
import com.nsmm.esg.scope_service.repository.MaterialMappingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

// ============================================================================
// 통합 Scope 배출량 서비스 (Scope 1, 2, 3 통합 지원)
// ============================================================================

/**
 * 통합 Scope 배출량 서비스
 * 
 * 주요 기능:
 * - 모든 Scope 타입 배출량 데이터 관리
 * - 권한 기반 데이터 접근 제어 (본사/협력사 구분)
 * - Scope 1, 2에서 제품 코드/제품명 선택적 처리
 * - 프론트엔드 계산값 그대로 저장
 * - 중복 데이터 검증 및 트랜잭션 관리
 * 비즈니스 규칙:
 * - Scope 1, 2: 제품 코드/제품명 선택적 (null 허용)
 * - Scope 3: 모든 필드 필수 (기존 로직 유지)
 * - 본사: 모든 하위 조직 데이터 접근 가능
 * - 협력사: 본인 및 하위 조직 데이터만 접근 가능
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScopeEmissionService {

  private final ScopeEmissionRepository scopeEmissionRepository;
  private final MaterialMappingRepository materialMappingRepository;

  // ============================================================================
  // 생성 메서드
  // ============================================================================

  /**
   * 통합 Scope 배출량 데이터 생성
   * 
   * 특징:
   * - Scope 1, 2: 제품 코드/제품명 선택적 처리
   * - Scope 3: 모든 필드 필수 (기존 로직 유지)
   * - 프론트엔드 계산값 그대로 저장
   * - 중복 검증 제거로 자유로운 데이터 입력 허용
   */
  @Transactional
  public ScopeEmissionResponse createScopeEmission(
      ScopeEmissionRequest request,
      String userType,
      String headquartersId,
      String partnerId,
      String treePath) {

    try {
      log.info("Scope 배출량 데이터 생성 시작: scopeType={}, userType={}",
          request.getScopeType(), userType);

      // 1. 사용자 인증 및 권한 검증
      Long finalHeadquartersId;
      Long finalPartnerId = null;

      if ("HEADQUARTERS".equals(userType)) {
        finalHeadquartersId = Long.parseLong(headquartersId);
        log.debug("본사 사용자 인증 완료: headquartersId={}", finalHeadquartersId);
      } else {
        finalHeadquartersId = Long.parseLong(headquartersId);
        finalPartnerId = Long.parseLong(partnerId);
        log.debug("협력사 사용자 인증 완료: headquartersId={}, partnerId={}",
            finalHeadquartersId, finalPartnerId);
      }

      // 2. 기본 필드 검증
      validateBasicFields(request);

      // 4. 엔티티 생성
      ScopeEmission emission = createScopeEmissionEntity(
          request, finalHeadquartersId, finalPartnerId, treePath);

      // 5. 저장 및 응답
      ScopeEmission savedEmission = scopeEmissionRepository.save(emission);
      log.info("Scope 배출량 데이터 생성 완료: id={}, scopeType={}, totalEmission={}",
          savedEmission.getId(), savedEmission.getScopeType(), savedEmission.getTotalEmission());

      return ScopeEmissionResponse.from(savedEmission);

    } catch (Exception e) {
      log.error("Scope 배출량 데이터 생성 중 오류 발생", e);
      throw e;
    }
  }

  // ============================================================================
  // 조회 메서드
  // ============================================================================

  // Scope 타입별 배출량 데이터 조회 (본인 데이터만)
  public List<ScopeEmissionResponse> getEmissionsByScope(
      ScopeType scopeType,
      String accountNumber,
      String userType,
      String headquartersId,
      String partnerId,
      String treePath) {

    log.info("Scope {} 배출량 조회: accountNumber={}, userType={}", scopeType, accountNumber, userType);
    validateUserPermissions(userType, headquartersId, partnerId, treePath);

    List<ScopeEmission> emissions;
    if ("HEADQUARTERS".equals(userType)) {
      // 본사: 본인의 본사 데이터만 조회 (하위 협력사 데이터 제외)
      emissions = scopeEmissionRepository.findByHeadquartersIdAndPartnerIdIsNullAndScopeType(
          Long.parseLong(headquartersId), scopeType);
      log.info("본사 본인 데이터 조회: headquartersId={}, 조회된 건수={}", headquartersId, emissions.size());
    } else if ("PARTNER".equals(userType)) {
      // 협력사: 본인의 협력사 데이터만 조회
      emissions = scopeEmissionRepository.findByPartnerIdAndScopeType(
          Long.parseLong(partnerId), scopeType);
      log.info("협력사 본인 데이터 조회: partnerId={}, 조회된 건수={}", partnerId, emissions.size());
    } else {
      throw new IllegalArgumentException("알 수 없는 사용자 유형입니다: " + userType);
    }

    return emissions.stream()
        .map(ScopeEmissionResponse::from)
        .collect(Collectors.toList());
  }



  // Scope 배출량 데이터 수정
  @Transactional
  public ScopeEmissionResponse updateScopeEmission(
      Long id,
      ScopeEmissionUpdateRequest request,
      String userType,
      String headquartersId,
      String partnerId,
      String treePath) {

    log.info("Scope 배출량 데이터 수정: id={}, userType={}", id, userType);

    // 1. 기존 데이터 조회
    ScopeEmission existingEmission = scopeEmissionRepository.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("배출량 데이터를 찾을 수 없습니다: " + id));

    // 2. 수정 권한 검증
    validateUpdatePermissions(existingEmission, userType, headquartersId, partnerId, treePath);

    // 3. 부분 업데이트 수행
    ScopeEmission updatedEmission = performPartialUpdate(existingEmission, request);

    // 4. 저장 및 응답
    ScopeEmission savedEmission = scopeEmissionRepository.save(updatedEmission);
    log.info("Scope 배출량 데이터 수정 완료: id={}", savedEmission.getId());

    return ScopeEmissionResponse.from(savedEmission);
  }

  // 배출량 데이터 삭제
  @Transactional
  public void deleteScopeEmission(
      Long id,
      String userType,
      String headquartersId,
      String partnerId,
      String treePath) {

    log.info("Scope 배출량 데이터 삭제 시작: id={}, userType={}", id, userType);

    // 1. 기존 데이터 조회
    ScopeEmission emission = scopeEmissionRepository.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("배출량 데이터를 찾을 수 없습니다: " + id));

    // 2. 권한 검증
    validateDeletePermissions(emission, userType, headquartersId, partnerId, treePath);

    // 3. 삭제
    scopeEmissionRepository.delete(emission);
    log.info("Scope 배출량 데이터 삭제 완료: id={}", id);
  }

  // ============================================================================
  // 유효성 검증 메서드
  // ============================================================================

  /**
   * 사용자 권한 검증
   * 
   * @param userType       사용자 타입
   * @param headquartersId 본사 ID
   * @param partnerId      협력사 ID
   * @param treePath       계층 경로
   */
  private void validateUserPermissions(String userType, String headquartersId, String partnerId, String treePath) {
    if (userType == null) {
      throw new IllegalArgumentException("사용자 타입이 필요합니다");
    }

    if ("HEADQUARTERS".equals(userType)) {
      if (headquartersId == null) {
        throw new IllegalArgumentException("본사 ID가 필요합니다");
      }
    } else if ("PARTNER".equals(userType)) {
      if (partnerId == null || treePath == null) {
        throw new IllegalArgumentException("협력사 ID와 TreePath가 필요합니다");
      }
    } else {
      throw new IllegalArgumentException("알 수 없는 사용자 타입입니다: " + userType);
    }
  }

  /**
   * 기본 필드 검증 (계산 검증 제거)
   * 
   * @param request 검증할 요청 데이터
   */
  private void validateBasicFields(ScopeEmissionRequest request) {
    if (request.getTotalEmission() == null) {
      throw new IllegalArgumentException("총 배출량은 필수입니다");
    }

    if (request.getInputType() == null) {
      throw new IllegalArgumentException("입력 타입은 필수입니다");
    }

    if (request.getHasProductMapping() == null) {
      throw new IllegalArgumentException("제품 코드 매핑 여부는 필수입니다");
    }

    // 제품 코드 매핑 검증
    if (Boolean.TRUE.equals(request.getHasProductMapping())) {
      if (request.getScopeType() == ScopeType.SCOPE3) {
        throw new IllegalArgumentException("Scope 3는 제품 코드 매핑을 설정할 수 없습니다");
      }
      if (request.getCompanyProductCode() == null || request.getCompanyProductCode().trim().isEmpty()) {
        throw new IllegalArgumentException("제품 코드 매핑이 설정된 경우 제품 코드는 필수입니다");
      }
      if (request.getProductName() == null || request.getProductName().trim().isEmpty()) {
        throw new IllegalArgumentException("제품 코드 매핑이 설정된 경우 제품명은 필수입니다");
      }
    }

    // Scope 3 추가 필드 검증
    if (request.getScopeType() == ScopeType.SCOPE3) {
      validateScope3RequiredFields(request);
    }
  }

  /**
   * Scope 3 필수 필드 검증
   * 
   * @param request 검증할 요청 데이터
   */
  private void validateScope3RequiredFields(ScopeEmissionRequest request) {
    if (request.getCategoryNumber() == null) {
      throw new IllegalArgumentException("Scope 3 카테고리 번호는 필수입니다");
    }
    if (request.getMajorCategory() == null || request.getMajorCategory().trim().isEmpty()) {
      throw new IllegalArgumentException("Scope 3 주요 카테고리는 필수입니다");
    }
    if (request.getSubcategory() == null || request.getSubcategory().trim().isEmpty()) {
      throw new IllegalArgumentException("Scope 3 세부 카테고리는 필수입니다");
    }
    if (request.getRawMaterial() == null || request.getRawMaterial().trim().isEmpty()) {
      throw new IllegalArgumentException("Scope 3 원재료는 필수입니다");
    }
  }



  /**
   * Scope 배출량 엔티티 생성
   * 
   * @param request        요청 데이터
   * @param headquartersId 본사 ID
   * @param partnerId      협력사 ID
   * @param treePath       계층 경로
   * @return 생성된 엔티티
   */
  private ScopeEmission createScopeEmissionEntity(
      ScopeEmissionRequest request,
      Long headquartersId,
      Long partnerId,
      String treePath) {

    ScopeEmission.ScopeEmissionBuilder builder = ScopeEmission.builder()
        .headquartersId(headquartersId)
        .partnerId(partnerId)
        .treePath(treePath)
        .scopeType(request.getScopeType())
        .reportingYear(request.getReportingYear())
        .reportingMonth(request.getReportingMonth())
        .majorCategory(request.getMajorCategory())
        .subcategory(request.getSubcategory())
        .rawMaterial(request.getRawMaterial())
        .activityAmount(request.getActivityAmount())
        .unit(request.getUnit())
        .emissionFactor(request.getEmissionFactor())
        .totalEmission(request.getTotalEmission())
        .inputType(request.getInputType())
        .hasProductMapping(request.getHasProductMapping())
        .factoryEnabled(request.getFactoryEnabled());

    // Scope 타입별 카테고리 설정
    switch (request.getScopeType()) {
      case SCOPE1:
        if (request.getScope1CategoryNumber() != null) {
          Scope1Category category = Scope1Category.fromCategoryNumber(request.getScope1CategoryNumber());
          builder.scope1CategoryNumber(category.getCategoryNumber())
              .scope1CategoryName(category.getCategoryName())
              .scope1CategoryGroup(category.getGroupName());
        }
        break;
      case SCOPE2:
        if (request.getScope2CategoryNumber() != null) {
          Scope2Category category = Scope2Category.fromCategoryNumber(request.getScope2CategoryNumber());
          builder.scope2CategoryNumber(category.getCategoryNumber())
              .scope2CategoryName(category.getCategoryName());
        }
        break;
      case SCOPE3:
        if (request.getScope3CategoryNumber() != null) {
          Scope3Category category = Scope3Category.fromCategoryNumber(request.getScope3CategoryNumber());
          builder.scope3CategoryNumber(category.getCategoryNumber())
              .scope3CategoryName(category.getCategoryName());
        }
        break;
    }

    // 자재코드 매핑이 설정된 경우 MaterialMapping 생성 및 연결 (저장 후 처리)
    ScopeEmission emission = builder.build();
    
    if (Boolean.TRUE.equals(request.getHasProductMapping())) {
      // ScopeEmission을 먼저 저장하여 ID를 얻음
      ScopeEmission savedEmission = scopeEmissionRepository.save(emission);
      
      // MaterialMapping 생성 및 scopeEmissionId 설정
      MaterialMapping materialMapping = createMaterialMapping(request, headquartersId, partnerId, treePath)
          .toBuilder()
          .scopeEmissionId(savedEmission.getId())
          .build();
      
      // MaterialMapping 저장
      MaterialMapping savedMapping = materialMappingRepository.save(materialMapping);
      
      // MaterialMapping과 연결된 ScopeEmission 업데이트
      savedEmission = savedEmission.setMaterialMapping(savedMapping);
      return scopeEmissionRepository.save(savedEmission);
    }
    
    return scopeEmissionRepository.save(emission);
  }

  /**
   * 수정 권한 검증
   * 
   * @param existingEmission 기존 배출량 데이터
   * @param userType         사용자 타입
   * @param headquartersId   본사 ID
   * @param partnerId        협력사 ID
   * @param treePath         계층 경로
   */
  private void validateUpdatePermissions(
      ScopeEmission existingEmission,
      String userType,
      String headquartersId,
      String partnerId,
      String treePath) {

    validateUserPermissions(userType, headquartersId, partnerId, treePath);

    if ("HEADQUARTERS".equals(userType)) {
      // 본사: 자신의 데이터만 수정 가능
      if (!existingEmission.getHeadquartersId().equals(Long.parseLong(headquartersId))) {
        throw new IllegalArgumentException("해당 데이터를 수정할 권한이 없습니다");
      }
    } else if ("PARTNER".equals(userType)) {
      // 협력사: 본인 데이터만 수정 가능 (하위 조직 데이터는 수정 불가)
      if (!existingEmission.getPartnerId().equals(Long.parseLong(partnerId)) ||
          !existingEmission.getTreePath().equals(treePath)) {
        throw new IllegalArgumentException("해당 데이터를 수정할 권한이 없습니다");
      }
    }
  }

  /**
   * 삭제 권한 검증
   * 
   * @param emission       삭제할 배출량 데이터
   * @param userType       사용자 타입
   * @param headquartersId 본사 ID
   * @param partnerId      협력사 ID
   * @param treePath       계층 경로
   */
  private void validateDeletePermissions(
      ScopeEmission emission,
      String userType,
      String headquartersId,
      String partnerId,
      String treePath) {

    validateUserPermissions(userType, headquartersId, partnerId, treePath);

    if ("HEADQUARTERS".equals(userType)) {
      // 본사: 자신의 데이터만 삭제 가능
      if (!emission.getHeadquartersId().equals(Long.parseLong(headquartersId))) {
        throw new IllegalArgumentException("해당 데이터를 삭제할 권한이 없습니다");
      }
    } else if ("PARTNER".equals(userType)) {
      // 협력사: 본인 데이터만 삭제 가능 (하위 조직 데이터는 삭제 불가)
      if (!emission.getPartnerId().equals(Long.parseLong(partnerId)) ||
          !emission.getTreePath().equals(treePath)) {
        throw new IllegalArgumentException("해당 데이터를 삭제할 권한이 없습니다");
      }
    }
  }

  /**
   * 부분 업데이트 수행
   * 
   * @param existingEmission 기존 배출량 데이터
   * @param request          업데이트 요청 데이터
   * @return 업데이트된 엔티티
   */
  private ScopeEmission performPartialUpdate(ScopeEmission existingEmission, ScopeEmissionUpdateRequest request) {
    ScopeEmission.ScopeEmissionBuilder builder = existingEmission.toBuilder();

    // 입력 모드 업데이트
    if (request.getInputType() != null) {
      builder.inputType(request.getInputType());
    }

    // 제품 코드 매핑 업데이트
    if (request.getHasProductMapping() != null) {
      builder.hasProductMapping(request.getHasProductMapping());

      // 제품 코드 매핑이 true인 경우 제품 코드 정보 필수 검증
      if (Boolean.TRUE.equals(request.getHasProductMapping())) {
        if (existingEmission.getScopeType() == ScopeType.SCOPE3) {
          throw new IllegalArgumentException("Scope 3는 제품 코드 매핑을 설정할 수 없습니다");
        }
        if ((request.getCompanyProductCode() == null || request.getCompanyProductCode().trim().isEmpty()) &&
            (request.getProductName() == null || request.getProductName().trim().isEmpty())) {
          throw new IllegalArgumentException("제품 코드 매핑이 설정된 경우 제품 코드와 제품명은 필수입니다");
        }
      }
    }

    // 공장 설비 활성화 여부 업데이트
    if (request.getFactoryEnabled() != null) {
      builder.factoryEnabled(request.getFactoryEnabled());
    }

    // 기존 필드 업데이트
    if (request.getMajorCategory() != null) {
      builder.majorCategory(request.getMajorCategory());
    }
    if (request.getSubcategory() != null) {
      builder.subcategory(request.getSubcategory());
    }
    if (request.getRawMaterial() != null) {
      builder.rawMaterial(request.getRawMaterial());
    }
    if (request.getUnit() != null) {
      builder.unit(request.getUnit());
    }
    if (request.getEmissionFactor() != null) {
      builder.emissionFactor(request.getEmissionFactor());
    }
    if (request.getActivityAmount() != null) {
      builder.activityAmount(request.getActivityAmount());
    }
    if (request.getTotalEmission() != null) {
      builder.totalEmission(request.getTotalEmission());
    }

    // 자재코드 매핑 업데이트
    if (Boolean.TRUE.equals(request.getHasProductMapping())) {
      // 기존 매핑이 있으면 업데이트, 없으면 새로 생성
      MaterialMapping materialMapping = existingEmission.getMaterialMapping();
      if (materialMapping != null) {
        // 기존 매핑 업데이트
        materialMapping = materialMapping.toBuilder()
            .internalMaterialCode(request.getCompanyProductCode())
            .materialName(request.getProductName())
            .materialDescription("업데이트된 자재: " + request.getProductName())
            .build();
      } else {
        // 새로운 매핑 생성
        materialMapping = createMaterialMapping(request, 
            existingEmission.getHeadquartersId(), 
            existingEmission.getPartnerId(), 
            existingEmission.getTreePath())
            .toBuilder()
            .scopeEmissionId(existingEmission.getId())
            .build();
      }
      
      // MaterialMapping 저장 또는 업데이트
      MaterialMapping savedMapping = materialMappingRepository.save(materialMapping);
      builder.materialMapping(savedMapping);
    } else {
      // 매핑이 false인 경우 매핑 제거
      builder.materialMapping(null);
    }

    return builder.build();
  }

  // ============================================================================
  // 자재코드 매핑 헬퍼 메서드 (Material Mapping Helper Methods)
  // ============================================================================

  /**
   * MaterialMapping 생성
   */
  private MaterialMapping createMaterialMapping(ScopeEmissionRequest request, Long headquartersId, Long partnerId, String treePath) {
    return MaterialMapping.builder()
        .headquartersId(headquartersId)
        .partnerId(partnerId)
        .partnerLevel(calculatePartnerLevel(treePath))
        .treePath(treePath)
        .upstreamMaterialCode(null) // 기존 제품코드는 상위 코드가 없으므로 null
        .internalMaterialCode(request.getCompanyProductCode())
        .materialName(request.getProductName())
        .materialDescription("기존 제품코드에서 전환된 자재: " + request.getProductName())
        .upstreamPartnerId(null) // 기존 제품코드는 상위 협력사가 없으므로 null
        .hasDownstreamAssignment(false)
        .downstreamAssignmentCount(0)
        .mappingDescription("기존 제품코드에서 자재코드 매핑으로 전환")
        .isActive(true)
        .isDeleted(false)
        .build();
  }

  /**
   * MaterialMapping 생성 (업데이트용)
   */
  private MaterialMapping createMaterialMapping(ScopeEmissionUpdateRequest request, Long headquartersId, Long partnerId, String treePath) {
    return MaterialMapping.builder()
        .headquartersId(headquartersId)
        .partnerId(partnerId)
        .partnerLevel(calculatePartnerLevel(treePath))
        .treePath(treePath)
        .upstreamMaterialCode(null) // 기존 제품코드는 상위 코드가 없으므로 null
        .internalMaterialCode(request.getCompanyProductCode())
        .materialName(request.getProductName())
        .materialDescription("기존 제품코드에서 전환된 자재: " + request.getProductName())
        .upstreamPartnerId(null) // 기존 제품코드는 상위 협력사가 없으므로 null
        .hasDownstreamAssignment(false)
        .downstreamAssignmentCount(0)
        .mappingDescription("기존 제품코드에서 자재코드 매핑으로 전환")
        .isActive(true)
        .isDeleted(false)
        .build();
  }

  /**
   * 트리 경로로부터 협력사 레벨 계산
   */
  private Integer calculatePartnerLevel(String treePath) {
    if (treePath == null || treePath.trim().isEmpty()) {
      return 0; // 본사
    }
    
    // treePath 형태: /1/L1-001/L2-003/ -> L1, L2 등으로 레벨 계산
    String[] parts = treePath.split("/");
    int level = 0;
    for (String part : parts) {
      if (part.startsWith("L")) {
        try {
          int partLevel = Integer.parseInt(part.substring(1, 2));
          level = Math.max(level, partLevel);
        } catch (NumberFormatException | StringIndexOutOfBoundsException e) {
          // 파싱 실패 시 기본값 유지
        }
      }
    }
    return level;
  }

}