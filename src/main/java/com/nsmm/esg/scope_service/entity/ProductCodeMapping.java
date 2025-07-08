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

/**
 * 제품 코드 매핑 엔티티 - 대시보드 집계 지원
 *
 * 특징:
 * - 본사 제품 코드와 각 협력사별 제품 코드 매핑
 * - 대시보드에서 제품별 집계 지원
 * - 보고 연도/월별로 관리
 * - 계층 구조 및 변환 비율 지원
 */
@Entity
@Table(name = "product_code_mapping", indexes = {
        @Index(name = "idx_headquarters_product", columnList = "headquarters_id, headquarters_product_code, reporting_year, reporting_month"),
        @Index(name = "idx_company_product", columnList = "headquarters_id, partner_id, company_product_code, reporting_year, reporting_month"),
        @Index(name = "idx_mapping_active", columnList = "headquarters_id, is_active, reporting_year, reporting_month")
})
@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class ProductCodeMapping {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ========================================================================
    // 기본 정보 (Basic Information)
    // ========================================================================

    @Column(name = "headquarters_id", nullable = false)
    private Long headquartersId; // 본사 ID

    @Column(name = "partner_id")
    private Long partnerId; // 협력사 ID

    @Enumerated(EnumType.STRING)
    @Column(name = "owner_type", nullable = false)
    private OwnerType ownerType; // HEADQUARTERS, PARTNER

    @Column(name = "reporting_year", nullable = false)
    private Integer reportingYear; // 보고 연도

    @Column(name = "reporting_month", nullable = false)
    private Integer reportingMonth; // 보고 월

    // ========================================================================
    // 제품 코드 매핑 정보 (Product Code Mapping)
    // ========================================================================

    @Column(name = "headquarters_product_code", nullable = false, length = 50)
    private String headquartersProductCode; // 본사 제품 코드 (W250 등)

    @Column(name = "company_product_code", nullable = false, length = 50)
    private String companyProductCode; // 각 회사별 제품 코드 (L01, L02, L03 등)

    @Column(name = "product_name", nullable = false, length = 100)
    private String productName; // 제품명 (휠, 엔진, 차체 등)

    @Column(name = "product_description", length = 500)
    private String productDescription; // 제품 설명

    // ========================================================================
    // 계층 구조 정보 (Hierarchy Information)
    // ========================================================================

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_mapping_id")
    private ProductCodeMapping parentMapping; // 상위 매핑

    @Column(name = "product_hierarchy_path", nullable = false, length = 500)
    private String productHierarchyPath; // 제품 계층 경로

    @Column(name = "hierarchy_level", nullable = false)
    @Builder.Default
    private Integer hierarchyLevel = 1; // 계층 레벨

    @Enumerated(EnumType.STRING)
    @Column(name = "mapping_type", nullable = false)
    private MappingType mappingType; // DIRECT, COMPONENT, RAW_MATERIAL

    @Column(name = "conversion_ratio", precision = 10, scale = 6)
    @Builder.Default
    private BigDecimal conversionRatio = BigDecimal.ONE; // 변환 비율

    // ========================================================================
    // 상태 정보 (Status Information)
    // ========================================================================

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true; // 활성 상태

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ========================================================================
    // 열거형 정의 (Enum Definitions)
    // ========================================================================

    public enum OwnerType {
        HEADQUARTERS("본사"),
        PARTNER("협력사");

        private final String description;

        OwnerType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    public enum MappingType {
        DIRECT("직접 매핑"),
        COMPONENT("부품 매핑"),
        RAW_MATERIAL("원재료 매핑");

        private final String description;

        MappingType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}