package com.nsmm.esg.scope_service.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * 자재코드 할당 응답 DTO
 * 
 * MaterialAssignment 엔티티의 필드와 비즈니스 메서드 결과만을 사용하여
 * 자재코드 할당 정보를 클라이언트에게 반환할 때 사용
 */
@Schema(description = "자재코드 할당 응답 DTO")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaterialAssignmentResponse {

    // ========================================================================
    // 기본 정보 (Basic Information)
    // ========================================================================
    
    @Schema(description = "할당 ID", example = "1")
    private Long id;
    
    @Schema(description = "본사 ID", example = "1")
    private Long headquartersId;
    
    @Schema(description = "할당하는 협력사 ID (본사인 경우 null)", example = "L1-001")
    private String fromPartnerId;
    
    @Schema(description = "할당받는 협력사 ID", example = "L2-001", required = true)
    private String toPartnerId;
    
    @Schema(description = "할당하는 청체 레벨 (0: 본사, 1: 1차사...)", example = "1")
    private Integer fromLevel;
    
    @Schema(description = "할당받는 청체 레벨", example = "2", required = true)
    private Integer toLevel;

    // ========================================================================
    // 자재코드 정보 (Material Code Information)
    // ========================================================================
    
    @Schema(description = "자재코드", example = "ST001", required = true)
    private String materialCode;
    
    @Schema(description = "자재명", example = "냉간압연강판", required = true)
    private String materialName;
    
    @Schema(description = "자재 카테고리", example = "강재")
    private String materialCategory;
    
    @Schema(description = "자재 상세 설명", example = "자동차 차체 외판용 고품질 냉간압연강판")
    private String materialDescription;

    // ========================================================================
    // 메타 정보 (Meta Information)
    // ========================================================================
    
    @Schema(description = "활성 상태", example = "true")
    private Boolean isActive;
    
    @Schema(description = "매핑 생성 여부", example = "false")
    private Boolean isMapped;
    
    @Schema(description = "생성일시", example = "2024-01-15T10:30:00")
    private LocalDateTime createdAt;
    
    @Schema(description = "수정일시", example = "2024-01-15T14:20:00")
    private LocalDateTime updatedAt;

    // ========================================================================
    // 비즈니스 메서드 결과 (Business Method Results)
    // ========================================================================
    
    @Schema(description = "매핑 개수", example = "3")
    private Integer mappingCount; // getMappingCount() 결과
    
    @Schema(description = "활성 매핑 개수", example = "2")
    private Long activeMappingCount; // getActiveMappingCount() 결과
    
    @Schema(description = "할당 관계 정보", example = "본사 → 협력사(L2-001) : ST001")
    private String assignmentInfo; // getAssignmentInfo() 결과

    @Schema(description = "수정 가능 여부", example = "true")
    private Boolean isModifiable; // isModifiable() 결과
    
    @Schema(description = "삭제 가능 여부", example = "true")
    private Boolean isDeletable; // isDeletable() 결과
}