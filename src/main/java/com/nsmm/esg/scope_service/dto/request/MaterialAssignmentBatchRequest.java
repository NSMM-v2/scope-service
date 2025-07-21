package com.nsmm.esg.scope_service.dto.request;

import com.nsmm.esg.scope_service.dto.MaterialInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
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
    @NotNull(message = "할당받는 협력사 ID는 필수입니다")
    @Schema(description = "할당받는 협력사 ID (비즈니스 ID)")
    private String toPartnerId;

    // 할당할 자재코드 목록
    @NotEmpty(message = "할당할 자재코드가 최소 1개는 있어야 합니다")
    @Valid
    @Schema(description = "할당할 자재 정보 목록")
    private List<MaterialInfo> materialCodes;

    // 공통 할당 메타 정보
    @Size(max = 100, message = "할당자 정보는 100자 이하여야 합니다")
    @Schema(description = "할당자 정보", example = "admin")
    private String assignedBy;

    @Size(max = 500, message = "할당 사유는 500자 이하여야 합니다")
    @Schema(description = "할당 사유", example = "신규 협력사들에 대한 일괄 자재 공급 설정")
    private String assignmentReason;

}