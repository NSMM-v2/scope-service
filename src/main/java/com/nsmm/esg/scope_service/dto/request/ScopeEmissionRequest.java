package com.nsmm.esg.scope_service.dto.request;

import com.nsmm.esg.scope_service.entity.ScopeType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Scope 배출량 생성/수정 요청 DTO (Scope1/2/3 통합)")
public class ScopeEmissionRequest {
    // Scope 구분
    @NotNull
    private ScopeType scopeType;

    // Scope별 카테고리 번호/명칭
    private Integer categoryNumber;
    private String categoryName;

    // 제품 정보 (Scope1/2만 사용, Scope3은 null)
    private String companyProduct;
    private String companyProductCode;

    // 공통 입력 필드
    @NotBlank
    private String majorCategory;
    @NotBlank
    private String subcategory;
    @NotBlank
    private String rawMaterial;
    @NotBlank
    private String unit;
    @NotNull
    private BigDecimal emissionFactor;
    @NotNull
    private BigDecimal activityAmount;
    @NotNull
    private BigDecimal totalEmission;
    private Boolean isManualInput;
    @NotNull
    private Integer reportingYear;
    @NotNull
    private Integer reportingMonth;

    /**
     * Scope 타입 설정
     */
    public void setScopeType(ScopeType scopeType) {
        this.scopeType = scopeType;
    }
}