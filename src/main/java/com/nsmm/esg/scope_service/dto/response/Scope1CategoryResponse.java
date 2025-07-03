package com.nsmm.esg.scope_service.dto.response;

import com.nsmm.esg.scope_service.entity.Scope1Category;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Scope 1 카테고리 응답 DTO
 *
 * 프론트엔드 카테고리 선택기와 연동:
 * - 카테고리 목록 제공
 * - 업스트림/다운스트림 구분
 *
 * @author ESG Project Team
 * @version 1.0
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Scope 1 카테고리 응답 DTO (카테고리 선택기 연동)")
public class Scope1CategoryResponse {
        @Schema(description = "카테고리 번호 (1~10)", example = "1")
        private Integer scope1CategoryNumber; // 카테고리 번호 (1-15)

        @Schema(description = "카테고리 한국어 명칭", example = "구매한 제품 및 서비스")
        private String scope1CategoryName; // 카테고리 한국어 명칭

        /**
         * Scope3Category Enum에서 DTO로 변환하는 정적 팩토리 메서드
         *
         * @param category Scope1Category Enum
         * @return Scope3CategoryResponse DTO
         */
        public static com.nsmm.esg.scope_service.dto.response.Scope1CategoryResponse from(Scope1Category category) {
            return com.nsmm.esg.scope_service.dto.response.Scope1CategoryResponse.builder()
                    .scope1CategoryNumber(category.getScope1CategoryNumber())
                    .scope1CategoryName(category.getScope1CategoryName())
                    .build();
        }
}
