package com.nsmm.esg.scope_service.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 자재코드 일괄 할당 요청 DTO
 * 
 * 여러 자재코드를 한 번에 할당할 때 사용
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaterialAssignmentBatchRequest {

    // 할당 대상 정보
    @NotNull(message = "할당받는 협력사 UUID는 필수입니다")
    private String toPartnerId;

    // 할당할 자재코드 목록
    @NotEmpty(message = "할당할 자재코드가 최소 1개는 있어야 합니다")
    @Valid
    private List<MaterialCodeInfo> materialCodes;

    // 공통 할당 메타 정보
    @Size(max = 100, message = "할당자 정보는 100자 이하여야 합니다")
    private String assignedBy;

    @Size(max = 500, message = "할당 사유는 500자 이하여야 합니다")
    private String assignmentReason;

    /**
     * 자재코드 정보 내부 클래스
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MaterialCodeInfo {

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
    }
}