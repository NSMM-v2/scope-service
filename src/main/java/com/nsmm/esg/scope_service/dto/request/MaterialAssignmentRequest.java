package com.nsmm.esg.scope_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 자재코드 할당 요청 DTO
 * 
 * 협력사에게 자재코드를 할당할 때 사용되는 요청 데이터
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaterialAssignmentRequest {

    // 자재코드 정보
    @NotBlank(message = "자재코드는 필수입니다")
    @Size(max = 50, message = "자재코드는 50자 이하여야 합니다")
    private String materialCode;

    @NotBlank(message = "자재명은 필수입니다")
    @Size(max = 200, message = "자재명은 200자 이하여야 합니다")
    private String materialName;

    @Size(max = 100, message = "카테고리는 100자 이하여야 합니다")
    private String materialCategory;

    @Size(max = 500, message = "자재 스펙은 500자 이하여야 합니다")
    private String materialSpec;

    @Size(max = 1000, message = "자재 설명은 1000자 이하여야 합니다")
    private String materialDescription;

    // 할당 대상 정보
    @NotNull(message = "할당받는 협력사 UUID는 필수입니다")
    private String toPartnerId;

    // 할당 메타 정보
    @Size(max = 100, message = "할당자 정보는 100자 이하여야 합니다")
    private String assignedBy;

    @Size(max = 500, message = "할당 사유는 500자 이하여야 합니다")
    private String assignmentReason;
}