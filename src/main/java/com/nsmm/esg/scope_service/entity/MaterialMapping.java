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
        @Index(name = "idx_downstream_tracking", columnList = "has_downstream_assignment, downstream_assignment_count"),
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

    @Column(name = "partner_id")
    private Long partnerId; // 매핑을 생성한 협력사 ID

    @Column(name = "partner_level", nullable = false)
    private Integer partnerLevel; // 협력사 레벨 (1차, 2차, 3차...)

    @Column(name = "tree_path", length = 500)
    private String treePath; // 계층 경로

    // ========================================================================
    // 매핑 정보 (Mapping Information)
    // ========================================================================

    @Column(name = "upstream_material_code", length = 50)
    private String upstreamMaterialCode; // 상위에서 할당받은 자재코드 (A100, FE100...) - 최상위인 경우 null

    @Column(name = "internal_material_code", nullable = false, length = 50)
    private String internalMaterialCode; // 내부 자재코드 (B100, FE200...)

    @Column(name = "material_name", length = 200)
    private String materialName; // 자재명

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
    // 메타 정보 (Meta Information)
    // ========================================================================

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
     * 총 배출량 조회
     */
    public BigDecimal getTotalEmission() {
        return scopeEmissions != null ? 
            scopeEmissions.stream()
                .filter(emission -> emission.getTotalEmission() != null)
                .map(ScopeEmission::getTotalEmission)
                .reduce(BigDecimal.ZERO, BigDecimal::add) : BigDecimal.ZERO;
    }


}