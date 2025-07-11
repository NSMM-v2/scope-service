package com.nsmm.esg.scope_service.dto.response;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 카테고리별 월별 배출량 집계 응답 DTO
 * 
 * 특정 Scope 타입의 카테고리별 월별 배출량 정보를 담는 DTO
 * 로그인된 사용자의 컨텍스트에 맞는 카테고리별 월별 데이터 제공
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "카테고리별 월별 배출량 집계 응답")
public class CategoryMonthlyEmission {

    @Schema(description = "카테고리 번호", example = "1")
    private Integer categoryNumber;

    @Schema(description = "카테고리명", example = "고정연소")
    private String categoryName;

    @Schema(description = "연도", example = "2024")
    private Integer year;

    @Schema(description = "월", example = "12")
    private Integer month;

    @Schema(description = "해당 카테고리의 월별 총 배출량", example = "1234.56")
    private BigDecimal totalEmission;

    @Schema(description = "해당 카테고리의 월별 데이터 건수", example = "15")
    private Long dataCount;

    @Schema(description = "Scope 타입", example = "SCOPE1")
    private String scopeType;

    @Schema(description = "해당 월의 모든 카테고리 총 배출량 합계", example = "12345.67")
    private BigDecimal totalSumAllCategories;
}