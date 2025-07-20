package com.nsmm.esg.scope_service.service;

import com.nsmm.esg.scope_service.dto.request.MaterialAssignmentBatchRequest;
import com.nsmm.esg.scope_service.dto.request.MaterialAssignmentRequest;
import com.nsmm.esg.scope_service.dto.response.MaterialAssignmentResponse;
import com.nsmm.esg.scope_service.entity.MaterialAssignment;
import com.nsmm.esg.scope_service.repository.MaterialAssignmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 자재코드 할당 서비스
 * 
 * 협력사에게 자재코드를 할당하고 관리하는 비즈니스 로직 처리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MaterialAssignmentService {

    private final MaterialAssignmentRepository materialAssignmentRepository;

    @Transactional(readOnly = true)
    public List<MaterialAssignmentResponse> getAssignmentsByPartner(String partnerUuid) {
        log.info("협력사 {}의 자재코드 할당 목록 조회 시작", partnerUuid);
        
        List<MaterialAssignment> assignments = materialAssignmentRepository.findActiveByToPartnerId(partnerUuid);
        
        List<MaterialAssignmentResponse> responses = assignments.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
        
        log.info("협력사 {}의 자재코드 할당 목록 조회 완료: {}개", partnerUuid, responses.size());
        return responses;
    }

    /**
     * 본사별 모든 자재코드 할당 목록 조회
     */
    @Transactional(readOnly = true)
    public List<MaterialAssignmentResponse> getAssignmentsByHeadquarters(Long headquartersId, String userType) {
        log.info("본사 {}의 모든 자재코드 할당 목록 조회 시작", headquartersId);
        
        // 본사 권한 검증
        if (!"HEADQUARTERS".equals(userType)) {
            throw new IllegalArgumentException("본사 전체 할당 목록은 본사 계정만 조회할 수 있습니다");
        }
        
        List<MaterialAssignment> assignments = materialAssignmentRepository.findByHeadquartersId(headquartersId);
        
        List<MaterialAssignmentResponse> responses = assignments.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
        
        log.info("본사 {}의 모든 자재코드 할당 목록 조회 완료: {}개", headquartersId, responses.size());
        return responses;
    }

    /**
     * 자재코드 할당 생성
     */
    @Transactional
    public MaterialAssignmentResponse createAssignment(MaterialAssignmentRequest request, String userType,
                                                      String headquartersId, String partnerId) {
        log.info("자재코드 할당 생성 시작: 협력사 {}, 자재코드 {}", request.getToPartnerId(), request.getMaterialCode());
        
        // 중복 체크
        Optional<MaterialAssignment> existing = materialAssignmentRepository
                .findByMaterialCodeAndToPartnerId(request.getMaterialCode(), request.getToPartnerId());
        
        if (existing.isPresent()) {
            throw new IllegalArgumentException(
                String.format("협력사 %d에 이미 자재코드 %s가 할당되어 있습니다", 
                            request.getToPartnerId(), request.getMaterialCode()));
        }
        
        // 할당 정보 구성
        MaterialAssignment assignment = buildAssignment(request, userType, headquartersId, partnerId);
        
        // 저장
        MaterialAssignment savedAssignment = materialAssignmentRepository.save(assignment);
        
        log.info("자재코드 할당 생성 완료: ID {}", savedAssignment.getId());
        return convertToResponse(savedAssignment);
    }

    /**
     * 자재코드 일괄 할당
     */
    @Transactional
    public List<MaterialAssignmentResponse> createBatchAssignments(MaterialAssignmentBatchRequest request, 
                                                                  String userType, String headquartersId, 
                                                                  String partnerId) {
        log.info("자재코드 일괄 할당 시작: 협력사 {}, {}개 자재코드", 
                request.getToPartnerId(), request.getMaterialCodes().size());
        
        List<MaterialAssignment> assignments = request.getMaterialCodes().stream()
                .map(materialCode -> {
                    // 중복 체크
                    Optional<MaterialAssignment> existing = materialAssignmentRepository
                            .findByMaterialCodeAndToPartnerId(materialCode.getMaterialCode(), request.getToPartnerId());
                    
                    if (existing.isPresent()) {
                        log.warn("중복된 자재코드 건너뛰기: {}", materialCode.getMaterialCode());
                        return null;
                    }
                    
                    // 개별 요청 객체 생성
                    MaterialAssignmentRequest individualRequest = MaterialAssignmentRequest.builder()
                            .materialCode(materialCode.getMaterialCode())
                            .materialName(materialCode.getMaterialName())
                            .materialCategory(materialCode.getMaterialCategory())
                            .materialSpec(materialCode.getMaterialSpec())
                            .materialDescription(materialCode.getMaterialDescription())
                            .toPartnerId(request.getToPartnerId())
                            .assignedBy(request.getAssignedBy())
                            .assignmentReason(request.getAssignmentReason())
                            .build();
                    
                    return buildAssignment(individualRequest, userType, headquartersId, partnerId);
                })
                .filter(assignment -> assignment != null) // 중복 제외
                .collect(Collectors.toList());
        
        // 일괄 저장
        List<MaterialAssignment> savedAssignments = materialAssignmentRepository.saveAll(assignments);
        
        List<MaterialAssignmentResponse> responses = savedAssignments.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
        
        log.info("자재코드 일괄 할당 완료: {}개 생성", responses.size());
        return responses;
    }

    /**
     * 자재코드 할당 수정
     */
    @Transactional
    public MaterialAssignmentResponse updateAssignment(Long assignmentId, MaterialAssignmentRequest request) {
        log.info("자재코드 할당 수정 시작: ID {}", assignmentId);
        
        MaterialAssignment assignment = materialAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new IllegalArgumentException("자재코드 할당을 찾을 수 없습니다: " + assignmentId));
        
        // 수정 가능 여부 확인
        if (!assignment.isModifiable()) {
            throw new IllegalArgumentException("이미 매핑이 생성된 할당은 수정할 수 없습니다");
        }
        
        // 자재코드가 변경되는 경우 중복 체크
        if (!assignment.getMaterialCode().equals(request.getMaterialCode())) {
            Optional<MaterialAssignment> existing = materialAssignmentRepository
                    .findByMaterialCodeAndToPartnerId(request.getMaterialCode(), request.getToPartnerId());
            
            if (existing.isPresent() && !existing.get().getId().equals(assignmentId)) {
                throw new IllegalArgumentException(
                    String.format("협력사 %d에 이미 자재코드 %s가 할당되어 있습니다", 
                                request.getToPartnerId(), request.getMaterialCode()));
            }
        }
        
        // 정보 업데이트
        MaterialAssignment updatedAssignment = assignment.toBuilder()
                .materialCode(request.getMaterialCode())
                .materialName(request.getMaterialName())
                .materialCategory(request.getMaterialCategory())
                .materialDescription(request.getMaterialDescription())
                .build();
        
        MaterialAssignment savedAssignment = materialAssignmentRepository.save(updatedAssignment);
        
        log.info("자재코드 할당 수정 완료: ID {}", assignmentId);
        return convertToResponse(savedAssignment);
    }

    /**
     * 자재코드 할당 삭제
     */
    @Transactional
    public void deleteAssignment(Long assignmentId) {
        log.info("자재코드 할당 삭제 시작: ID {}", assignmentId);
        
        MaterialAssignment assignment = materialAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new IllegalArgumentException("자재코드 할당을 찾을 수 없습니다: " + assignmentId));
        
        // 삭제 가능 여부 확인
        if (!assignment.isDeletable()) {
            throw new IllegalArgumentException("이미 매핑이 생성된 할당은 삭제할 수 없습니다");
        }
        
        materialAssignmentRepository.delete(assignment);
        
        log.info("자재코드 할당 삭제 완료: ID {}", assignmentId);
    }

    /**
     * 자재코드 할당 삭제 가능 여부 확인
     */
    @Transactional(readOnly = true)
    public Map<String, Object> canDeleteAssignment(Long assignmentId) {
        log.info("자재코드 할당 삭제 가능 여부 확인 시작: ID {}", assignmentId);
        
        // 할당 존재 여부 확인
        MaterialAssignment assignment = materialAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new IllegalArgumentException("할당을 찾을 수 없습니다: ID " + assignmentId));
        
        Map<String, Object> result = new HashMap<>();
        
        // Scope 계산기에서 사용 중인지 확인 (isMapped 필드 기준)
        boolean isMapped = assignment.getIsMapped() != null && assignment.getIsMapped();
        boolean canDelete = !isMapped;
        
        result.put("canDelete", canDelete);
        
        if (!canDelete) {
            result.put("reason", "이 자재코드는 Scope 계산기에서 사용 중이어서 삭제할 수 없습니다.");
            // 매핑된 코드 정보가 있다면 추가 (현재는 단순히 현재 코드 정보만 반환)
            result.put("mappedCodes", List.of(assignment.getMaterialCode()));
        } else {
            result.put("reason", "삭제 가능한 자재코드입니다.");
            result.put("mappedCodes", List.of());
        }
        
        log.info("자재코드 할당 삭제 가능 여부 확인 완료: ID {}, canDelete: {}", assignmentId, canDelete);
        return result;
    }

    /**
     * MaterialAssignment 엔티티를 Response DTO로 변환
     */
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

    /**
     * 요청 정보로부터 MaterialAssignment 엔티티 구성
     */
    private MaterialAssignment buildAssignment(MaterialAssignmentRequest request, String userType, 
                                             String headquartersId, String partnerId) {
        Long hqId = Long.parseLong(headquartersId);
        String fromPartnerId = "HEADQUARTERS".equals(userType) ? null : partnerId;
        Integer fromLevel = "HEADQUARTERS".equals(userType) ? 0 : 1; // 임시로 1차로 설정
        Integer toLevel = fromLevel + 1;
        
        return MaterialAssignment.builder()
                .headquartersId(hqId)
                .fromPartnerId(fromPartnerId)
                .toPartnerId(request.getToPartnerId())
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


}