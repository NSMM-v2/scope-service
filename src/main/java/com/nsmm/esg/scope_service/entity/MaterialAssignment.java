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
 * 자재코드 할당 엔티티
 * 상위 협력사가 하위 협력사에게 자재코드 할당 관리
 */
@Entity
@Table(name = "material_assignment", indexes = {
        @Index(name = "idx_to_partner", columnList = "to_partner_id, is_active"),
        @Index(name = "idx_headquarters_material", columnList = "headquarters_id, material_code"),
        @Index(name = "idx_from_partner", columnList = "from_partner_id")
})
@Getter
@Builder
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

    @Column(name = "from_partner_id")
    private Long fromPartnerId; // 할당하는 협력사 ID (null이면 본사)

    @Column(name = "to_partner_id", nullable = false)
    private Long toPartnerId; // 할당받는 협력사 ID

    // ========================================================================
    // 자재코드 정보 (Material Code Information)
    // ========================================================================

    @Column(name = "material_code", nullable = false, length = 50)
    private String materialCode; // 자재코드 (A001, B100, C200...)

    @Column(name = "material_name", nullable = false, length = 200)
    private String materialName; // 자재명 (부품, 철강, 원료...)

    @Column(name = "material_category", length = 100)
    private String materialCategory; // 카테고리 (제조업, 원재료...)

    @Column(name = "material_spec", length = 500)
    private String materialSpec; // 자재 스펙 설명

    // ========================================================================
    // 메타 정보 (Meta Information)
    // ========================================================================

    @Column(name = "assigned_by", length = 100)
    private String assignedBy; // 할당한 사용자

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}