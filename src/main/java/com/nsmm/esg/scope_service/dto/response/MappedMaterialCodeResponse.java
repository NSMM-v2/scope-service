package com.nsmm.esg.scope_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 맵핑된 자재코드 목록 응답 DTO
 * 
 * 기능:
 * - MaterialAssignment 테이블에서 isMapped = true인 자재 정보 제공
 * - 자재코드, 자재명, 자재설명만 간단하게 반환
 * - 배출량 집계 없이 자재 기본 정보만 제공
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MappedMaterialCodeResponse {

    // 자재코드 (A100, B200 등)
    private String materialCode;
    
    // 자재명 (철강 자재, 플라스틱 원료 등)
    private String materialName;
    
    // 자재설명 (고강도 철강 원자재, 재활용 플라스틱 펠릿 등)
    private String materialDescription;

    /**
     * Object[] 배열에서 MappedMaterialCodeResponse 생성
     * Repository 쿼리 결과: [materialCode, materialName, materialDescription]
     * 
     * @param result 쿼리 결과 배열
     * @return MappedMaterialCodeResponse 객체
     */
    public static MappedMaterialCodeResponse fromQueryResult(Object[] result) {
        return MappedMaterialCodeResponse.builder()
                .materialCode((String) result[0])
                .materialName((String) result[1])
                .materialDescription((String) result[2])
                .build();
    }
}