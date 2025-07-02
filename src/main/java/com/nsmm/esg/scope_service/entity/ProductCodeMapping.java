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
 * 제품 코드 매핑 엔티티
 *
 * 특징:
 * - 본사 제품 코드와 각 협력사별 제품 코드 매핑
 * - 보고 연도/월별로 관리
 * - 계층 구조(부품/원재료) 및 변환 비율 지원
 * - 대시보드에서 본사 제품 코드 기준으로 매핑된 협력사 데이터만 집계 가능
 * - 생성일/수정일만 감사 필드로 유지
 */
@Entity
@Table(name = "product_code_mapping", indexes = {
        @Index(name = "idx_headquarters_product_year_month", columnList = "headquarters_id, headquarters_product_code, reporting_year, reporting_month"),
        @Index(name = "idx_company_product_year_month", columnList = "headquarters_id, partner_id, company_product_code, reporting_year, reporting_month"),
        @Index(name = "idx_mapping_active", columnList = "headquarters_id, is_active, reporting_year, reporting_month"),
        @Index(name = "idx_parent_mapping", columnList = "parent_mapping_id")
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

    @Column(name = "headquarters_id", nullable = false)
    private Long headquartersId;

    @Column(name = "partner_id")
    private Long partnerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "owner_type", nullable = false)
    private OwnerType ownerType; // HEADQUARTERS, PARTNER

    @Column(name = "reporting_year", nullable = false)
    private Integer reportingYear;

    @Column(name = "reporting_month", nullable = false)
    private Integer reportingMonth;

    @Column(name = "headquarters_product_code", nullable = false, length = 50)
    private String headquartersProductCode;

    @Column(name = "company_product_code", nullable = false, length = 50)
    private String companyProductCode;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(name = "product_description")
    private String productDescription;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_mapping_id")
    private ProductCodeMapping parentMapping;

    @Column(name = "product_hierarchy_path", nullable = false, length = 500)
    private String productHierarchyPath;

    @Column(name = "hierarchy_level", nullable = false)
    @Builder.Default
    private Integer hierarchyLevel = 1;

    @Enumerated(EnumType.STRING)
    @Column(name = "mapping_type", nullable = false)
    private MappingType mappingType; // DIRECT, COMPONENT, RAW_MATERIAL

    @Column(name = "conversion_ratio", precision = 10, scale = 6)
    @Builder.Default
    private BigDecimal conversionRatio = BigDecimal.ONE;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum OwnerType {
        HEADQUARTERS, PARTNER
    }
    public enum MappingType {
        DIRECT, COMPONENT, RAW_MATERIAL
    }
}