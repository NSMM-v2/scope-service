package com.nsmm.esg.scope_service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 자재코드 매핑 엔티티
 * Scope 입력 시 상위 자재코드와 내부 자재코드 매핑 관리
 */
@Entity
@Table(name = "material_mapping", indexes = {
        @Index(name = "idx_partner_mapping", columnList = "partner_id, is_active"),
        @Index(name = "idx_upstream_code", columnList = "upstream_material_code"),
        @Index(name = "idx_internal_code", columnList = "internal_material_code"),
        @Index(name = "idx_scope_emission", columnList = "scope_emission_id")
})
@Getter
@Builder
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

    @Column(name = "tree_path", length = 500)
    private String treePath; // 계층 경로

    // ========================================================================
    // 매핑 정보 (Mapping Information)
    // ========================================================================

    @Column(name = "upstream_material_code", nullable = false, length = 50)
    private String upstreamMaterialCode; // 상위에서 할당받은 자재코드 (A001, B100...)

    @Column(name = "internal_material_code", nullable = false, length = 50)
    private String internalMaterialCode; // 내부 자재코드 (B001, C100...)

    @Column(name = "material_name", length = 200)
    private String materialName; // 자재명

    // ========================================================================
    // 연결 정보 (Relation Information)
    // ========================================================================

    @Column(name = "scope_emission_id")
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
    // 비즈니스 로직 메서드 (Business Logic Methods)
    // ========================================================================

    /**
     * 매핑 관계를 문자열로 표현
     */
    public String getMappingChain() {
        return upstreamMaterialCode + " → " + internalMaterialCode;
    }
}