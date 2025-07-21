package com.nsmm.esg.scope_service.service;

import com.nsmm.esg.scope_service.client.AuthServiceClient;
import com.nsmm.esg.scope_service.dto.ApiResponse;
import com.nsmm.esg.scope_service.dto.request.MaterialAssignmentBatchRequest;
import com.nsmm.esg.scope_service.dto.request.MaterialAssignmentRequest;
import com.nsmm.esg.scope_service.dto.response.MaterialAssignmentResponse;
import com.nsmm.esg.scope_service.entity.MaterialAssignment;
import com.nsmm.esg.scope_service.repository.MaterialAssignmentRepository;
import com.nsmm.esg.scope_service.util.UuidUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 자재코드 할당 서비스
 * 
 * MaterialAssignment 엔티티를 관리하는 핵심 비즈니스 로직을 제공합니다.
 * 본사와 협력사 간의 자재코드 할당, 조회, 수정, 삭제 기능을 담당합니다.
 * 
 * 주요 기능:
 * - 협력사별/본사별 자재코드 할당 조회
 * - 자재코드 할당 생성 (단건/일괄)
 * - 자재코드 할당 수정/삭제
 * - 사용자 타입별 자재 데이터 조회 (본사: 더미데이터, 협력사: 할당데이터)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MaterialAssignmentService {

    private final MaterialAssignmentRepository materialAssignmentRepository;
    private final MaterialDataService materialDataService;
    private final AuthServiceClient authServiceClient;

    /**
     * 특정 협력사에게 할당된 자재코드 목록을 조회합니다.
     * 
     * @param partnerId 협력사 ID (UUID 또는 비즈니스 ID 형식 모두 지원)
     * @return 해당 협력사에게 할당된 활성 상태의 자재코드 목록
     * @throws IllegalArgumentException UUID 변환 실패 시
     */
    @Transactional(readOnly = true)
    public List<MaterialAssignmentResponse> getAssignmentsByPartner(String partnerId) {
        log.info("협력사 {}의 자재코드 할당 목록 조회 시작", partnerId);
        
        // UUID → partnerId 변환 (저장 시와 동일한 로직 적용)
        String businessId = convertToBusinessId(partnerId);
        log.info("조회용 비즈니스 ID 변환: {} → {}", partnerId, businessId);
        
        List<MaterialAssignment> assignments = materialAssignmentRepository.findActiveByToPartnerId(businessId);
        return assignments.stream().map(this::convertToResponse).collect(Collectors.toList());
    }

    /**
     * 본사의 모든 자재코드 할당 목록을 조회합니다.
     * 
     * @param headquartersId 본사 ID
     * @param userType 사용자 타입 (HEADQUARTERS만 허용)
     * @return 해당 본사의 모든 자재코드 할당 목록
     * @throws IllegalArgumentException 본사 계정이 아닌 경우
     */
    @Transactional(readOnly = true)
    public List<MaterialAssignmentResponse> getAssignmentsByHeadquarters(Long headquartersId, String userType) {
        if (!"HEADQUARTERS".equals(userType)) {
            throw new IllegalArgumentException("본사 전체 할당 목록은 본사 계정만 조회할 수 있습니다");
        }
        List<MaterialAssignment> assignments = materialAssignmentRepository.findByHeadquartersId(headquartersId);
        return assignments.stream().map(this::convertToResponse).collect(Collectors.toList());
    }

    /**
     * 로그인 사용자의 자재 데이터를 조회합니다.
     * 본사 사용자: 더미 자재 데이터 반환
     * 협력사 사용자: 할당받은 자재 데이터 반환
     * 
     * @param userType 사용자 타입 (HEADQUARTERS 또는 PARTNER)
     * @param headquartersId 본사 ID
     * @param partnerId 협력사 ID (협력사 사용자인 경우 필수)
     * @return 사용자별 맞춤 자재 데이터 목록
     */
    @Transactional(readOnly = true)
    public List<MaterialAssignmentResponse> getMyMaterialData(String userType, String headquartersId, String partnerId) {
        log.info("사용자 자재 데이터 조회 시작 - 사용자타입: {}, 본사ID: {}, 협력사ID: {}", userType, headquartersId, partnerId);
        if ("HEADQUARTERS".equals(userType)) {
            // 본사 사용자: MaterialDataService에서 더미 데이터 반환
            return materialDataService.getHeadquartersDummyData();
        } else {
            // 협력사 사용자: 할당받은 자재 데이터 반환
            return getPartnerMaterialData(partnerId);
        }
    }

    /**
     * 협력사에게 할당된 자재 데이터를 조회합니다.
     * 
     * @param partnerId 협력사 ID
     * @return 협력사에게 할당된 자재 데이터 목록
     */
    private List<MaterialAssignmentResponse> getPartnerMaterialData(String partnerId) {
        List<MaterialAssignment> assignments = materialAssignmentRepository.findActiveByToPartnerId(partnerId);
        return assignments.stream().map(this::convertToResponse).collect(Collectors.toList());
    }

    /**
     * 새로운 자재코드 할당을 생성합니다.
     * 중복 할당 검증을 수행하고, 사용자 타입에 따라 할당 레벨을 설정합니다.
     * 
     * @param request 자재코드 할당 요청 데이터
     * @param userType 사용자 타입 (HEADQUARTERS 또는 PARTNER)
     * @param headquartersId 본사 ID
     * @param currentPartnerId 현재 협력사 ID (협력사가 할당하는 경우)
     * @return 생성된 자재코드 할당 정보
     * @throws IllegalArgumentException 중복 할당이 존재하는 경우
     */
    @Transactional
    public MaterialAssignmentResponse createAssignment(MaterialAssignmentRequest request, String userType, String headquartersId, String currentPartnerId) {
        log.info("자재코드 할당 생성 시작: 받는 협력사 ID {}", request.getToPartnerId());

        // 중복 할당 검증
        materialAssignmentRepository
                .findByMaterialCodeAndToPartnerId(request.getMaterialInfo().getMaterialCode(), request.getToPartnerId())
                .ifPresent(existing -> {
                    throw new IllegalArgumentException(
                        String.format("협력사 %s에 이미 자재코드 %s가 할당되어 있습니다",
                                    request.getToPartnerId(), request.getMaterialInfo().getMaterialCode()));
                });

        MaterialAssignment assignment = buildAssignment(request, userType, headquartersId, currentPartnerId);
        MaterialAssignment savedAssignment = materialAssignmentRepository.save(assignment);
        log.info("자재코드 할당 생성 완료: ID {}", savedAssignment.getId());
        return convertToResponse(savedAssignment);
    }

    /**
     * 여러 자재코드를 한 번에 일괄 할당합니다.
     * 중복되는 자재코드는 건너뛰고, 나머지만 할당합니다.
     * 
     * @param request 일괄 할당 요청 데이터
     * @param userType 사용자 타입 (HEADQUARTERS 또는 PARTNER)
     * @param headquartersId 본사 ID
     * @param currentPartnerId 현재 협력사 ID (협력사가 할당하는 경우)
     * @return 생성된 자재코드 할당 목록
     */
    @Transactional
    public List<MaterialAssignmentResponse> createBatchAssignments(MaterialAssignmentBatchRequest request, String userType, String headquartersId, String currentPartnerId) {
        log.info("자재코드 일괄 할당 시작: 받는 협력사 ID {}, {}개 자재코드",
                request.getToPartnerId(), request.getMaterialCodes().size());

        List<MaterialAssignment> assignments = request.getMaterialCodes().stream()
                .map(materialInfo -> {
                    // 중복 검사 후 건너뛰기
                    Optional<MaterialAssignment> existing = materialAssignmentRepository
                            .findByMaterialCodeAndToPartnerId(materialInfo.getMaterialCode(), request.getToPartnerId());
                    if (existing.isPresent()) {
                        log.warn("중복된 자재코드 건너뛰기: {}", materialInfo.getMaterialCode());
                        return null;
                    }
                    // 개별 요청 객체 생성 (MaterialInfo 객체 직접 사용)
                    MaterialAssignmentRequest individualRequest = MaterialAssignmentRequest.builder()
                            .toPartnerId(request.getToPartnerId())
                            .materialInfo(materialInfo)
                            .assignedBy(request.getAssignedBy())
                            .assignmentReason(request.getAssignmentReason())
                            .build();
                    return buildAssignment(individualRequest, userType, headquartersId, currentPartnerId);
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        List<MaterialAssignment> savedAssignments = materialAssignmentRepository.saveAll(assignments);
        log.info("자재코드 일괄 할당 완료: {}개 생성", savedAssignments.size());
        return savedAssignments.stream().map(this::convertToResponse).collect(Collectors.toList());
    }

    /**
     * 기존 자재코드 할당 정보를 수정합니다.
     * 매핑이 생성된 할당은 수정할 수 없습니다.
     * 
     * @param assignmentId 할당 ID
     * @param request 수정할 할당 데이터
     * @return 수정된 할당 정보
     * @throws IllegalArgumentException 할당이 존재하지 않거나 이미 매핑된 경우, 중복 할당인 경우
     */
    @Transactional
    public MaterialAssignmentResponse updateAssignment(Long assignmentId, MaterialAssignmentRequest request) {
        log.info("자재코드 할당 수정 시작: ID {}", assignmentId);
        
        // 할당 존재 여부 확인
        MaterialAssignment assignment = materialAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new IllegalArgumentException("자재코드 할당을 찾을 수 없습니다: " + assignmentId));
        
        // 매핑 생성 여부 확인 (매핑된 할당은 수정 불가)
        if (assignment.getIsMapped()) {
            throw new IllegalArgumentException("이미 매핑이 생성된 할당은 수정할 수 없습니다");
        }
        
        // 자재코드가 변경되는 경우 중복 검증
        if (!assignment.getMaterialCode().equals(request.getMaterialInfo().getMaterialCode())) {
            materialAssignmentRepository
                    .findByMaterialCodeAndToPartnerId(request.getMaterialInfo().getMaterialCode(), request.getToPartnerId())
                    .ifPresent(existing -> {
                        if (!existing.getId().equals(assignmentId)) {
                            throw new IllegalArgumentException(
                                String.format("협력사 %s에 이미 자재코드 %s가 할당되어 있습니다",
                                            request.getToPartnerId(), request.getMaterialInfo().getMaterialCode()));
                        }
                    });
        }
        
        // 할당 정보 업데이트
        MaterialAssignment updatedAssignment = assignment.toBuilder()
                .materialCode(request.getMaterialInfo().getMaterialCode())
                .materialName(request.getMaterialInfo().getMaterialName())
                .materialCategory(request.getMaterialInfo().getMaterialCategory())
                .materialDescription(request.getMaterialInfo().getMaterialDescription())
                .build();
        
        MaterialAssignment savedAssignment = materialAssignmentRepository.save(updatedAssignment);
        return convertToResponse(savedAssignment);
    }

    /**
     * 자재코드 할당을 삭제합니다.
     * 매핑이 생성된 할당은 삭제할 수 없습니다.
     * 
     * @param assignmentId 삭제할 할당 ID
     * @throws IllegalArgumentException 할당이 존재하지 않거나 이미 매핑된 경우
     */
    @Transactional
    public void deleteAssignment(Long assignmentId) {
        log.info("자재코드 할당 삭제 시작: ID {}", assignmentId);
        
        MaterialAssignment assignment = materialAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new IllegalArgumentException("자재코드 할당을 찾을 수 없습니다: " + assignmentId));
        
        // 매핑 생성 여부 확인 (매핑된 할당은 삭제 불가)
        if (assignment.getIsMapped()) {
            throw new IllegalArgumentException("이미 매핑이 생성된 할당은 삭제할 수 없습니다");
        }
        
        materialAssignmentRepository.delete(assignment);
        log.info("자재코드 할당 삭제 완료: ID {}", assignmentId);
    }

    /**
     * 자재코드 할당의 삭제 가능 여부를 확인합니다.
     * 매핑이 생성되지 않은 할당만 삭제 가능합니다.
     * 
     * @param assignmentId 확인할 할당 ID
     * @return 삭제 가능 여부와 사유 정보
     * @throws IllegalArgumentException 할당이 존재하지 않는 경우
     */
    @Transactional(readOnly = true)
    public Map<String, Object> canDeleteAssignment(Long assignmentId) {
        MaterialAssignment assignment = materialAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new IllegalArgumentException("할당을 찾을 수 없습니다: ID " + assignmentId));
        
        Map<String, Object> result = new HashMap<>();
        boolean isMapped = assignment.getIsMapped() != null && assignment.getIsMapped();
        
        result.put("canDelete", !isMapped);
        if (isMapped) {
            result.put("reason", "이 자재코드는 Scope 계산기에서 사용 중이어서 삭제할 수 없습니다.");
            result.put("mappedCodes", List.of(assignment.getMaterialCode()));
        } else {
            result.put("reason", "삭제 가능한 자재코드입니다.");
            result.put("mappedCodes", List.of());
        }
        return result;
    }

    /**
     * MaterialAssignment 엔티티를 생성합니다.
     * 사용자 타입에 따라 할당 레벨과 출처를 설정합니다.
     * 
     * @param request 할당 요청 데이터
     * @param userType 사용자 타입 (HEADQUARTERS 또는 PARTNER)
     * @param headquartersId 본사 ID
     * @param currentPartnerId 현재 협력사 ID (협력사가 할당하는 경우)
     * @return 생성된 MaterialAssignment 엔티티
     */
    private MaterialAssignment buildAssignment(MaterialAssignmentRequest request, String userType, String headquartersId, String currentPartnerId) {
        Long hqId = Long.parseLong(headquartersId);
        String fromPartnerBusinessId = null;
        Integer fromLevel = 0; // 본사가 할당하는 경우 기본 레벨

        // 협력사가 할당하는 경우
        if ("PARTNER".equals(userType)) {
            fromPartnerBusinessId = currentPartnerId; // 헤더의 X-PARTNER-ID 사용
            fromLevel = 1;
        }

        Integer toLevel = fromLevel + 1;

        // UUID → 비즈니스 ID 변환 로직
        String toPartnerBusinessId = convertToBusinessId(request.getToPartnerId());

        return MaterialAssignment.builder()
                .headquartersId(hqId)
                .fromPartnerId(fromPartnerBusinessId) // 비즈니스 ID 저장
                .toPartnerId(toPartnerBusinessId) // 변환된 비즈니스 ID 저장
                .fromLevel(fromLevel)
                .toLevel(toLevel)
                .materialCode(request.getMaterialInfo().getMaterialCode())
                .materialName(request.getMaterialInfo().getMaterialName())
                .materialCategory(request.getMaterialInfo().getMaterialCategory())
                .materialDescription(request.getMaterialInfo().getMaterialDescription())
                .isActive(true)
                .isMapped(false)
                .build();
    }

    /**
     * UUID를 비즈니스 ID로 변환합니다.
     * UUID 형식이면 Auth-Service를 호출하여 변환하고,
     * 이미 비즈니스 ID 형식이면 그대로 반환합니다.
     * 
     * @param partnerId UUID 또는 비즈니스 ID
     * @return 변환된 비즈니스 ID
     * @throws IllegalArgumentException UUID 변환 실패 시
     */
    private String convertToBusinessId(String partnerId) {
        // UUID 형식인지 확인
        if (UuidUtil.isValidUUID(partnerId)) {
            log.info("UUID 형식 감지, 비즈니스 ID로 변환 시도: {}", partnerId);
            try {
                // Auth-Service 호출하여 UUID → 비즈니스 ID 변환
                ApiResponse<String> response = authServiceClient.getBusinessIdByUuid(partnerId);
                if (response != null && response.isSuccess() && response.getData() != null) {
                    String businessId = response.getData();
                    log.info("UUID {} → 비즈니스 ID {} 변환 성공", partnerId, businessId);
                    return businessId;
                } else {
                    throw new IllegalArgumentException("Auth-Service에서 UUID 변환 실패: " + partnerId);
                }
            } catch (Exception e) {
                log.error("UUID {} 변환 중 오류 발생: {}", partnerId, e.getMessage());
                throw new IllegalArgumentException("협력사 UUID 변환에 실패했습니다: " + partnerId, e);
            }
        } else {
            // 이미 비즈니스 ID 형식이면 그대로 반환
            log.debug("비즈니스 ID 형식으로 그대로 사용: {}", partnerId);
            return partnerId;
        }
    }

    /**
     * MaterialAssignment 엔티티를 MaterialAssignmentResponse DTO로 변환합니다.
     * 엔티티의 모든 필드와 비즈니스 메서드 결과를 매핑합니다.
     * 
     * @param assignment MaterialAssignment 엔티티
     * @return MaterialAssignmentResponse DTO
     */
    private MaterialAssignmentResponse convertToResponse(MaterialAssignment assignment) {
        return MaterialAssignmentResponse.builder()
                // 기본 정보 (Basic Information)
                .id(assignment.getId())
                .headquartersId(assignment.getHeadquartersId())
                .fromPartnerId(assignment.getFromPartnerId())
                .toPartnerId(assignment.getToPartnerId())
                .fromLevel(assignment.getFromLevel())
                .toLevel(assignment.getToLevel())
                
                // 자재코드 정보 (Material Code Information)
                .materialCode(assignment.getMaterialCode())
                .materialName(assignment.getMaterialName())
                .materialCategory(assignment.getMaterialCategory())
                .materialDescription(assignment.getMaterialDescription())
                
                // 메타 정보 (Meta Information)
                .isActive(assignment.getIsActive())
                .isMapped(assignment.getIsMapped())
                .createdAt(assignment.getCreatedAt())
                .updatedAt(assignment.getUpdatedAt())
                
                // 비즈니스 메서드 결과 (Business Method Results)
                .mappingCount(assignment.getMappingCount()) // getMappingCount() 결과
                .activeMappingCount(assignment.getActiveMappingCount()) // getActiveMappingCount() 결과
                .assignmentInfo(assignment.getAssignmentInfo()) // getAssignmentInfo() 결과
                .isModifiable(assignment.isModifiable()) // isModifiable() 결과
                .isDeletable(assignment.isDeletable()) // isDeletable() 결과
                .build();
    }
}