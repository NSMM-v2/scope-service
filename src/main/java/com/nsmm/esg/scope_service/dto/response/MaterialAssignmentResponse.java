package com.nsmm.esg.scope_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 자재코드 할당 응답 DTO
 * 
 * 자재코드 할당 정보를 클라이언트에게 반환할 때 사용
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaterialAssignmentResponse {

    // 기본 정보
    private Long id;
    private Long headquartersId;
    private String fromPartnerId;
    private String toPartnerId;
    private Integer fromLevel;
    private Integer toLevel;

    // 자재코드 정보
    private String materialCode;
    private String materialName;
    private String materialCategory;
    private String materialSpec;
    private String materialDescription;

    // 할당 메타 정보
    private String assignedBy;
    private String assignmentReason;
    private Boolean isActive;
    private Boolean isMapped;

    // 연결 정보
    private Integer mappingCount;
    private Long activeMappingCount;

    // 시간 정보
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 할당 관계 정보
    private String fromPartnerName; // 할당하는 협력사명 (조인 데이터)
    private String toPartnerName;   // 할당받는 협력사명 (조인 데이터)
}