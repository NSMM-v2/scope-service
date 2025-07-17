package com.nsmm.esg.scope_service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 자재코드 할당 엔티티
 * 상위 협력사가 하위 협력사에게 자재코드 할당 관리
 */
@Entity
@Table(name = "material_assignment", indexes = {
        @Index(name = "idx_to_partner", columnList = "to_partner_id, is_active"),
        @Index(name = "idx_headquarters_material", columnList = "headquarters_id, material_code"),
        @Index(name = "idx_from_partner", columnList = "from_partner_id"),
        @Index(name = "idx_material_code_unique", columnList = "material_code, is_active"),
        @Index(name = "idx_headquarters_partner_chain", columnList = "headquarters_id, from_partner_id, to_partner_id"),
        @Index(name = "idx_level_active", columnList = "to_level, is_active, is_mapped")
})
@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class MaterialAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ========================================================================
    // 조직 정보 (Organization Information)
    // ========================================================================

    @Column(name = "headquarters_id", nullable = false)
    private Long headquartersId; // 본사 ID

    @Column(name = "from_partner_id", length = 36)
    private String fromPartnerId; // 할당하는 협력사 UUID (null이면 본사)

    @Column(name = "to_partner_id", nullable = false, length = 36)
    private String toPartnerId; // 할당받는 협력사 UUID

    @Column(name = "from_level")
    private Integer fromLevel; // 할당하는 협력사 레벨 (0: 본사, 1: 1차사...)

    @Column(name = "to_level", nullable = false)
    private Integer toLevel; // 할당받는 협력사 레벨

    // ========================================================================
    // 자재코드 정보 (Material Code Information)
    // ========================================================================

    @Column(name = "material_code", nullable = false, length = 50)
    private String materialCode; // 자재코드 (A100, FE100, GH100...)

    @Column(name = "material_name", nullable = false, length = 200)
    private String materialName; // 자재명 (부품, 철강, 원료...)

    @Column(name = "material_category", length = 100)
    private String materialCategory; // 카테고리 (제조업, 원재료...)
    
    @Column(name = "material_description", length = 1000)
    private String materialDescription; // 자재 상세 설명

    // ========================================================================
    // 메타 정보 (Meta Information)
    // ========================================================================

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true; // 활성 상태

    @Column(name = "is_mapped")
    @Builder.Default
    private Boolean isMapped = false; // 매핑 생성 여부

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // ========================================================================
    // JPA 관계 설정 (JPA Relations)
    // ========================================================================
    
    @OneToMany(mappedBy = "materialAssignment", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<MaterialMapping> materialMappings = new ArrayList<>(); // 연결된 매핑들
    
    @OneToMany(mappedBy = "materialAssignment", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ScopeEmission> scopeEmissions = new ArrayList<>(); // 연결된 배출량 데이터들

    // ========================================================================
    // 비즈니스 로직 메서드 (Business Logic Methods)
    // ========================================================================

    /**
     * 매핑 생성 상태로 변경
     */
    public MaterialAssignment markAsMapped() {
        return this.toBuilder()
                .isMapped(true)
                .build();
    }

    /**
     * 수정 가능 여부 확인
     */
    public boolean isModifiable() {
        return !this.isMapped && this.isActive;
    }

    /**
     * 삭제 가능 여부 확인
     */
    public boolean isDeletable() {
        return !this.isMapped && this.isActive;
    }

    /**
     * 할당 관계 문자열 표현
     */
    public String getAssignmentInfo() {
        String fromName = fromPartnerId == null ? "본사" : "협력사(" + fromPartnerId + ")";
        return fromName + " → 협력사(" + toPartnerId + ") : " + materialCode;
    }
    
    /**
     * 매핑 개수 조회
     */
    public int getMappingCount() {
        return materialMappings != null ? materialMappings.size() : 0;
    }
    
    /**
     * 활성 매핑 개수 조회
     */
    public long getActiveMappingCount() {
        return materialMappings != null ? 
            materialMappings.stream().filter(mapping -> mapping.getIsActive() && !mapping.getIsDeleted()).count() : 0;
    }
    
    /**
     * 매핑 추가
     */
    public void addMapping(MaterialMapping mapping) {
        if (materialMappings == null) {
            materialMappings = new ArrayList<>();
        }
        materialMappings.add(mapping);
        // 매핑이 생성되면 자동으로 매핑됨 상태로 변경
        if (!this.isMapped) {
            this.isMapped = true;
        }
    }
    
    /**
     * 배출량 데이터 추가
     */
    public void addScopeEmission(ScopeEmission emission) {
        if (scopeEmissions == null) {
            scopeEmissions = new ArrayList<>();
        }
        scopeEmissions.add(emission);
    }
    
    /**
     * 총 배출량 계산
     */
    public BigDecimal getTotalEmission() {
        return scopeEmissions != null ? 
            scopeEmissions.stream()
                .filter(emission -> emission.getTotalEmission() != null)
                .map(ScopeEmission::getTotalEmission)
                .reduce(BigDecimal.ZERO, BigDecimal::add) : BigDecimal.ZERO;
    }
    
    /**
     * 데이터 무결성 검증
     */
    public boolean validateIntegrity() {
        // 활성 상태이지만 매핑이 없는 경우 검증
        if (isActive && !isMapped && getMappingCount() > 0) {
            return false; // 매핑이 있는데 플래그가 false
        }
        
        // 매핑됨으로 표시되었지만 실제 매핑이 없는 경우
        if (isMapped && getActiveMappingCount() == 0) {
            return false; // 플래그는 true인데 활성 매핑이 없음
        }
        
        return true;
    }
    
    /**
     * 할당 요약 정보
     */
    public String getSummaryInfo() {
        return String.format(
            "자재코드: %s | 대상: %s차사 | 매핑수: %d | 총배출량: %s tCO2eq", 
            materialCode, 
            toLevel,
            getActiveMappingCount(),
            getTotalEmission().toString()
        );
    }
}