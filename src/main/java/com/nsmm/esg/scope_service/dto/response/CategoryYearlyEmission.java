package com.nsmm.esg.scope_service.dto.response;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 카테고리별 연간 배출량 집계 응답 DTO
 * 
 * 특정 Scope 타입의 카테고리별 연간 총 배출량 정보를 담는 DTO
 * 로그인된 사용자의 컨텍스트에 맞는 카테고리별 연간 데이터 제공
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "카테고리별 연간 배출량 집계 응답")
public class CategoryYearlyEmission {

    @Schema(description = "카테고리 번호", example = "1")
    private Integer categoryNumber;

    @Schema(description = "카테고리명", example = "고정연소")
    private String categoryName;

    @Schema(description = "연도", example = "2024")
    private Integer year;

    @Schema(description = "해당 카테고리의 연간 총 배출량", example = "15678.90")
    private BigDecimal totalEmission;

    @Schema(description = "해당 카테고리의 연간 데이터 건수", example = "180")
    private Long dataCount;

    @Schema(description = "Scope 타입", example = "SCOPE1")
    private String scopeType;

    @Schema(description = "모든 카테고리의 연간 총 배출량 합계", example = "125678.90")
    private BigDecimal totalSumAllCategories;
}