package com.nsmm.esg.scope_service.service;

import com.nsmm.esg.scope_service.dto.request.Scope3EmissionRequest;
import com.nsmm.esg.scope_service.dto.response.Scope3EmissionResponse;
import com.nsmm.esg.scope_service.dto.request.Scope3EmissionUpdateRequest;
import com.nsmm.esg.scope_service.entity.Scope3Category;
import com.nsmm.esg.scope_service.entity.Scope3Emission;
import com.nsmm.esg.scope_service.repository.Scope3EmissionRepository;
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
 * Scope 3 배출량 서비스
 * 
 * 비즈니스 로직 처리:
 * - 권한 검증 및 데이터 격리
 * - 배출량 계산 로직
 * - 중복 데이터 검증
 * - 트랜잭션 관리
 * 
 * @author ESG Project Team
 * @version 1.0
 * @since 2024
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class Scope3EmissionService {

  private final Scope3EmissionRepository scope3EmissionRepository;

  // ========================================================================
  // 생성 메서드 (Creation Methods)
  // ========================================================================

  /**
   * Scope 3 배출량 데이터 생성
   * 
   * @param request        생성 요청 데이터
   * @param userType       사용자 타입 (HEADQUARTERS | PARTNER)
   * @param headquartersId 본사 ID (본사인 경우)
   * @param partnerId      협력사 ID (협력사인 경우)
   * @param treePath       계층 경로
   * @return 생성된 배출량 데이터
   */
  @Transactional
  public Scope3EmissionResponse createScope3Emission(
      Scope3EmissionRequest request,
      String userType,
      String headquartersId,
      String partnerId,
      String treePath) {

    log.info("Scope 3 배출량 데이터 생성 시작: userType={}, categoryNumber={}", userType, request.getCategoryNumber());
    log.debug("수신된 요청 데이터: {}", request);
    log.debug("헤더 정보: userType={}, headquartersId={}, partnerId={}, treePath={}",
        userType, headquartersId, partnerId, treePath);

    // 0. 요청 데이터 상세 검증 로그
    log.debug("요청 데이터 상세 검증:");
    log.debug("  - majorCategory: '{}' (length: {})", request.getMajorCategory(),
        request.getMajorCategory() != null ? request.getMajorCategory().length() : "null");
    log.debug("  - subcategory: '{}' (length: {})", request.getSubcategory(),
        request.getSubcategory() != null ? request.getSubcategory().length() : "null");
    log.debug("  - rawMaterial: '{}' (length: {})", request.getRawMaterial(),
        request.getRawMaterial() != null ? request.getRawMaterial().length() : "null");
    log.debug("  - unit: '{}' (length: {})", request.getUnit(),
        request.getUnit() != null ? request.getUnit().length() : "null");
    log.debug("  - activityAmount: {}", request.getActivityAmount());
    log.debug("  - emissionFactor: {}", request.getEmissionFactor());
    log.debug("  - totalEmission: {}", request.getTotalEmission());
    log.debug("  - reportingYear: {}", request.getReportingYear());
    log.debug("  - reportingMonth: {}", request.getReportingMonth());
    log.debug("  - categoryNumber: {}", request.getCategoryNumber());

    try {
      // 1. 권한 검증
      if (userType == null) {
        log.error("권한 검증 실패: 사용자 유형이 null");
        throw new IllegalArgumentException("사용자 유형이 필요합니다");
      }

      // 2. 본사/협력사 구분에 따른 처리
      Long finalHeadquartersId;
      Long finalPartnerId = null;

      if ("HEADQUARTERS".equals(userType)) {
        if (headquartersId == null) {
          log.error("본사 권한 검증 실패: headquartersId가 null");
          throw new IllegalArgumentException("본사 ID가 필요합니다");
        }
        try {
          finalHeadquartersId = Long.parseLong(headquartersId);
          log.debug("본사 사용자 인증 완료: headquartersId={}", finalHeadquartersId);
        } catch (NumberFormatException e) {
          log.error("본사 ID 파싱 오류: headquartersId='{}'", headquartersId, e);
          throw new IllegalArgumentException("올바르지 않은 본사 ID 형식입니다: " + headquartersId);
        }
      } else if ("PARTNER".equals(userType)) {
        if (partnerId == null) {
          log.error("협력사 권한 검증 실패: partnerId가 null");
          throw new IllegalArgumentException("협력사 ID가 필요합니다");
        }
        if (treePath == null) {
          log.error("협력사 권한 검증 실패: treePath가 null");
          throw new IllegalArgumentException("협력사는 TreePath가 필요합니다");
        }
        try {
          finalHeadquartersId = Long.parseLong(headquartersId);
          finalPartnerId = Long.parseLong(partnerId);
          log.debug("협력사 사용자 인증 완료: headquartersId={}, partnerId={}, treePath='{}'",
              finalHeadquartersId, finalPartnerId, treePath);
        } catch (NumberFormatException e) {
          log.error("협력사 ID 파싱 오류: headquartersId='{}', partnerId='{}'", headquartersId, partnerId, e);
          throw new IllegalArgumentException("올바르지 않은 ID 형식입니다");
        }
      } else {
        log.error("알 수 없는 사용자 유형: userType='{}'", userType);
        throw new IllegalArgumentException("알 수 없는 사용자 유형입니다: " + userType);
      }

      // 3. 중복 데이터 검증
      log.debug("중복 데이터 검증 시작");
      validateDuplicateData(request, userType, finalHeadquartersId, finalPartnerId);
      log.debug("중복 데이터 검증 완료");

      // 4. 카테고리 정보 조회
      log.debug("카테고리 정보 조회 시작: categoryNumber={}", request.getCategoryNumber());
      Scope3Category category = Scope3Category.fromCategoryNumber(request.getCategoryNumber());
      log.debug("카테고리 정보 조회 완료: categoryName='{}'", category.getCategoryName());

      // 5. 엔티티 생성
      log.debug("엔티티 생성 시작");
      Scope3Emission emission = Scope3Emission.builder()
          .headquartersId(finalHeadquartersId)
          .partnerId(finalPartnerId)
          .treePath(treePath)
          .reportingYear(request.getReportingYear())
          .reportingMonth(request.getReportingMonth())
          .categoryNumber(category.getCategoryNumber())
          .categoryName(category.getCategoryName())
          .majorCategory(request.getMajorCategory())
          .subcategory(request.getSubcategory())
          .rawMaterial(request.getRawMaterial())
          .activityAmount(request.getActivityAmount())
          .unit(request.getUnit())
          .emissionFactor(request.getEmissionFactor())
          .totalEmission(request.getTotalEmission())
          .isManualInput(request.getIsManualInput() != null ? request.getIsManualInput() : false) // 수동 입력 여부 설정
          .build();
      log.debug("엔티티 생성 완료");

      // 6. 저장 및 응답
      log.debug("데이터베이스 저장 시작");
      Scope3Emission savedEmission = scope3EmissionRepository.save(emission);
      log.info("Scope 3 배출량 데이터 생성 완료: id={}, totalEmission={}",
          savedEmission.getId(), savedEmission.getTotalEmission());

      return Scope3EmissionResponse.from(savedEmission);

    } catch (Exception e) {
      log.error("Scope 3 배출량 데이터 생성 중 오류 발생", e);
      log.error("오류 발생 시점의 요청 데이터: {}", request);
      log.error("오류 발생 시점의 헤더 정보: userType={}, headquartersId={}, partnerId={}, treePath={}",
          userType, headquartersId, partnerId, treePath);
      throw e; // 원본 예외를 다시 던짐
    }
  }

  // ========================================================================
  // 조회 메서드 (Query Methods)
  // ========================================================================

  /**
   * TreePath 기반 권한으로 배출량 데이터 조회
   * 
   * @param treePath 계층 경로
   * @param pageable 페이지 정보
   * @return 배출량 데이터 목록
   */
  public Page<Scope3EmissionResponse> getScope3EmissionsByTreePath(String treePath, Pageable pageable) {
    log.info("TreePath 기반 배출량 데이터 조회: treePath={}", treePath);

    Page<Scope3Emission> emissions = scope3EmissionRepository.findByTreePathStartingWith(treePath, pageable);

    return emissions.map(Scope3EmissionResponse::from);
  }

  /**
   * 회사별 배출량 데이터 조회
   */
  public List<Scope3EmissionResponse> getScope3EmissionsByCompany(
      String userType,
      String headquartersId,
      String partnerId,
      String treePath) {

    log.info("회사별 배출량 데이터 조회: userType={}", userType);

    // 1. 권한 검증
    if (userType == null) {
      throw new IllegalArgumentException("사용자 유형이 필요합니다");
    }

    List<Scope3Emission> emissions;

    // 2. 본사/협력사 구분에 따른 처리
    if ("HEADQUARTERS".equals(userType)) {
      if (headquartersId == null) {
        throw new IllegalArgumentException("본사 ID가 필요합니다");
      }
      // 본사: 모든 하위 조직 데이터 조회 가능
      emissions = scope3EmissionRepository.findByHeadquartersId(Long.parseLong(headquartersId));
    } else if ("PARTNER".equals(userType)) {
      if (partnerId == null) {
        throw new IllegalArgumentException("협력사 ID가 필요합니다");
      }
      if (treePath == null) {
        throw new IllegalArgumentException("협력사는 TreePath가 필요합니다");
      }
      // 협력사: 본인 및 하위 조직 데이터만 조회 가능
      emissions = scope3EmissionRepository.findByPartnerIdAndTreePathStartingWith(
          Long.parseLong(partnerId), treePath);
    } else {
      throw new IllegalArgumentException("알 수 없는 사용자 유형입니다: " + userType);
    }

    return emissions.stream()
        .map(Scope3EmissionResponse::from)
        .collect(Collectors.toList());
  }

  /**
   * 연도별 배출량 데이터 조회
   */
  public List<Scope3EmissionResponse> getScope3EmissionsByYear(
      Integer year,
      String userType,
      String headquartersId,
      String partnerId,
      String treePath) {

    log.info("연도별 배출량 데이터 조회: year={}, userType={}", year, userType);

    // 1. 권한 검증
    if (userType == null) {
      throw new IllegalArgumentException("사용자 유형이 필요합니다");
    }

    List<Scope3Emission> emissions;

    // 2. 본사/협력사 구분에 따른 처리
    if ("HEADQUARTERS".equals(userType)) {
      if (headquartersId == null) {
        throw new IllegalArgumentException("본사 ID가 필요합니다");
      }
      // 본사: 모든 하위 조직 데이터 조회 가능
      emissions = scope3EmissionRepository.findByHeadquartersIdAndReportingYear(
          Long.parseLong(headquartersId), year);
    } else if ("PARTNER".equals(userType)) {
      if (partnerId == null) {
        throw new IllegalArgumentException("협력사 ID가 필요합니다");
      }
      if (treePath == null) {
        throw new IllegalArgumentException("협력사는 TreePath가 필요합니다");
      }
      // 협력사: 본인 및 하위 조직 데이터만 조회 가능
      emissions = scope3EmissionRepository.findByPartnerIdAndTreePathStartingWithAndReportingYear(
          Long.parseLong(partnerId), treePath, year);
    } else {
      throw new IllegalArgumentException("알 수 없는 사용자 유형입니다: " + userType);
    }

    return emissions.stream()
        .map(Scope3EmissionResponse::from)
        .collect(Collectors.toList());
  }

  /**
   * 카테고리별 배출량 데이터 조회
   */
  public List<Scope3EmissionResponse> getScope3EmissionsByCategory(
      Integer categoryNumber,
      String userType,
      String headquartersId,
      String partnerId,
      String treePath) {

    log.info("카테고리별 배출량 데이터 조회: categoryNumber={}, userType={}", categoryNumber, userType);

    // 1. 권한 검증
    if (userType == null) {
      throw new IllegalArgumentException("사용자 유형이 필요합니다");
    }

    List<Scope3Emission> emissions;

    // 2. 본사/협력사 구분에 따른 처리
    if ("HEADQUARTERS".equals(userType)) {
      if (headquartersId == null) {
        throw new IllegalArgumentException("본사 ID가 필요합니다");
      }
      // 본사: 모든 하위 조직 데이터 조회 가능
      emissions = scope3EmissionRepository.findByHeadquartersIdAndCategoryNumber(
          Long.parseLong(headquartersId), categoryNumber);
    } else if ("PARTNER".equals(userType)) {
      if (partnerId == null) {
        throw new IllegalArgumentException("협력사 ID가 필요합니다");
      }
      if (treePath == null) {
        throw new IllegalArgumentException("협력사는 TreePath가 필요합니다");
      }
      // 협력사: 본인 및 하위 조직 데이터만 조회 가능
      emissions = scope3EmissionRepository.findByPartnerIdAndTreePathStartingWithAndCategoryNumber(
          Long.parseLong(partnerId), treePath, categoryNumber);
    } else {
      throw new IllegalArgumentException("알 수 없는 사용자 유형입니다: " + userType);
    }

    return emissions.stream()
        .map(Scope3EmissionResponse::from)
        .collect(Collectors.toList());
  }

  /**
   * 연도/월별 배출량 데이터 조회
   */
  public List<Scope3EmissionResponse> getScope3EmissionsByYearAndMonth(
      Integer year,
      Integer month,
      String userType,
      String headquartersId,
      String partnerId,
      String treePath) {

    log.info("연도/월별 배출량 데이터 조회: year={}, month={}, userType={}", year, month, userType);

    // 1. 권한 검증
    if (userType == null) {
      throw new IllegalArgumentException("사용자 유형이 필요합니다");
    }

    List<Scope3Emission> emissions;

    // 2. 본사/협력사 구분에 따른 처리
    if ("HEADQUARTERS".equals(userType)) {
      if (headquartersId == null) {
        throw new IllegalArgumentException("본사 ID가 필요합니다");
      }
      // 본사: 모든 하위 조직 데이터 조회 가능
      emissions = scope3EmissionRepository.findByHeadquartersIdAndReportingYearAndReportingMonth(
          Long.parseLong(headquartersId), year, month);
    } else if ("PARTNER".equals(userType)) {
      if (partnerId == null) {
        throw new IllegalArgumentException("협력사 ID가 필요합니다");
      }
      if (treePath == null) {
        throw new IllegalArgumentException("협력사는 TreePath가 필요합니다");
      }
      // 협력사: 본인 및 하위 조직 데이터만 조회 가능
      emissions = scope3EmissionRepository.findByPartnerIdAndTreePathStartingWithAndReportingYearAndReportingMonth(
          Long.parseLong(partnerId), treePath, year, month);
    } else {
      throw new IllegalArgumentException("알 수 없는 사용자 유형입니다: " + userType);
    }

    return emissions.stream()
        .map(Scope3EmissionResponse::from)
        .collect(Collectors.toList());
  }

  /**
   * 연도/월/카테고리별 배출량 데이터 조회
   */
  public List<Scope3EmissionResponse> getScope3EmissionsByYearAndMonthAndCategory(
      Integer year,
      Integer month,
      Integer categoryNumber,
      String userType,
      String headquartersId,
      String partnerId,
      String treePath) {

    log.info("연도/월/카테고리별 배출량 데이터 조회: year={}, month={}, category={}, userType={}",
        year, month, categoryNumber, userType);

    // 1. 권한 검증
    if (userType == null) {
      throw new IllegalArgumentException("사용자 유형이 필요합니다");
    }

    List<Scope3Emission> emissions;

    // 2. 본사/협력사 구분에 따른 처리
    if ("HEADQUARTERS".equals(userType)) {
      if (headquartersId == null) {
        throw new IllegalArgumentException("본사 ID가 필요합니다");
      }
      // 본사: 모든 하위 조직 데이터 조회 가능
      emissions = scope3EmissionRepository.findByHeadquartersIdAndReportingYearAndReportingMonthAndCategoryNumber(
          Long.parseLong(headquartersId), year, month, categoryNumber);
    } else if ("PARTNER".equals(userType)) {
      if (partnerId == null) {
        throw new IllegalArgumentException("협력사 ID가 필요합니다");
      }
      if (treePath == null) {
        throw new IllegalArgumentException("협력사는 TreePath가 필요합니다");
      }
      // 협력사: 본인 및 하위 조직 데이터만 조회 가능
      emissions = scope3EmissionRepository
          .findByPartnerIdAndTreePathStartingWithAndReportingYearAndReportingMonthAndCategoryNumber(
              Long.parseLong(partnerId), treePath, year, month, categoryNumber);
    } else {
      throw new IllegalArgumentException("알 수 없는 사용자 유형입니다: " + userType);
    }

    return emissions.stream()
        .map(Scope3EmissionResponse::from)
        .collect(Collectors.toList());
  }

  /**
   * 연도/월별 카테고리 총계 조회
   */
  public Map<Integer, BigDecimal> getCategorySummaryByYearAndMonth(
      Integer year,
      Integer month,
      String userType,
      String headquartersId,
      String partnerId,
      String treePath) {

    log.info("카테고리 총계 조회: year={}, month={}, userType={}", year, month, userType);

    // 1. 권한 검증
    if (userType == null) {
      throw new IllegalArgumentException("사용자 유형이 필요합니다");
    }

    List<Object[]> results;

    // 2. 본사/협력사 구분에 따른 처리
    if ("HEADQUARTERS".equals(userType)) {
      if (headquartersId == null) {
        throw new IllegalArgumentException("본사 ID가 필요합니다");
      }
      // 본사: 모든 하위 조직 데이터 조회 가능
      if (month != null) {
        results = scope3EmissionRepository.sumTotalEmissionByCategoryAndYearAndMonthForHeadquarters(
            Long.parseLong(headquartersId), year, month);
      } else {
        results = scope3EmissionRepository.sumTotalEmissionByCategoryAndYearForHeadquarters(
            Long.parseLong(headquartersId), year);
      }
    } else if ("PARTNER".equals(userType)) {
      if (partnerId == null) {
        throw new IllegalArgumentException("협력사 ID가 필요합니다");
      }
      if (treePath == null) {
        throw new IllegalArgumentException("협력사는 TreePath가 필요합니다");
      }
      // 협력사: 본인 및 하위 조직 데이터만 조회 가능
      if (month != null) {
        results = scope3EmissionRepository.sumTotalEmissionByCategoryAndYearAndMonthForPartner(
            Long.parseLong(partnerId), treePath, year, month);
      } else {
        results = scope3EmissionRepository.sumTotalEmissionByCategoryAndYearForPartner(
            Long.parseLong(partnerId), treePath, year);
      }
    } else {
      throw new IllegalArgumentException("알 수 없는 사용자 유형입니다: " + userType);
    }

    return results.stream()
        .collect(Collectors.toMap(
            result -> (Integer) result[0], // categoryNumber
            result -> (BigDecimal) result[1] // totalEmission
        ));
  }

  /**
   * 연도별 카테고리 총계 조회 (월 구분 없음)
   */
  public Map<Integer, BigDecimal> getCategorySummaryByYear(
      Integer year,
      String userType,
      String headquartersId,
      String partnerId,
      String treePath) {

    return getCategorySummaryByYearAndMonth(year, null, userType, headquartersId, partnerId, treePath);
  }

  /**
   * 특정 배출량 데이터 조회 (권한 검증 포함)
   * 
   * @param id       배출량 데이터 ID
   * @param userId   사용자 UUID
   * @param userType 사용자 타입 (HEADQUARTERS | PARTNER)
   * @param treePath 사용자 TreePath
   * @return 배출량 데이터
   */
  public Scope3EmissionResponse getScope3EmissionById(Long id, String userId, String userType, String treePath) {
    log.info("배출량 데이터 조회: id={}, userId={}, userType={}, treePath={}", id, userId, userType, treePath);

    Scope3Emission emission = scope3EmissionRepository.findById(id)
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

    return Scope3EmissionResponse.from(emission);
  }

  // ========================================================================
  // 업데이트 메서드 (Update Methods)
  // ========================================================================

  /**
   * Scope 3 배출량 데이터 수정
   */
  @Transactional
  public Scope3EmissionResponse updateScope3Emission(
      Long id,
      Scope3EmissionUpdateRequest request,
      String userType,
      String headquartersId,
      String partnerId,
      String treePath) {

    log.info("Scope 3 배출량 데이터 업데이트 시작: id={}, userType={}", id, userType);

    // 1. 업데이트할 필드가 있는지 확인
    if (!request.hasAnyField()) {
      throw new IllegalArgumentException("업데이트할 필드가 없습니다");
    }

    // 2. 기존 데이터 조회
    Scope3Emission existingEmission = scope3EmissionRepository.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("배출량 데이터를 찾을 수 없습니다: " + id));

    // 3. 권한 검증
    if (userType == null) {
      throw new IllegalArgumentException("사용자 유형이 필요합니다");
    }

    if ("HEADQUARTERS".equals(userType)) {
      if (headquartersId == null) {
        throw new IllegalArgumentException("본사 ID가 필요합니다");
      }
      // 본사: 자신의 데이터만 수정 가능
      if (Long.parseLong(headquartersId) != existingEmission.getHeadquartersId()) {
        throw new IllegalArgumentException("해당 데이터를 수정할 권한이 없습니다");
      }
    } else if ("PARTNER".equals(userType)) {
      if (partnerId == null) {
        throw new IllegalArgumentException("협력사 ID가 필요합니다");
      }
      if (treePath == null) {
        throw new IllegalArgumentException("협력사는 TreePath가 필요합니다");
      }
      // 협력사: 본인 데이터만 수정 가능 (하위 조직 데이터는 수정 불가)
      if (Long.parseLong(partnerId) != existingEmission.getPartnerId() ||
          !existingEmission.getTreePath().equals(treePath)) {
        throw new IllegalArgumentException("해당 데이터를 수정할 권한이 없습니다");
      }
    } else {
      throw new IllegalArgumentException("알 수 없는 사용자 유형입니다: " + userType);
    }

    // 4. 중복 데이터 검증 (수정용 - 자기 자신 제외)
    if (request.hasKeyFields()) {
      // 중복 검증이 필요한 핵심 필드들이 변경된 경우
      log.debug("중복 데이터 검증 시작 (수정용)");
      Scope3EmissionRequest tempRequest = createValidationRequest(request, existingEmission);

      Long finalHeadquartersId = existingEmission.getHeadquartersId();
      Long finalPartnerId = existingEmission.getPartnerId();

      validateDuplicateData(tempRequest, userType, finalHeadquartersId, finalPartnerId, id);
      log.debug("중복 데이터 검증 완료 (수정용)");
    }

    // 5. 부분 업데이트 로직 - null이 아닌 필드만 업데이트
    Scope3Emission.Scope3EmissionBuilder builder = existingEmission.toBuilder();

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
    if (request.getCategoryNumber() != null) {
      builder.categoryNumber(request.getCategoryNumber());
    }
    if (request.getCategoryName() != null) {
      builder.categoryName(request.getCategoryName());
    }

    // 6. 총 배출량 처리
    if (request.getTotalEmission() != null) {
      // 프론트엔드에서 계산된 값이 제공된 경우 그대로 사용
      builder.totalEmission(request.getTotalEmission());
      log.info("프론트엔드 계산 배출량 사용: {}", request.getTotalEmission());
    } else if (request.needsEmissionCalculation()) {
      // totalEmission이 없고 activityAmount나 emissionFactor가 변경된 경우 자동 계산
      BigDecimal finalActivityAmount = request.getActivityAmount() != null ? request.getActivityAmount()
          : existingEmission.getActivityAmount();
      BigDecimal finalEmissionFactor = request.getEmissionFactor() != null ? request.getEmissionFactor()
          : existingEmission.getEmissionFactor();

      BigDecimal calculatedTotalEmission = finalActivityAmount.multiply(finalEmissionFactor);
      builder.totalEmission(calculatedTotalEmission);
      log.info("자동 계산 배출량: {} * {} = {}", finalActivityAmount, finalEmissionFactor, calculatedTotalEmission);
    }

    // 7. 저장 및 응답
    Scope3Emission updatedEmission = scope3EmissionRepository.save(builder.build());
    log.info("Scope 3 배출량 데이터 업데이트 완료: id={}, totalEmission={}",
        updatedEmission.getId(), updatedEmission.getTotalEmission());

    return Scope3EmissionResponse.from(updatedEmission);
  }

  // ========================================================================
  // 삭제 메서드 (Delete Methods)
  // ========================================================================

  /**
   * Scope 3 배출량 데이터 삭제
   */
  @Transactional
  public void deleteScope3Emission(
      Long id,
      String userType,
      String headquartersId,
      String partnerId,
      String treePath) {

    log.info("Scope 3 배출량 데이터 삭제 시작: id={}, userType={}", id, userType);

    // 1. 기존 데이터 조회
    Scope3Emission emission = scope3EmissionRepository.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("배출량 데이터를 찾을 수 없습니다: " + id));

    // 2. 권한 검증
    if (userType == null) {
      throw new IllegalArgumentException("사용자 유형이 필요합니다");
    }

    if ("HEADQUARTERS".equals(userType)) {
      if (headquartersId == null) {
        throw new IllegalArgumentException("본사 ID가 필요합니다");
      }
      // 본사: 자신의 데이터만 삭제 가능
      if (Long.parseLong(headquartersId) != emission.getHeadquartersId()) {
        throw new IllegalArgumentException("해당 데이터를 삭제할 권한이 없습니다");
      }
    } else if ("PARTNER".equals(userType)) {
      if (partnerId == null) {
        throw new IllegalArgumentException("협력사 ID가 필요합니다");
      }
      if (treePath == null) {
        throw new IllegalArgumentException("협력사는 TreePath가 필요합니다");
      }
      // 협력사: 본인 데이터만 삭제 가능 (하위 조직 데이터는 삭제 불가)
      if (Long.parseLong(partnerId) != emission.getPartnerId() ||
          !emission.getTreePath().equals(treePath)) {
        throw new IllegalArgumentException("해당 데이터를 삭제할 권한이 없습니다");
      }
    } else {
      throw new IllegalArgumentException("알 수 없는 사용자 유형입니다: " + userType);
    }

    // 3. 삭제
    scope3EmissionRepository.delete(emission);
    log.info("Scope 3 배출량 데이터 삭제 완료: id={}", id);
  }

  // ========================================================================
  // 집계 메서드 (Aggregation Methods)
  // ========================================================================

  /**
   * 회사별 총 배출량 집계
   * 
   * @param userId 사용자 UUID
   * @param year   보고 연도
   * @return 총 배출량
   */
  public BigDecimal getTotalEmissionByCompanyAndYear(String userId, Integer year) {
    log.info("회사별 총 배출량 집계: userId={}, year={}", userId, year);

    // TODO: 본사/협력사 구분에 따른 집계 로직 구현 필요
    return BigDecimal.ZERO;
  }

  /**
   * TreePath 기반 총 배출량 집계
   * 
   * @param treePath 계층 경로
   * @param year     보고 연도
   * @return 총 배출량
   */
  public BigDecimal getTotalEmissionByTreePathAndYear(String treePath, Integer year) {
    log.info("TreePath 기반 총 배출량 집계: treePath={}, year={}", treePath, year);

    // TODO: 본사/협력사 구분에 따른 집계 로직 구현 필요
    return BigDecimal.ZERO;
  }

  // ========================================================================
  // 비즈니스 로직 메서드 (Business Logic Methods)
  // ========================================================================

  /**
   * 중복 데이터 검증 (본사/협력사 구분) - 신규 생성용
   */
  private void validateDuplicateData(
      Scope3EmissionRequest request,
      String userType,
      Long headquartersId,
      Long partnerId) {
    validateDuplicateData(request, userType, headquartersId, partnerId, null);
  }

  /**
   * 중복 데이터 검증 (본사/협력사 구분) - 수정용 (자기 자신 제외)
   */
  private void validateDuplicateData(
      Scope3EmissionRequest request,
      String userType,
      Long headquartersId,
      Long partnerId,
      Long excludeId) {

    List<Scope3Emission> existingData;

    if ("HEADQUARTERS".equals(userType)) {
      existingData = scope3EmissionRepository
          .findByHeadquartersIdAndReportingYearAndReportingMonthAndCategoryNumberAndMajorCategoryAndSubcategoryAndRawMaterial(
              headquartersId,
              request.getReportingYear(),
              request.getReportingMonth(),
              request.getCategoryNumber(),
              request.getMajorCategory(),
              request.getSubcategory(),
              request.getRawMaterial());
    } else {
      existingData = scope3EmissionRepository
          .findByPartnerIdAndReportingYearAndReportingMonthAndCategoryNumberAndMajorCategoryAndSubcategoryAndRawMaterial(
              partnerId,
              request.getReportingYear(),
              request.getReportingMonth(),
              request.getCategoryNumber(),
              request.getMajorCategory(),
              request.getSubcategory(),
              request.getRawMaterial());
    }

    // 수정 시에는 자기 자신을 제외하고 중복 검증
    if (excludeId != null) {
      existingData = existingData.stream()
          .filter(emission -> !emission.getId().equals(excludeId))
          .collect(Collectors.toList());
    }

    if (!existingData.isEmpty()) {
      throw new IllegalArgumentException("동일한 조건의 배출량 데이터가 이미 존재합니다");
    }
  }

  // ========================================================================
  // 유틸리티 메서드 (Utility Methods)
  // ========================================================================

  /**
   * 수정 요청을 중복 검증용 요청으로 변환
   * 기존 데이터 + 변경된 필드를 합쳐서 검증용 요청 생성
   */
  private Scope3EmissionRequest createValidationRequest(
      Scope3EmissionUpdateRequest updateRequest,
      Scope3Emission existingEmission) {

    return Scope3EmissionRequest.builder()
        .reportingYear(updateRequest.getReportingYear() != null ? updateRequest.getReportingYear()
            : existingEmission.getReportingYear())
        .reportingMonth(updateRequest.getReportingMonth() != null ? updateRequest.getReportingMonth()
            : existingEmission.getReportingMonth())
        .categoryNumber(updateRequest.getCategoryNumber() != null ? updateRequest.getCategoryNumber()
            : existingEmission.getCategoryNumber())
        .majorCategory(updateRequest.getMajorCategory() != null ? updateRequest.getMajorCategory()
            : existingEmission.getMajorCategory())
        .subcategory(
            updateRequest.getSubcategory() != null ? updateRequest.getSubcategory() : existingEmission.getSubcategory())
        .rawMaterial(
            updateRequest.getRawMaterial() != null ? updateRequest.getRawMaterial() : existingEmission.getRawMaterial())
        .build();
  }


}