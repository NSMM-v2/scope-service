package com.nsmm.esg.scope_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 자재 정보 공통 DTO
 * 
 * 자재코드 할당 시 사용되는 자재 정보를 담는 공통 클래스
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaterialInfo {

    @NotBlank(message = "자재코드는 필수입니다")
    @Size(max = 50, message = "자재코드는 50자 이하여야 합니다")
    @Schema(description = "자재코드", example = "MAT-001", required = true)
    private String materialCode;

    @NotBlank(message = "자재명은 필수입니다")
    @Size(max = 200, message = "자재명은 200자 이하여야 합니다")
    @Schema(description = "자재명", example = "강철판", required = true)
    private String materialName;

    @Size(max = 100, message = "카테고리는 100자 이하여야 합니다")
    @Schema(description = "자재 카테고리", example = "금속")
    private String materialCategory;

    @Size(max = 500, message = "자재 스펙은 500자 이하여야 합니다")
    @Schema(description = "자재 스펙", example = "두께: 10mm, 크기: 1000x2000mm")
    private String materialSpec;

    @Size(max = 1000, message = "자재 설명은 1000자 이하여야 합니다")
    @Schema(description = "자재 설명", example = "고강도 스틸 플레이트로 건설 및 제조업에 사용")
    private String materialDescription;
}