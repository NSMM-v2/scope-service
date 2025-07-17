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
 * 자재코드 매핑 엔티티
 * Scope 입력 시 상위 자재코드와 내부 자재코드 매핑 관리
 */
@Entity
@Table(name = "material_mapping", indexes = {
        @Index(name = "idx_partner_mapping", columnList = "partner_id, is_active"),
        @Index(name = "idx_upstream_code", columnList = "upstream_material_code"),
        @Index(name = "idx_internal_code", columnList = "internal_material_code"),
        @Index(name = "idx_scope_emission", columnList = "scope_emission_id"),
        @Index(name = "idx_assignment_link", columnList = "material_assignment_id"),
        @Index(name = "idx_mapping_chain", columnList = "headquarters_id, upstream_material_code, internal_material_code"),
        @Index(name = "idx_partner_level_active", columnList = "partner_id, partner_level, is_active"),
        @Index(name = "idx_downstream_tracking", columnList = "has_downstream_assignment, downstream_assignment_count"),
        @Index(name = "idx_tree_path_active", columnList = "tree_path, is_active, is_deleted")
})
@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class MaterialMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ========================================================================
    // 조직 정보 (Organization Information)
    // ========================================================================

    @Column(name = "headquarters_id", nullable = false)
    private Long headquartersId; // 본사 ID

    @Column(name = "partner_id", nullable = false)
    private Long partnerId; // 매핑을 생성한 협력사 ID

    @Column(name = "partner_level", nullable = false)
    private Integer partnerLevel; // 협력사 레벨 (1차, 2차, 3차...)

    @Column(name = "tree_path", length = 500)
    private String treePath; // 계층 경로

    // ========================================================================
    // 매핑 정보 (Mapping Information)
    // ========================================================================

    @Column(name = "upstream_material_code", nullable = false, length = 50)
    private String upstreamMaterialCode; // 상위에서 할당받은 자재코드 (A100, FE100...)

    @Column(name = "internal_material_code", nullable = false, length = 50)
    private String internalMaterialCode; // 내부 자재코드 (B100, FE200...)

    @Column(name = "material_name", length = 200)
    private String materialName; // 자재명
    
    @Column(name = "material_description", length = 1000)
    private String materialDescription; // 자재 상세 설명

    @Column(name = "upstream_partner_id")
    private Long upstreamPartnerId; // 상위 협력사 ID (null이면 본사)

    // ========================================================================
    // 연결 정보 (Relation Information)
    // ========================================================================

    @Column(name = "material_assignment_id", insertable = false, updatable = false)
    private Long materialAssignmentId; // 연결된 할당 정보 ID

    @Column(name = "scope_emission_id", nullable = false)
    private Long scopeEmissionId; // 연결된 배출량 데이터 ID

    // ========================================================================
    // 하위 할당 추적 (Downstream Tracking)
    // ========================================================================

    @Column(name = "has_downstream_assignment")
    @Builder.Default
    private Boolean hasDownstreamAssignment = false; // 하위에 할당했는지 여부

    @Column(name = "downstream_assignment_count")
    @Builder.Default
    private Integer downstreamAssignmentCount = 0; // 하위 할당 개수

    // ========================================================================
    // 메타 정보 (Meta Information)
    // ========================================================================

    @Column(name = "mapping_description", length = 500)
    private String mappingDescription; // 매핑 설명

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true; // 활성 상태

    @Column(name = "is_deleted")
    @Builder.Default
    private Boolean isDeleted = false; // 소프트 삭제 여부

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // ========================================================================
    // JPA 관계 설정 (JPA Relations)
    // ========================================================================
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "material_assignment_id", foreignKey = @ForeignKey(name = "fk_mapping_assignment"))
    private MaterialAssignment materialAssignment; // 연결된 할당 정보
    
    @OneToMany(mappedBy = "materialMapping", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ScopeEmission> scopeEmissions = new ArrayList<>(); // 연결된 배출량 데이터들

    // ========================================================================
    // 비즈니스 로직 메서드 (Business Logic Methods)
    // ========================================================================

    /**
     * 매핑 관계를 문자열로 표현
     */
    public String getMappingChain() {
        return upstreamMaterialCode + " → " + internalMaterialCode;
    }

    /**
     * 하위 할당 추가
     */
    public MaterialMapping addDownstreamAssignment() {
        return this.toBuilder()
                .hasDownstreamAssignment(true)
                .downstreamAssignmentCount(this.downstreamAssignmentCount + 1)
                .build();
    }

    /**
     * 하위 할당 제거
     */
    public MaterialMapping removeDownstreamAssignment() {
        int newCount = Math.max(0, this.downstreamAssignmentCount - 1);
        return this.toBuilder()
                .hasDownstreamAssignment(newCount > 0)
                .downstreamAssignmentCount(newCount)
                .build();
    }

    /**
     * 삭제 가능 여부 확인
     */
    public boolean isDeletable() {
        return !this.hasDownstreamAssignment && this.isActive && !this.isDeleted;
    }

    /**
     * 소프트 삭제 처리
     */
    public MaterialMapping softDelete() {
        return this.toBuilder()
                .isDeleted(true)
                .isActive(false)
                .build();
    }

    /**
     * 전체 매핑 체인 ID 생성 (추적용)
     */
    public String getChainId() {
        return headquartersId + "_" + upstreamMaterialCode + "_" + internalMaterialCode;
    }
    
    /**
     * 할당 정보에서 매핑 제거
     */
    public void removeFromAssignment() {
        if (materialAssignment != null) {
            materialAssignment.getMaterialMappings().remove(this);
            this.materialAssignment = null;
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
        // 배출량 ID 업데이트 (최신 데이터로)
        if (emission != null && emission.getId() != null) {
            this.scopeEmissionId = emission.getId();
        }
    }
    
    /**
     * 총 배출량 조회
     */
    public BigDecimal getTotalEmission() {
        return scopeEmissions != null ? 
            scopeEmissions.stream()
                .filter(emission -> emission.getTotalEmission() != null)
                .map(ScopeEmission::getTotalEmission)
                .reduce(BigDecimal.ZERO, BigDecimal::add) : BigDecimal.ZERO;
    }
    
    /**
     * 배출량 데이터 개수 조회
     */
    public int getEmissionCount() {
        return scopeEmissions != null ? scopeEmissions.size() : 0;
    }
    
    /**
     * 매핑 상태 정보 문자열
     */
    public String getStatusInfo() {
        String status = isActive ? "활성" : "비활성";
        if (isDeleted) status = "삭제됨";
        return String.format("%s [%s] - %s", getMappingChain(), status, 
                            hasDownstreamAssignment ? "하위할당있음" : "하위할당없음");
    }
    
    /**
     * 할당 정보 연결
     */
    public void setMaterialAssignment(MaterialAssignment assignment) {
        // 기존 관계 제거
        if (this.materialAssignment != null) {
            this.materialAssignment.getMaterialMappings().remove(this);
        }
        
        this.materialAssignment = assignment;
        this.materialAssignmentId = assignment != null ? assignment.getId() : null;
        
        // 새로운 관계 추가
        if (assignment != null && !assignment.getMaterialMappings().contains(this)) {
            assignment.addMapping(this);
        }

    }
}