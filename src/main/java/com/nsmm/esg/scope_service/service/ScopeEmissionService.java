package com.nsmm.esg.scope_service.service;

import com.nsmm.esg.scope_service.dto.request.ScopeEmissionRequest;
import com.nsmm.esg.scope_service.dto.request.ScopeEmissionUpdateRequest;
import com.nsmm.esg.scope_service.dto.response.ScopeEmissionResponse;
import com.nsmm.esg.scope_service.dto.response.ScopeCategoryResponse;
import com.nsmm.esg.scope_service.entity.*;
import com.nsmm.esg.scope_service.repository.ScopeEmissionRepository;
import com.nsmm.esg.scope_service.repository.ProductCodeMappingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
 * 
 * 비즈니스 규칙:
 * - Scope 1, 2: 제품 코드/제품명 선택적 (null 허용)
 * - Scope 3: 모든 필드 필수 (기존 로직 유지)
 * - 본사: 모든 하위 조직 데이터 접근 가능
 * - 협력사: 본인 및 하위 조직 데이터만 접근 가능
 * 
 * @author ESG 프로젝트팀
 * @version 2.0
 * @since 2024
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScopeEmissionService {

  private final ScopeEmissionRepository scopeEmissionRepository;
  private final ProductCodeMappingRepository productCodeMappingRepository;

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
   * 
   * @param request        생성 요청 데이터
   * @param userType       사용자 타입 (HEADQUARTERS | PARTNER)
   * @param headquartersId 본사 ID
   * @param partnerId      협력사 ID (협력사인 경우)
   * @param treePath       계층 경로 (협력사인 경우)
   * @return 생성된 배출량 데이터
   */
  @Transactional
  public ScopeEmissionResponse createScopeEmission(
      ScopeEmissionRequest request,
      String userType,
      String headquartersId,
      String partnerId,
      String treePath) {

    log.info("Scope 배출량 데이터 생성 시작: scopeType={}, userType={}",
        request.getScopeType(), userType);
    log.debug("수신된 요청 데이터: {}", request);

    try {
      // 1. 권한 검증
      validateUserPermissions(userType, headquartersId, partnerId, treePath);

      // 2. 본사/협력사 구분에 따른 ID 처리
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

      // 3. 기본 필드 검증 (계산 검증 제거)
      validateBasicFields(request);

      // 4. 중복 데이터 검증
      validateDuplicateData(request, userType, finalHeadquartersId, finalPartnerId);

      // 5. 제품 코드 처리 (Scope 1, 2에서 선택적)
      processProductCodeMapping(request);

      // 6. 엔티티 생성
      ScopeEmission emission = createScopeEmissionEntity(
          request, finalHeadquartersId, finalPartnerId, treePath);

      // 7. 저장 및 응답
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

  /**
   * 특정 배출량 데이터 조회 (권한 검증 포함)
   * 
   * @param id            배출량 데이터 ID
   * @param accountNumber 계정 번호
   * @param userType      사용자 타입
   * @param treePath      사용자 TreePath
   * @return 배출량 데이터
   */
  public ScopeEmissionResponse getScopeEmissionById(Long id, String accountNumber, String userType, String treePath) {
    log.info("배출량 데이터 조회: id={}, accountNumber={}, userType={}", id, accountNumber, userType);

    ScopeEmission emission = scopeEmissionRepository.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("배출량 데이터를 찾을 수 없습니다: " + id));

    // 권한 검증: 본사는 무조건 허용, 협력사는 본인/하위조직만 허용
    if ("HEADQUARTERS".equals(userType)) {
      // 본사는 모든 데이터 접근 허용
    } else if ("PARTNER".equals(userType)) {
      if (!emission.getTreePath().startsWith(treePath)) {
        throw new IllegalArgumentException("해당 데이터에 접근할 권한이 없습니다");
      }
    } else {
      throw new IllegalArgumentException("알 수 없는 사용자 유형입니다");
    }

    return ScopeEmissionResponse.from(emission);
  }

  /**
   * Scope 타입별 배출량 데이터 조회
   */
  public List<ScopeEmissionResponse> getEmissionsByScope(
      ScopeType scopeType,
      String userType,
      String headquartersId,
      String partnerId,
      String treePath) {

    log.info("Scope {} 배출량 조회: userType={}", scopeType, userType);
    validateUserPermissions(userType, headquartersId, partnerId, treePath);

    List<ScopeEmission> emissions;
    if ("HEADQUARTERS".equals(userType)) {
      emissions = scopeEmissionRepository.findByHeadquartersIdAndScopeType(
          Long.parseLong(headquartersId), scopeType);
    } else {
      emissions = scopeEmissionRepository.findByTreePathStartingWithAndScopeType(treePath, scopeType);
    }

    return emissions.stream()
        .map(ScopeEmissionResponse::from)
        .collect(Collectors.toList());
  }

  /**
   * 연도/월별 배출량 데이터 조회
   */
  public List<ScopeEmissionResponse> getEmissionsByYearAndMonth(
      Integer year,
      Integer month,
      ScopeType scopeType,
      String userType,
      String headquartersId,
      String partnerId,
      String treePath) {

    log.info("연도/월별 배출량 조회: year={}, month={}, scopeType={}", year, month, scopeType);
    validateUserPermissions(userType, headquartersId, partnerId, treePath);

    List<ScopeEmission> emissions;
    if ("HEADQUARTERS".equals(userType)) {
      if (scopeType != null) {
        emissions = scopeEmissionRepository.findByHeadquartersIdAndScopeTypeAndReportingYearAndReportingMonth(
            Long.parseLong(headquartersId), scopeType, year, month);
      } else {
        emissions = scopeEmissionRepository.findByHeadquartersIdAndReportingYearAndReportingMonth(
            Long.parseLong(headquartersId), year, month);
      }
    } else {
      if (scopeType != null) {
        emissions = scopeEmissionRepository
            .findByPartnerIdAndTreePathStartingWithAndScopeTypeAndReportingYearAndReportingMonth(
                Long.parseLong(partnerId), treePath, scopeType, year, month);
      } else {
        emissions = scopeEmissionRepository.findByPartnerIdAndTreePathStartingWithAndReportingYearAndReportingMonth(
            Long.parseLong(partnerId), treePath, year, month);
      }
    }

    return emissions.stream()
        .map(ScopeEmissionResponse::from)
        .collect(Collectors.toList());
  }

  /**
   * 연도/월/카테고리별 배출량 데이터 조회
   */
  public List<ScopeEmissionResponse> getEmissionsByYearAndMonthAndCategory(
      Integer year,
      Integer month,
      ScopeType scopeType,
      Integer categoryNumber,
      String userType,
      String headquartersId,
      String partnerId,
      String treePath) {

    log.info("연도/월/카테고리별 배출량 조회: year={}, month={}, scope={}, category={}",
        year, month, scopeType, categoryNumber);
    validateUserPermissions(userType, headquartersId, partnerId, treePath);

    List<ScopeEmission> emissions;
    if ("HEADQUARTERS".equals(userType)) {
      emissions = findByCategoryNumber(Long.parseLong(headquartersId), null, treePath,
          year, month, scopeType, categoryNumber, true);
    } else {
      emissions = findByCategoryNumber(null, Long.parseLong(partnerId), treePath,
          year, month, scopeType, categoryNumber, false);
    }

    return emissions.stream()
        .map(ScopeEmissionResponse::from)
        .collect(Collectors.toList());
  }

  /**
   * 제품 코드별 배출량 데이터 조회
   */
  public List<ScopeEmissionResponse> getEmissionsByProductCode(
      String productCode,
      ScopeType scopeType,
      String userType,
      String headquartersId,
      String partnerId,
      String treePath) {

    log.info("제품 코드별 배출량 조회: productCode={}, scopeType={}", productCode, scopeType);
    validateUserPermissions(userType, headquartersId, partnerId, treePath);

    List<ScopeEmission> emissions;
    if ("HEADQUARTERS".equals(userType)) {
      emissions = scopeEmissionRepository.findByHeadquartersIdAndCompanyProductCode(
          Long.parseLong(headquartersId), productCode);

      // Scope 타입 필터링
      if (scopeType != null) {
        emissions = emissions.stream()
            .filter(emission -> emission.getScopeType() == scopeType)
            .collect(Collectors.toList());
      }
    } else {
      emissions = scopeEmissionRepository.findByPartnerIdAndTreePathStartingWithAndCompanyProductCode(
          Long.parseLong(partnerId), treePath, productCode);

      // Scope 타입 필터링
      if (scopeType != null) {
        emissions = emissions.stream()
            .filter(emission -> emission.getScopeType() == scopeType)
            .collect(Collectors.toList());
      }
    }

    return emissions.stream()
        .map(ScopeEmissionResponse::from)
        .collect(Collectors.toList());
  }

  /**
   * 연도/월별 Scope 타입별 총계 조회
   */
  public Map<String, BigDecimal> getScopeSummaryByYearAndMonth(
      Integer year,
      Integer month,
      String userType,
      String headquartersId,
      String partnerId,
      String treePath) {

    log.info("연도/월별 Scope 타입별 총계 조회: year={}, month={}", year, month);
    validateUserPermissions(userType, headquartersId, partnerId, treePath);

    Map<String, BigDecimal> summaryMap = Map.of(
        "SCOPE1", BigDecimal.ZERO,
        "SCOPE2", BigDecimal.ZERO,
        "SCOPE3", BigDecimal.ZERO);

    // 각 Scope별로 개별 계산
    for (ScopeType scopeType : ScopeType.values()) {
      BigDecimal total;
      if ("HEADQUARTERS".equals(userType)) {
        total = scopeEmissionRepository.sumTotalEmissionByScopeTypeAndYearAndMonthForHeadquarters(
            Long.parseLong(headquartersId), scopeType, year, month);
      } else {
        total = scopeEmissionRepository.sumTotalEmissionByScopeTypeAndYearAndMonthForPartner(
            Long.parseLong(partnerId), treePath, scopeType, year, month);
      }
      summaryMap.put(scopeType.name(), total != null ? total : BigDecimal.ZERO);
    }

    return summaryMap;
  }

  /**
   * 특정 Scope의 카테고리별 총계 조회
   */
  public Map<Integer, BigDecimal> getCategorySummaryByScope(
      ScopeType scopeType,
      Integer year,
      Integer month,
      String userType,
      String headquartersId,
      String partnerId,
      String treePath) {

    log.info("Scope {} 카테고리별 총계 조회: year={}, month={}", scopeType, year, month);
    validateUserPermissions(userType, headquartersId, partnerId, treePath);

    List<Object[]> results;
    if ("HEADQUARTERS".equals(userType)) {
      results = getCategorySummaryResults(Long.parseLong(headquartersId), null, treePath,
          scopeType, year, month, true);
    } else {
      results = getCategorySummaryResults(null, Long.parseLong(partnerId), treePath,
          scopeType, year, month, false);
    }

    return results.stream()
        .collect(Collectors.toMap(
            result -> (Integer) result[0], // categoryNumber
            result -> (BigDecimal) result[1] // totalEmission
        ));
  }

  // ============================================================================
  // 업데이트 메서드
  // ============================================================================

  /**
   * 통합 Scope 배출량 데이터 수정
   * 
   * 특징:
   * - 부분 업데이트 지원 (null이 아닌 필드만 수정)
   * - 권한 기반 수정 제어
   * - 프론트엔드 계산값 그대로 저장
   * 
   * @param id             수정할 데이터 ID
   * @param request        수정 요청 데이터
   * @param userType       사용자 타입
   * @param headquartersId 본사 ID
   * @param partnerId      협력사 ID
   * @param treePath       계층 경로
   * @return 수정된 배출량 데이터
   */
  @Transactional
  public ScopeEmissionResponse updateScopeEmission(
      Long id,
      ScopeEmissionUpdateRequest request,
      String userType,
      String headquartersId,
      String partnerId,
      String treePath) {

    log.info("Scope 배출량 데이터 업데이트 시작: id={}, userType={}", id, userType);

    // 1. 업데이트할 필드가 있는지 확인
    if (request.getMajorCategory() == null && request.getSubcategory() == null && request.getRawMaterial() == null &&
        request.getCompanyProductCode() == null && request.getProductName() == null && request.getUnit() == null &&
        request.getEmissionFactor() == null && request.getActivityAmount() == null &&
        request.getReportingYear() == null && request.getReportingMonth() == null &&
        request.getTotalEmission() == null) {
      throw new IllegalArgumentException("업데이트할 필드가 없습니다");
    }

    // 2. 기존 데이터 조회
    ScopeEmission existingEmission = scopeEmissionRepository.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("배출량 데이터를 찾을 수 없습니다: " + id));

    // 3. 권한 검증
    validateUpdatePermissions(existingEmission, userType, headquartersId, partnerId, treePath);

    // 4. 중복 데이터 검증 (수정용 - 자기 자신 제외)
    if (request.getReportingYear() != null || request.getReportingMonth() != null) {
      validateDuplicateDataForUpdate(request, existingEmission, userType, id);
    }

    // 5. 부분 업데이트 수행
    ScopeEmission updatedEmission = performPartialUpdate(existingEmission, request);

    // 6. 저장 및 응답
    ScopeEmission savedEmission = scopeEmissionRepository.save(updatedEmission);
    log.info("Scope 배출량 데이터 업데이트 완료: id={}, totalEmission={}",
        savedEmission.getId(), savedEmission.getTotalEmission());

    return ScopeEmissionResponse.from(savedEmission);
  }

  // ============================================================================
  // 삭제 메서드
  // ============================================================================

  /**
   * 배출량 데이터 삭제 (권한 검증 포함)
   * 
   * @param id             삭제할 데이터 ID
   * @param userType       사용자 타입
   * @param headquartersId 본사 ID
   * @param partnerId      협력사 ID
   * @param treePath       계층 경로
   */
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

    // Scope 3만 추가 필드 검증
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
   * 중복 데이터 검증 (생성용)
   * 
   * @param request        검증할 요청 데이터
   * @param userType       사용자 타입
   * @param headquartersId 본사 ID
   * @param partnerId      협력사 ID
   */
  private void validateDuplicateData(
      ScopeEmissionRequest request,
      String userType,
      Long headquartersId,
      Long partnerId) {

    boolean exists;

    if ("HEADQUARTERS".equals(userType)) {
      exists = scopeEmissionRepository.existsByHeadquartersIdAndScopeTypeAndReportingYearAndReportingMonth(
          headquartersId, request.getScopeType(), request.getReportingYear(), request.getReportingMonth());
    } else {
      exists = scopeEmissionRepository.existsByPartnerIdAndScopeTypeAndReportingYearAndReportingMonth(
          partnerId, request.getScopeType(), request.getReportingYear(), request.getReportingMonth());
    }

    if (exists) {
      throw new IllegalArgumentException("동일한 조건의 배출량 데이터가 이미 존재합니다");
    }
  }

  /**
   * 중복 데이터 검증 (수정용)
   * 
   * @param request          검증할 요청 데이터
   * @param existingEmission 기존 배출량 데이터
   * @param userType         사용자 타입
   * @param excludeId        제외할 데이터 ID
   */
  private void validateDuplicateDataForUpdate(
      ScopeEmissionUpdateRequest request,
      ScopeEmission existingEmission,
      String userType,
      Long excludeId) {

    // 핵심 필드가 변경된 경우에만 중복 검증 수행
    Integer finalYear = request.getReportingYear() != null ? request.getReportingYear()
        : existingEmission.getReportingYear();
    Integer finalMonth = request.getReportingMonth() != null ? request.getReportingMonth()
        : existingEmission.getReportingMonth();

    List<ScopeEmission> existingData;

    if ("HEADQUARTERS".equals(userType)) {
      existingData = scopeEmissionRepository.findByHeadquartersIdAndScopeTypeAndReportingYearAndReportingMonth(
          existingEmission.getHeadquartersId(), existingEmission.getScopeType(), finalYear, finalMonth);
    } else {
      existingData = scopeEmissionRepository.findByPartnerIdAndScopeTypeAndReportingYearAndReportingMonth(
          existingEmission.getPartnerId(), existingEmission.getScopeType(), finalYear, finalMonth);
    }

    // 자기 자신을 제외하고 중복 검증
    existingData = existingData.stream()
        .filter(emission -> !emission.getId().equals(excludeId))
        .collect(Collectors.toList());

    if (!existingData.isEmpty()) {
      throw new IllegalArgumentException("동일한 조건의 배출량 데이터가 이미 존재합니다");
    }
  }

  /**
   * 제품 코드 매핑 처리
   * 
   * @param request 처리할 요청 데이터
   */
  private void processProductCodeMapping(ScopeEmissionRequest request) {
    // Scope 1, 2에서 제품 코드가 제공된 경우에만 처리
    if ((request.getScopeType() == ScopeType.SCOPE1 || request.getScopeType() == ScopeType.SCOPE2) &&
        request.getProductCode() != null && !request.getProductCode().trim().isEmpty()) {

      // 제품 코드 매핑 정보 조회
      Optional<ProductCodeMapping> mapping = productCodeMappingRepository.findByProductCode(request.getProductCode());

      if (mapping.isPresent()) {
        // 매핑 정보가 있는 경우 제품명 자동 설정
        if (request.getProductName() == null || request.getProductName().trim().isEmpty()) {
          request.setProductName(mapping.get().getProductName());
          log.debug("제품 코드 매핑을 통한 제품명 자동 설정: {} -> {}",
              request.getProductCode(), request.getProductName());
        }
      } else {
        log.warn("제품 코드 매핑 정보를 찾을 수 없습니다: {}", request.getProductCode());
      }
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
        .totalEmission(request.getTotalEmission()) // 프론트엔드 계산값 그대로 저장
        .isManualInput(request.getIsManualInput() != null ? request.getIsManualInput() : false);

    // Scope 타입별 카테고리 설정
    switch (request.getScopeType()) {
      case SCOPE1:
        if (request.getCategoryNumber() != null) {
          builder.scope1CategoryNumber(request.getCategoryNumber())
              .scope1CategoryName(request.getCategoryName());
        }
        break;
      case SCOPE2:
        if (request.getCategoryNumber() != null) {
          builder.scope2CategoryNumber(request.getCategoryNumber())
              .scope2CategoryName(request.getCategoryName());
        }
        break;
      case SCOPE3:
        if (request.getCategoryNumber() != null) {
          builder.scope3CategoryNumber(request.getCategoryNumber())
              .scope3CategoryName(request.getCategoryName());
        }
        break;
    }

    // 제품 코드 설정 (Scope 1, 2에서 선택적)
    if (request.getScopeType() == ScopeType.SCOPE1 || request.getScopeType() == ScopeType.SCOPE2) {
      builder.companyProductCode(request.getProductCode())
          .productName(request.getProductName());
    }

    return builder.build();
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

    // 비즈니스 데이터 필드 업데이트
    if (request.getMajorCategory() != null) {
      builder.majorCategory(request.getMajorCategory());
    }
    if (request.getSubcategory() != null) {
      builder.subcategory(request.getSubcategory());
    }
    if (request.getRawMaterial() != null) {
      builder.rawMaterial(request.getRawMaterial());
    }
    if (request.getCompanyProductCode() != null) {
      builder.companyProductCode(request.getCompanyProductCode());
    }
    if (request.getProductName() != null) {
      builder.productName(request.getProductName());
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

    // 메타데이터 필드 업데이트
    if (request.getReportingYear() != null) {
      builder.reportingYear(request.getReportingYear());
    }
    if (request.getReportingMonth() != null) {
      builder.reportingMonth(request.getReportingMonth());
    }

    // 총 배출량 업데이트 (프론트엔드 계산값 그대로 저장)
    if (request.getTotalEmission() != null) {
      builder.totalEmission(request.getTotalEmission());
    }

    return builder.build();
  }

  // ============================================================================
  // 헬퍼 메서드
  // ============================================================================

  /**
   * 카테고리 번호로 배출량 데이터 조회
   */
  private List<ScopeEmission> findByCategoryNumber(
      Long headquartersId, Long partnerId, String treePath,
      Integer year, Integer month, ScopeType scopeType, Integer categoryNumber,
      boolean isHeadquarters) {

    switch (scopeType) {
      case SCOPE1:
        if (isHeadquarters) {
          return scopeEmissionRepository.findByHeadquartersIdAndScope1CategoryNumber(
              headquartersId, categoryNumber);
        } else {
          return scopeEmissionRepository.findByPartnerIdAndTreePathStartingWithAndScope1CategoryNumber(
              partnerId, treePath, categoryNumber);
        }
      case SCOPE2:
        if (isHeadquarters) {
          return scopeEmissionRepository.findByHeadquartersIdAndScope2CategoryNumber(
              headquartersId, categoryNumber);
        } else {
          return scopeEmissionRepository.findByPartnerIdAndTreePathStartingWithAndScope2CategoryNumber(
              partnerId, treePath, categoryNumber);
        }
      case SCOPE3:
        if (isHeadquarters) {
          return scopeEmissionRepository.findByHeadquartersIdAndScope3CategoryNumber(
              headquartersId, categoryNumber);
        } else {
          return scopeEmissionRepository.findByPartnerIdAndTreePathStartingWithAndScope3CategoryNumber(
              partnerId, treePath, categoryNumber);
        }
      default:
        return List.of();
    }
  }

  /**
   * 카테고리별 총계 결과 조회
   */
  private List<Object[]> getCategorySummaryResults(
      Long headquartersId, Long partnerId, String treePath,
      ScopeType scopeType, Integer year, Integer month,
      boolean isHeadquarters) {

    switch (scopeType) {
      case SCOPE1:
      case SCOPE2:
        // Scope 1, 2는 카테고리별 집계 기능이 제한적이므로 빈 리스트 반환
        return List.of();
      case SCOPE3:
        if (isHeadquarters) {
          return scopeEmissionRepository.sumTotalEmissionByScope3CategoryAndYearForHeadquarters(
              headquartersId, year);
        } else {
          return scopeEmissionRepository.sumTotalEmissionByScope3CategoryAndYearForPartner(
              partnerId, treePath, year);
        }
      default:
        return List.of();
    }
  }
}