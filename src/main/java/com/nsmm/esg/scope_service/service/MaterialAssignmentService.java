package com.nsmm.esg.scope_service.service;

import com.nsmm.esg.scope_service.client.AuthServiceClient;
import com.nsmm.esg.scope_service.dto.ApiResponse;
import com.nsmm.esg.scope_service.dto.request.MaterialAssignmentBatchRequest;
import com.nsmm.esg.scope_service.dto.request.MaterialAssignmentRequest;
import com.nsmm.esg.scope_service.dto.response.MaterialAssignmentResponse;
import com.nsmm.esg.scope_service.dto.response.MaterialDataResponse;
import com.nsmm.esg.scope_service.entity.MaterialAssignment;
import com.nsmm.esg.scope_service.repository.MaterialAssignmentRepository;
import com.nsmm.esg.scope_service.util.UuidUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MaterialAssignmentService {

    private final MaterialAssignmentRepository materialAssignmentRepository;
    private final MaterialDataService materialDataService;
    private final AuthServiceClient authServiceClient;

    // ... (기존 조회 메소드들은 변경 없음) ...

    @Transactional(readOnly = true)
    public List<MaterialAssignmentResponse> getAssignmentsByPartner(String partnerId) {
        log.info("협력사 {}의 자재코드 할당 목록 조회 시작", partnerId);
        
        // UUID → partnerId 변환 (저장 시와 동일한 로직 적용)
        String businessId = convertToBusinessId(partnerId);
        log.info("조회용 비즈니스 ID 변환: {} → {}", partnerId, businessId);
        
        List<MaterialAssignment> assignments = materialAssignmentRepository.findActiveByToPartnerId(businessId);
        return assignments.stream().map(this::convertToResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MaterialAssignmentResponse> getAssignmentsByHeadquarters(Long headquartersId, String userType) {
        if (!"HEADQUARTERS".equals(userType)) {
            throw new IllegalArgumentException("본사 전체 할당 목록은 본사 계정만 조회할 수 있습니다");
        }
        List<MaterialAssignment> assignments = materialAssignmentRepository.findByHeadquartersId(headquartersId);
        return assignments.stream().map(this::convertToResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MaterialDataResponse> getMyMaterialData(String userType, String headquartersId, String partnerId) {
        log.info("사용자 자재 데이터 조회 시작 - 사용자타입: {}, 본사ID: {}, 협력사ID: {}", userType, headquartersId, partnerId);
        if ("HEADQUARTERS".equals(userType)) {
            return materialDataService.getHeadquartersDummyData();
        } else {
            return getPartnerMaterialData(partnerId);
        }
    }

    private List<MaterialDataResponse> getPartnerMaterialData(String partnerId) {
        List<MaterialAssignment> assignments = materialAssignmentRepository.findActiveByToPartnerId(partnerId);
        return assignments.stream().map(this::convertAssignmentToMaterialData).collect(Collectors.toList());
    }

    @Transactional
    public MaterialAssignmentResponse createAssignment(MaterialAssignmentRequest request, String userType, String headquartersId, String currentPartnerId) {
        log.info("자재코드 할당 생성 시작: 받는 협력사 ID {}", request.getToPartnerId());

        materialAssignmentRepository
                .findByMaterialCodeAndToPartnerId(request.getMaterialCode(), request.getToPartnerId())
                .ifPresent(existing -> {
                    throw new IllegalArgumentException(
                        String.format("협력사 %s에 이미 자재코드 %s가 할당되어 있습니다",
                                    request.getToPartnerId(), request.getMaterialCode()));
                });

        MaterialAssignment assignment = buildAssignment(request, userType, headquartersId, currentPartnerId);
        MaterialAssignment savedAssignment = materialAssignmentRepository.save(assignment);
        log.info("자재코드 할당 생성 완료: ID {}", savedAssignment.getId());
        return convertToResponse(savedAssignment);
    }

    @Transactional
    public List<MaterialAssignmentResponse> createBatchAssignments(MaterialAssignmentBatchRequest request, String userType, String headquartersId, String currentPartnerId) {
        log.info("자재코드 일괄 할당 시작: 받는 협력사 ID {}, {}개 자재코드",
                request.getToPartnerId(), request.getMaterialCodes().size());

        List<MaterialAssignment> assignments = request.getMaterialCodes().stream()
                .map(materialCode -> {
                    Optional<MaterialAssignment> existing = materialAssignmentRepository
                            .findByMaterialCodeAndToPartnerId(materialCode.getMaterialCode(), request.getToPartnerId());
                    if (existing.isPresent()) {
                        log.warn("중복된 자재코드 건너뛰기: {}", materialCode.getMaterialCode());
                        return null;
                    }
                    MaterialAssignmentRequest individualRequest = MaterialAssignmentRequest.builder()
                            .toPartnerId(request.getToPartnerId())
                            .materialCode(materialCode.getMaterialCode())
                            .materialName(materialCode.getMaterialName())
                            .materialCategory(materialCode.getMaterialCategory())
                            .materialSpec(materialCode.getMaterialSpec())
                            .materialDescription(materialCode.getMaterialDescription())
                            .build();
                    return buildAssignment(individualRequest, userType, headquartersId, currentPartnerId);
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        List<MaterialAssignment> savedAssignments = materialAssignmentRepository.saveAll(assignments);
        log.info("자재코드 일괄 할당 완료: {}개 생성", savedAssignments.size());
        return savedAssignments.stream().map(this::convertToResponse).collect(Collectors.toList());
    }

    private MaterialAssignment buildAssignment(MaterialAssignmentRequest request, String userType, String headquartersId, String currentPartnerId) {
        Long hqId = Long.parseLong(headquartersId);
        String fromPartnerBusinessId = null;
        Integer fromLevel = 0; // 본사가 할당하는 경우 기본 레벨

        if ("PARTNER".equals(userType)) {
            fromPartnerBusinessId = currentPartnerId; // 헤더의 X-PARTNER-ID 사용
            // 협력사의 레벨은 간단히 1로 설정 (필요시 추후 헤더에서 X-LEVEL 사용 가능)
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
                .materialCode(request.getMaterialCode())
                .materialName(request.getMaterialName())
                .materialCategory(request.getMaterialCategory())
                .materialDescription(request.getMaterialDescription())
                .isActive(true)
                .isMapped(false)
                .build();
    }

    /**
     * UUID를 비즈니스 ID로 변환하는 메서드
     * UUID 형식이면 Auth-Service를 호출하여 비즈니스 ID로 변환
     * 이미 비즈니스 ID 형식이면 그대로 반환
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

    private MaterialAssignmentResponse convertToResponse(MaterialAssignment assignment) {
        return MaterialAssignmentResponse.builder()
                .id(assignment.getId())
                .headquartersId(assignment.getHeadquartersId())
                .fromPartnerId(assignment.getFromPartnerId())
                .toPartnerId(assignment.getToPartnerId())
                .fromLevel(assignment.getFromLevel())
                .toLevel(assignment.getToLevel())
                .materialCode(assignment.getMaterialCode())
                .materialName(assignment.getMaterialName())
                .materialCategory(assignment.getMaterialCategory())
                .materialDescription(assignment.getMaterialDescription())
                .isActive(assignment.getIsActive())
                .isMapped(assignment.getIsMapped())
                .mappingCount(assignment.getMappingCount())
                .activeMappingCount(assignment.getActiveMappingCount())
                .createdAt(assignment.getCreatedAt())
                .updatedAt(assignment.getUpdatedAt())
                .build();
    }
    
    // ... (기타 기존 메소드들은 변경 없음) ...

    private MaterialDataResponse convertAssignmentToMaterialData(MaterialAssignment assignment) {
        BigDecimal emissionFactor = getDefaultEmissionFactor(assignment.getMaterialCode(), assignment.getMaterialCategory());
        return MaterialDataResponse.builder()
                .materialCode(assignment.getMaterialCode())
                .materialName(assignment.getMaterialName())
                .materialCategory(assignment.getMaterialCategory())
                .materialSubCategory(determineSubCategory(assignment.getMaterialCategory()))
                .materialDescription(assignment.getMaterialDescription())
                .materialSpec("협력사 할당 자재 - 스펙 정보 없음")
                .emissionFactor(emissionFactor)
                .unit(getDefaultUnit(assignment.getMaterialCategory()))
                .scopeCategory("SCOPE3")
                .scope3CategoryNumber(1)
                .accessType(MaterialDataResponse.AccessType.modifiable)
                .assignmentSource(assignment.getFromPartnerId() == null ?
                    MaterialDataResponse.AssignmentSource.headquarters :
                    MaterialDataResponse.AssignmentSource.parent_partner)
                .assignedBy(assignment.getFromPartnerId() == null ? "본사" : "상위 협력사")
                .assignmentReason("협력사 자재 사용을 위한 할당")
                .isAssigned(true)
                .isUsed(assignment.getIsMapped())
                .usageCount(assignment.getMappingCount())
                .lastUsedDate(assignment.getUpdatedAt() != null ? assignment.getUpdatedAt().toString() : null)
                .supplierInfo("협력사 관리 자재")
                .qualityGrade("B")
                .isEcoFriendly(false)
                .build();
    }

    private BigDecimal getDefaultEmissionFactor(String materialCode, String materialCategory) {
        if (materialCategory == null) return new BigDecimal("2.0");
        switch (materialCategory.toLowerCase()) {
            case "강재": case "철강": return new BigDecimal("2.3");
            case "알루미늄": case "비철금속": return new BigDecimal("11.5");
            case "플라스틱": return new BigDecimal("3.8");
            case "고무": return new BigDecimal("3.2");
            case "전자부품": return new BigDecimal("8.5");
            case "화학원료": return new BigDecimal("2.1");
            case "유리": return new BigDecimal("0.9");
            case "텍스타일": return new BigDecimal("1.8");
            default: return new BigDecimal("2.0");
        }
    }

    private String getDefaultUnit(String materialCategory) {
        if (materialCategory == null) return "kg";
        switch (materialCategory.toLowerCase()) {
            case "강재": case "철강": case "알루미늄": case "비철금속": return "ton";
            case "플라스틱": case "고무": case "화학원료": return "kg";
            case "전자부품": return "개";
            case "유리": case "텍스타일": return "m²";
            default: return "kg";
        }
    }

    private String determineSubCategory(String materialCategory) {
        if (materialCategory == null) return "일반용";
        switch (materialCategory.toLowerCase()) {
            case "강재": case "철강": return "구조용";
            case "알루미늄": case "비철금속": return "경량화용";
            case "플라스틱": return "내외장용";
            case "고무": return "씰링용";
            case "전자부품": return "제어용";
            case "화학원료": return "가공용";
            case "유리": return "투명부품용";
            case "텍스타일": return "내장용";
            default: return "일반용";
        }
    }
    
    @Transactional
    public MaterialAssignmentResponse updateAssignment(Long assignmentId, MaterialAssignmentRequest request) {
        log.info("자재코드 할당 수정 시작: ID {}", assignmentId);
        MaterialAssignment assignment = materialAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new IllegalArgumentException("자재코드 할당을 찾을 수 없습니다: " + assignmentId));
        if (assignment.getIsMapped()) {
            throw new IllegalArgumentException("이미 매핑이 생성된 할당은 수정할 수 없습니다");
        }
        if (!assignment.getMaterialCode().equals(request.getMaterialCode())) {
            materialAssignmentRepository
                    .findByMaterialCodeAndToPartnerId(request.getMaterialCode(), request.getToPartnerId())
                    .ifPresent(existing -> {
                        if (!existing.getId().equals(assignmentId)) {
                            throw new IllegalArgumentException(
                                String.format("협력사 %s에 이미 자재코드 %s가 할당되어 있습니다",
                                            request.getToPartnerId(), request.getMaterialCode()));
                        }
                    });
        }
        MaterialAssignment updatedAssignment = assignment.toBuilder()
                .materialCode(request.getMaterialCode())
                .materialName(request.getMaterialName())
                .materialCategory(request.getMaterialCategory())
                .materialDescription(request.getMaterialDescription())
                .build();
        MaterialAssignment savedAssignment = materialAssignmentRepository.save(updatedAssignment);
        return convertToResponse(savedAssignment);
    }

    @Transactional
    public void deleteAssignment(Long assignmentId) {
        log.info("자재코드 할당 삭제 시작: ID {}", assignmentId);
        MaterialAssignment assignment = materialAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new IllegalArgumentException("자재코드 할당을 찾을 수 없습니다: " + assignmentId));
        if (assignment.getIsMapped()) {
            throw new IllegalArgumentException("이미 매핑이 생성된 할당은 삭제할 수 없습니다");
        }
        materialAssignmentRepository.delete(assignment);
        log.info("자재코드 할당 삭제 완료: ID {}", assignmentId);
    }

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
}
