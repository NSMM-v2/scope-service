package com.nsmm.esg.scope_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 자재 데이터 응답 DTO
 * 
 * 사용자가 접근 가능한 자재 데이터 정보를 반환할 때 사용
 * 본사는 더미 데이터, 협력사는 할당받은 자재 데이터 제공
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaterialDataResponse {

    // ========================================================================
    // 기본 자재 정보 (Basic Material Information)
    // ========================================================================

    private String materialCode; // 자재코드 (ST001, AL200, PL300 등)
    private String materialName; // 자재명 (냉간압연강판, 알루미늄합금 등)
    private String materialCategory; // 카테고리 (강재, 알루미늄, 플라스틱 등)
    private String materialSubCategory; // 세부카테고리 (차체용, 엔진용 등)
    private String materialDescription; // 자재 상세 설명
    private String materialSpec; // 자재 스펙 정보

    // ========================================================================
    // ESG 배출량 정보 (ESG Emission Information)
    // ========================================================================

    private BigDecimal emissionFactor; // 배출계수 (tCO2eq/ton)
    private String unit; // 단위 (ton, kg, 개, m³ 등)
    private String scopeCategory; // 적용 Scope 카테고리 (SCOPE1, SCOPE2, SCOPE3)
    private Integer scope3CategoryNumber; // Scope 3 세부 카테고리 번호 (1-15)
    
    // ========================================================================
    // 접근 권한 정보 (Access Permission Information)
    // ========================================================================

    private AccessType accessType; // 접근 권한 타입
    private AssignmentSource assignmentSource; // 할당 출처
    private String assignedBy; // 할당자 정보
    private String assignmentReason; // 할당 사유

    // ========================================================================
    // 사용 현황 정보 (Usage Status Information)
    // ========================================================================

    private Boolean isAssigned; // 할당 여부
    private Boolean isUsed; // 사용 중 여부 (Scope 계산에 사용됨)
    private Integer usageCount; // 사용 횟수
    private String lastUsedDate; // 마지막 사용일

    // ========================================================================
    // 공급업체 정보 (Supplier Information)
    // ========================================================================

    private String supplierInfo; // 공급업체 정보
    private String qualityGrade; // 품질 등급 (A, B, C등급)
    private Boolean isEcoFriendly; // 친환경 소재 여부

    /**
     * 접근 권한 타입 열거형
     */
    public enum AccessType {
        READ_only("READ_ONLY", "읽기 전용"),
        modifiable("MODIFIABLE", "수정 가능"),
        full_access("FULL_ACCESS", "전체 접근");

        private final String code;
        private final String description;

        AccessType(String code, String description) {
            this.code = code;
            this.description = description;
        }

        public String getCode() { return code; }
        public String getDescription() { return description; }
    }

    /**
     * 할당 출처 열거형
     */
    public enum AssignmentSource {
        headquarters("HEADQUARTERS", "본사 직접 할당"),
        parent_partner("PARENT_PARTNER", "상위 협력사 할당"),
        self_managed("SELF_MANAGED", "자체 관리"),
        dummy_data("DUMMY_DATA", "더미 데이터");

        private final String code;
        private final String description;

        AssignmentSource(String code, String description) {
            this.code = code;
            this.description = description;
        }

        public String getCode() { return code; }
        public String getDescription() { return description; }
    }

    // ========================================================================
    // 편의 메서드 (Convenience Methods)
    // ========================================================================

    /**
     * 자재 정보 요약 문자열 반환
     */
    public String getMaterialSummary() {
        return String.format("%s (%s) - %s", materialName, materialCode, materialCategory);
    }

    /**
     * 배출량 정보 문자열 반환
     */
    public String getEmissionInfo() {
        return String.format("%.4f tCO2eq/%s", emissionFactor, unit);
    }

    /**
     * 접근 권한 정보 문자열 반환
     */
    public String getPermissionInfo() {
        return String.format("%s (%s)", accessType.getDescription(), assignmentSource.getDescription());
    }

    /**
     * 수정 가능 여부 확인
     */
    public boolean isModifiable() {
        return accessType == AccessType.modifiable || accessType == AccessType.full_access;
    }

    /**
     * 본사 데이터 여부 확인
     */
    public boolean isHeadquartersData() {
        return assignmentSource == AssignmentSource.headquarters || assignmentSource == AssignmentSource.dummy_data;
    }
}