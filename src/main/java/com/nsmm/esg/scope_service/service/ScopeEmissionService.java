package com.nsmm.esg.scope_service.service;

import com.nsmm.esg.scope_service.dto.request.ScopeEmissionRequest;
import com.nsmm.esg.scope_service.dto.request.ScopeEmissionUpdateRequest;
import com.nsmm.esg.scope_service.dto.response.ScopeEmissionResponse;
import com.nsmm.esg.scope_service.entity.ScopeEmission;
import com.nsmm.esg.scope_service.entity.ScopeType;
import com.nsmm.esg.scope_service.repository.ScopeEmissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Scope 배출량 서비스 (Scope1/2/3 통합)
 * 
 * 특징:
 * - Scope 1, 2, 3 모든 타입 처리 (scopeType 파라미터로 구분)
 * - 제품 정보는 Scope 1, 2에서만 저장 (Scope 3은 null)
 * - TreePath 기반 권한 제어
 * - 사용자 직접 입력 기반
 * - 중복 데이터 검증
 * 
 * @author ESG Project Team
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScopeEmissionService {

  private final ScopeEmissionRepository scopeEmissionRepository;

  // ========================================================================
  // 생성 메서드 (Creation Methods)
  // ========================================================================

  /**
   * Scope 배출량 데이터 생성
   */
  @Transactional
  public ScopeEmissionResponse create(
      ScopeEmissionRequest request,
      String userType,
      String headquartersId,
      String partnerId,
      String treePath) {

    log.info("Scope 배출량 생성 요청: scopeType={}, categoryNumber={}, userType={}",
        request.getScopeType(), request.getCategoryNumber(), userType);

    // 1. 권한 검증
    validateUserPermission(userType, headquartersId, partnerId, treePath);

    // 2. 중복 데이터 검증
    validateDuplicateData(request, headquartersId);

    // 3. 제품 정보 처리 (Scope 1, 2에서만 사용)
    String companyProduct = null;
    String companyProductCode = null;

    if (request.getScopeType() == ScopeType.SCOPE1 ||
        request.getScopeType() == ScopeType.SCOPE2) {
      companyProduct = request.getCompanyProduct();
      companyProductCode = request.getCompanyProductCode();
    }
    // Scope 3은 제품 정보 무시 (null 유지)

    // 4. 엔티티 생성
    ScopeEmission emission = ScopeEmission.builder()
        .headquartersId(Long.parseLong(headquartersId))
        .partnerId(partnerId != null ? Long.parseLong(partnerId) : null)
        .treePath(treePath)
        .reportingYear(request.getReportingYear())
        .reportingMonth(request.getReportingMonth())
        .scopeType(request.getScopeType())
        .companyProduct(companyProduct)
        .companyProductCode(companyProductCode)
        .majorCategory(request.getMajorCategory())
        .subcategory(request.getSubcategory())
        .rawMaterial(request.getRawMaterial())
        .activityAmount(request.getActivityAmount())
        .unit(request.getUnit())
        .emissionFactor(request.getEmissionFactor())
        .totalEmission(request.getTotalEmission())
        .isManualInput(request.getIsManualInput() != null ? request.getIsManualInput() : false)
        .build();

    // 5. 카테고리 정보 설정
    setCategoryInfo(emission, request);

    // 6. 저장 및 응답
    ScopeEmission savedEmission = scopeEmissionRepository.save(emission);
    log.info("Scope 배출량 생성 완료: id={}", savedEmission.getId());

    return mapToResponse(savedEmission);
  }

  // ========================================================================
  // 조회 메서드 (Read Methods)
  // ========================================================================

  /**
   * 배출량 데이터 단건 조회
   */
  public ScopeEmissionResponse getById(
      Long id,
      String userId,
      String userType,
      String treePath) {

    log.info("배출량 단건 조회 요청: id={}, userType={}", id, userType);

    ScopeEmission emission = scopeEmissionRepository.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("배출량 데이터를 찾을 수 없습니다: " + id));

    // 권한 검증
    validateDataAccess(emission, userType, treePath);

    return mapToResponse(emission);
  }

  /**
   * 배출량 데이터 목록 조회 (조건별)
   */
  public List<ScopeEmissionResponse> getList(
      ScopeType scopeType,
      Integer year,
      Integer month,
      Integer categoryNumber,
      String userType,
      String headquartersId,
      String partnerId,
      String treePath) {

    log.info("배출량 목록 조회 요청: scopeType={}, year={}, month={}, categoryNumber={}, userType={}",
        scopeType, year, month, categoryNumber, userType);

    List<ScopeEmission> emissions;
    Long hqId = Long.parseLong(headquartersId);

    // 조건별 조회
    if (scopeType != null && year != null && month != null && categoryNumber != null) {
      // 모든 조건이 있는 경우
      emissions = getEmissionsByAllConditions(scopeType, hqId, year, month, categoryNumber);
    } else if (scopeType != null && year != null && month != null) {
      // Scope 타입, 연도, 월만 있는 경우
      emissions = scopeEmissionRepository.findByHeadquartersIdAndReportingYearAndReportingMonthAndScopeType(
          hqId, year, month, scopeType);
    } else if (scopeType != null && year != null) {
      // Scope 타입, 연도만 있는 경우
      emissions = scopeEmissionRepository.findByHeadquartersIdAndReportingYearAndScopeType(
          hqId, year, scopeType);
    } else if (scopeType != null) {
      // Scope 타입만 있는 경우
      emissions = scopeEmissionRepository.findByHeadquartersIdAndScopeType(hqId, scopeType);
    } else if (year != null && month != null) {
      // 연도, 월만 있는 경우
      emissions = scopeEmissionRepository.findByHeadquartersIdAndReportingYearAndReportingMonth(
          hqId, year, month);
    } else if (year != null) {
      // 연도만 있는 경우
      emissions = scopeEmissionRepository.findByHeadquartersIdAndReportingYear(hqId, year);
    } else {
      // 조건이 없는 경우 (전체 조회)
      emissions = scopeEmissionRepository.findByHeadquartersId(hqId);
    }

    // 권한 필터링 (TreePath 기반)
    emissions = filterByPermission(emissions, userType, treePath);

    return emissions.stream()
        .map(this::mapToResponse)
        .collect(Collectors.toList());
  }

  /**
   * 배출량 데이터 페이징 조회
   */
  public Page<ScopeEmissionResponse> getPage(
      ScopeType scopeType,
      String userType,
      String headquartersId,
      String treePath,
      Pageable pageable) {

    log.info("배출량 페이징 조회 요청: scopeType={}, userType={}", scopeType, userType);

    Long hqId = Long.parseLong(headquartersId);
    Page<ScopeEmission> emissionPage;

    if (scopeType != null) {
      emissionPage = scopeEmissionRepository.findByHeadquartersIdAndScopeType(hqId, scopeType, pageable);
    } else {
      emissionPage = scopeEmissionRepository.findByHeadquartersId(hqId, pageable);
    }

    return emissionPage.map(this::mapToResponse);
  }

  // ========================================================================
  // 수정 메서드 (Update Methods)
  // ========================================================================

  /**
   * 배출량 데이터 수정
   */
  @Transactional
  public ScopeEmissionResponse update(
      Long id,
      ScopeEmissionUpdateRequest request,
      String userType,
      String headquartersId,
      String partnerId,
      String treePath) {

    log.info("배출량 수정 요청: id={}, userType={}", id, userType);

    // 1. 기존 데이터 조회
    ScopeEmission existingEmission = scopeEmissionRepository.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("수정할 배출량 데이터를 찾을 수 없습니다: " + id));

    // 2. 권한 검증
    validateDataAccess(existingEmission, userType, treePath);

    // 3. 제품 정보 처리 (Scope 1, 2에서만 사용)
    if (existingEmission.getScopeType() == ScopeType.SCOPE1 ||
        existingEmission.getScopeType() == ScopeType.SCOPE2) {
      // Scope 1, 2는 제품 정보 수정 가능
      if (request.getCompanyProduct() != null) {
        existingEmission.setCompanyProduct(request.getCompanyProduct());
      }
      if (request.getCompanyProductCode() != null) {
        existingEmission.setCompanyProductCode(request.getCompanyProductCode());
      }
    }
    // Scope 3은 제품 정보 수정 불가 (기존 null 유지)

    // 4. 기타 필드 수정
    if (request.getMajorCategory() != null) {
      existingEmission.setMajorCategory(request.getMajorCategory());
    }
    if (request.getSubcategory() != null) {
      existingEmission.setSubcategory(request.getSubcategory());
    }
    if (request.getRawMaterial() != null) {
      existingEmission.setRawMaterial(request.getRawMaterial());
    }
    if (request.getActivityAmount() != null) {
      existingEmission.setActivityAmount(java.math.BigDecimal.valueOf(request.getActivityAmount()));
    }
    if (request.getUnit() != null) {
      existingEmission.setUnit(request.getUnit());
    }
    if (request.getEmissionFactor() != null) {
      existingEmission.setEmissionFactor(java.math.BigDecimal.valueOf(request.getEmissionFactor()));
    }
    if (request.getTotalEmission() != null) {
      existingEmission.setTotalEmission(java.math.BigDecimal.valueOf(request.getTotalEmission()));
    }
    if (request.getIsManualInput() != null) {
      existingEmission.setIsManualInput(request.getIsManualInput());
    }
    // 카테고리 정보도 수정
    if (request.getCategoryNumber() != null && request.getCategoryName() != null) {
      switch (existingEmission.getScopeType()) {
        case SCOPE1:
          existingEmission.setScope1CategoryNumber(request.getCategoryNumber());
          existingEmission.setScope1CategoryName(request.getCategoryName());
          break;
        case SCOPE2:
          existingEmission.setScope2CategoryNumber(request.getCategoryNumber());
          existingEmission.setScope2CategoryName(request.getCategoryName());
          break;
        case SCOPE3:
          existingEmission.setScope3CategoryNumber(request.getCategoryNumber());
          existingEmission.setScope3CategoryName(request.getCategoryName());
          break;
      }
    }

    // 5. 저장 및 응답
    ScopeEmission updatedEmission = scopeEmissionRepository.save(existingEmission);
    log.info("배출량 수정 완료: id={}", updatedEmission.getId());

    return mapToResponse(updatedEmission);
  }

  // ========================================================================
  // 삭제 메서드 (Delete Methods)
  // ========================================================================

  /**
   * 배출량 데이터 삭제
   */
  @Transactional
  public void delete(
      Long id,
      String userType,
      String headquartersId,
      String partnerId,
      String treePath) {

    log.info("배출량 삭제 요청: id={}, userType={}", id, userType);

    // 1. 기존 데이터 조회
    ScopeEmission emission = scopeEmissionRepository.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("삭제할 배출량 데이터를 찾을 수 없습니다: " + id));

    // 2. 권한 검증
    validateDataAccess(emission, userType, treePath);

    // 3. 삭제
    scopeEmissionRepository.delete(emission);
    log.info("배출량 삭제 완료: id={}", id);
  }

  // ========================================================================
  // 집계 메서드 (Aggregation Methods)
  // ========================================================================

  /**
   * 연도별 총 배출량 집계
   */
  public BigDecimal getTotalEmissionByYear(Long headquartersId, Integer year) {
    return scopeEmissionRepository.sumTotalEmissionByHeadquartersIdAndYear(headquartersId, year)
        .orElse(BigDecimal.ZERO);
  }

  /**
   * Scope 타입별 총 배출량 집계
   */
  public BigDecimal getTotalEmissionByScopeType(Long headquartersId, Integer year, ScopeType scopeType) {
    return scopeEmissionRepository.sumTotalEmissionByHeadquartersIdAndYearAndScopeType(headquartersId, year, scopeType)
        .orElse(BigDecimal.ZERO);
  }

  /**
   * TreePath 기반 하위 조직 배출량 집계
   */
  public BigDecimal getTotalEmissionByTreePath(String treePath, Integer year, Integer month) {
    return scopeEmissionRepository.sumTotalEmissionByTreePathAndYearAndMonth(treePath, year, month)
        .orElse(BigDecimal.ZERO);
  }

  // ========================================================================
  // 유틸리티 메서드 (Utility Methods)
  // ========================================================================

  private void validateUserPermission(String userType, String headquartersId, String partnerId, String treePath) {
    if ("HEADQUARTERS".equals(userType)) {
      if (headquartersId == null) {
        throw new IllegalArgumentException("본사 사용자는 본사 ID가 필요합니다");
      }
    } else if ("PARTNER".equals(userType)) {
      if (partnerId == null || treePath == null) {
        throw new IllegalArgumentException("협력사 사용자는 협력사 ID와 TreePath가 필요합니다");
      }
    } else {
      throw new IllegalArgumentException("유효하지 않은 사용자 타입입니다: " + userType);
    }
  }

  private void validateDuplicateData(ScopeEmissionRequest request, String headquartersId) {
    Long hqId = Long.parseLong(headquartersId);
    boolean exists = false;
    switch (request.getScopeType()) {
      case SCOPE1:
        exists = scopeEmissionRepository
            .existsByHeadquartersIdAndScopeTypeAndScope1CategoryNumberAndReportingYearAndReportingMonth(
                hqId, request.getScopeType(), request.getCategoryNumber(),
                request.getReportingYear(), request.getReportingMonth());
        break;
      case SCOPE2:
        exists = scopeEmissionRepository
            .existsByHeadquartersIdAndScopeTypeAndScope2CategoryNumberAndReportingYearAndReportingMonth(
                hqId, request.getScopeType(), request.getCategoryNumber(),
                request.getReportingYear(), request.getReportingMonth());
        break;
      case SCOPE3:
        exists = scopeEmissionRepository
            .existsByHeadquartersIdAndScopeTypeAndScope3CategoryNumberAndReportingYearAndReportingMonth(
                hqId, request.getScopeType(), request.getCategoryNumber(),
                request.getReportingYear(), request.getReportingMonth());
        break;
    }
    if (exists) {
      throw new IllegalArgumentException("동일한 조건의 배출량 데이터가 이미 존재합니다");
    }
  }

  private void validateDataAccess(ScopeEmission emission, String userType, String treePath) {
    if ("PARTNER".equals(userType)) {
      if (!emission.getTreePath().startsWith(treePath)) {
        throw new IllegalArgumentException("해당 데이터에 접근할 권한이 없습니다");
      }
    }
  }

  private List<ScopeEmission> filterByPermission(List<ScopeEmission> emissions, String userType, String treePath) {
    if ("PARTNER".equals(userType)) {
      return emissions.stream()
          .filter(emission -> emission.getTreePath().startsWith(treePath))
          .collect(Collectors.toList());
    }
    return emissions;
  }

  private void setCategoryInfo(ScopeEmission emission, ScopeEmissionRequest request) {
    switch (request.getScopeType()) {
      case SCOPE1:
        emission.setScope1CategoryNumber(request.getCategoryNumber());
        emission.setScope1CategoryName(request.getCategoryName());
        break;
      case SCOPE2:
        emission.setScope2CategoryNumber(request.getCategoryNumber());
        emission.setScope2CategoryName(request.getCategoryName());
        break;
      case SCOPE3:
        emission.setScope3CategoryNumber(request.getCategoryNumber());
        emission.setScope3CategoryName(request.getCategoryName());
        break;
    }
  }

  private List<ScopeEmission> getEmissionsByAllConditions(
      ScopeType scopeType, Long headquartersId, Integer year, Integer month, Integer categoryNumber) {

    switch (scopeType) {
      case SCOPE1:
        return scopeEmissionRepository.findByHeadquartersIdAndScopeTypeAndScope1CategoryNumber(
            headquartersId, scopeType, categoryNumber);
      case SCOPE2:
        return scopeEmissionRepository.findByHeadquartersIdAndScopeTypeAndScope2CategoryNumber(
            headquartersId, scopeType, categoryNumber);
      case SCOPE3:
        return scopeEmissionRepository.findByHeadquartersIdAndScopeTypeAndScope3CategoryNumber(
            headquartersId, scopeType, categoryNumber);
      default:
        throw new IllegalArgumentException("유효하지 않은 Scope 타입입니다: " + scopeType);
    }
  }

  private ScopeEmissionResponse mapToResponse(ScopeEmission emission) {
    return ScopeEmissionResponse.builder()
        .id(emission.getId())
        .scopeType(emission.getScopeType())
        .categoryNumber(getCategoryNumber(emission))
        .categoryName(getCategoryName(emission))
        .companyProduct(emission.getCompanyProduct())
        .companyProductCode(emission.getCompanyProductCode())
        .majorCategory(emission.getMajorCategory())
        .subcategory(emission.getSubcategory())
        .rawMaterial(emission.getRawMaterial())
        .unit(emission.getUnit())
        .emissionFactor(emission.getEmissionFactor())
        .activityAmount(emission.getActivityAmount())
        .totalEmission(emission.getTotalEmission())
        .isManualInput(emission.getIsManualInput())
        .reportingYear(emission.getReportingYear())
        .reportingMonth(emission.getReportingMonth())
        .headquartersId(emission.getHeadquartersId())
        .partnerId(emission.getPartnerId())
        .treePath(emission.getTreePath())
        .createdAt(emission.getCreatedAt())
        .updatedAt(emission.getUpdatedAt())
        .build();
  }

  private Integer getCategoryNumber(ScopeEmission emission) {
    switch (emission.getScopeType()) {
      case SCOPE1:
        return emission.getScope1CategoryNumber();
      case SCOPE2:
        return emission.getScope2CategoryNumber();
      case SCOPE3:
        return emission.getScope3CategoryNumber();
      default:
        return null;
    }
  }

  private String getCategoryName(ScopeEmission emission) {
    switch (emission.getScopeType()) {
      case SCOPE1:
        return emission.getScope1CategoryName();
      case SCOPE2:
        return emission.getScope2CategoryName();
      case SCOPE3:
        return emission.getScope3CategoryName();
      default:
        return null;
    }
  }
}