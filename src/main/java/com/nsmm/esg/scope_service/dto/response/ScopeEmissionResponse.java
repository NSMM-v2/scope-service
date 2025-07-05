package com.nsmm.esg.scope_service.dto.response;

import com.nsmm.esg.scope_service.entity.ScopeType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Scope 배출량 응답 DTO (Scope1/2/3 통합)")
public class ScopeEmissionResponse {
  private Long id;
  private ScopeType scopeType;
  private Integer categoryNumber;
  private String categoryName;
  private String companyProduct; // Scope1/2만 값, Scope3은 null
  private String companyProductCode; // Scope1/2만 값, Scope3은 null
  private String majorCategory;
  private String subcategory;
  private String rawMaterial;
  private String unit;
  private BigDecimal emissionFactor;
  private BigDecimal activityAmount;
  private BigDecimal totalEmission;
  private Boolean isManualInput;
  private Integer reportingYear;
  private Integer reportingMonth;
  private Long headquartersId;
  private Long partnerId;
  private String treePath;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}