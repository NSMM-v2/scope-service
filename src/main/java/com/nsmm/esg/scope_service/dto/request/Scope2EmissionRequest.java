package com.nsmm.esg.scope_service.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Scope 2 배출량 생성 요청 DTO (프론트엔드 입력 구조와 1:1 매핑)")
public class Scope2EmissionRequest {
    @Schema(description = "제품", example = "제품명")
    @Size(max = 20, message = "제품명은 20자 이하여야 합니다")
    private String companyProduct;

    @Schema(description = "제품코드", example = "회사 제품 코드")
    @Size(max = 10, message = "제품 코드는 10자 이하여야 합니다")
    private String companyProductCode;

    @Schema(description = "대분류", example = "구매한 제품 및 서비스")
    @NotBlank(message = "대분류는 필수입니다")
    @Size(max = 100, message = "대분류는 100자 이하여야 합니다")
    private String majorCategory; // 대분류 (프론트 category)

    @Schema(description = "구분", example = "원재료")
    @NotBlank(message = "구분은 필수입니다")
    @Size(max = 100, message = "구분은 100자 이하여야 합니다")
    private String subcategory; // 구분 (프론트 separate)

    @Schema(description = "원료/에너지", example = "철강")
    @NotBlank(message = "원료/에너지는 필수입니다")
    @Size(max = 100, message = "원료/에너지는 100자 이하여야 합니다")
    private String rawMaterial; // 원료/에너지 (프론트 rawMaterial)

    @Schema(description = "단위", example = "kg")
    @NotBlank(message = "단위는 필수입니다")
    @Size(max = 20, message = "단위는 20자 이하여야 합니다")
    private String unit; // 단위 (프론트 unit)

    @Schema(description = "배출계수 (kgCO2eq/단위)", example = "2.1")
    @NotNull(message = "배출계수는 필수입니다")
    @DecimalMin(value = "0.000001", message = "배출계수는 0.000001 이상이어야 합니다")
    @Digits(integer = 9, fraction = 6, message = "배출계수는 정수 9자리, 소수점 6자리까지 가능합니다")
    private BigDecimal emissionFactor; // 배출계수 (프론트 emissionFactor)

    @Schema(description = "수량(활동량)", example = "1000")
    @NotNull(message = "수량(활동량)은 필수입니다")
    @DecimalMin(value = "0.001", message = "수량은 0.001 이상이어야 합니다")
    @Digits(integer = 12, fraction = 3, message = "수량은 정수 12자리, 소수점 3자리까지 가능합니다")
    private BigDecimal activityAmount; // 수량(활동량, 프론트 quantity)

    @Schema(description = "계산된 배출량", example = "2100.0")
    @NotNull(message = "계산된 배출량은 필수입니다")
    @DecimalMin(value = "0.000001", message = "계산된 배출량은 0.000001 이상이어야 합니다")
    @Digits(integer = 15, fraction = 6, message = "계산된 배출량은 정수 15자리, 소수점 6자리까지 가능합니다")
    private BigDecimal totalEmission; // 계산된 배출량 (프론트 totalEmission)

    @Schema(description = "수동 입력 여부", example = "true")
    @Builder.Default
    private Boolean isManualInput = false; // 수동 입력 여부 (true: 수동, false: 자동)

    @Schema(description = "보고 연도", example = "2024")
    @NotNull(message = "보고 연도는 필수입니다")
    private Integer reportingYear; // 보고 연도

    @Schema(description = "보고 월", example = "6")
    @NotNull(message = "보고 월은 필수입니다")
    @Min(value = 1, message = "보고 월은 1 이상이어야 합니다")
    @Max(value = 12, message = "보고 월은 12 이하여야 합니다")
    private Integer reportingMonth; // 보고 월

    @Schema(description = "카테고리 번호 (1~10)", example = "1")
    @NotNull(message = "카테고리 번호는 필수입니다")
    @Min(value = 1, message = "카테고리 번호는 1 이상이어야 합니다")
    @Max(value = 10, message = "카테고리 번호는 10 이하여야 합니다")
    private Integer scope2CategoryNumber; // 카테고리 번호 (1-10)

    @Schema(description = "카테고리 명칭", example = "구매한 제품 및 서비스")
    @NotBlank(message = "카테고리 명칭은 필수입니다")
    @Size(max = 100, message = "카테고리 명칭은 100자 이하여야 합니다")
    private String scope2CategoryName; // 카테고리 명칭
}
