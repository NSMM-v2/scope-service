package com.nsmm.esg.scope_service.entity;

import com.nsmm.esg.scope_service.enums.*;
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
 * 통합 Scope 배출량 엔티티 - 프론트엔드 연동 기반
 *
 * 특징:
 * - 프론트엔드 폼 구조와 1:1 매핑
 * - Scope 1(10개), Scope 2(2개), Scope 3(15개) 카테고리 지원
 * - 제품 코드 매핑 지원
 * - 수동 입력 모드 지원
 * - 계층적 권한 관리
 */
@Entity
@Table(name = "scope_emission", indexes = {
        @Index(name = "idx_scope_year_month", columnList = "headquarters_id, reporting_year, reporting_month"),
        @Index(name = "idx_scope_category", columnList = "scope_type, scope1_category_number, scope2_category_number, scope3_category_number"),
        @Index(name = "idx_tree_path", columnList = "tree_path"),
        @Index(name = "idx_partner_scope", columnList = "partner_id, scope_type, reporting_year, reporting_month"),
        @Index(name = "idx_scope_reporting", columnList = "scope_type, reporting_year, reporting_month"),
        @Index(name = "idx_material_mapping", columnList = "material_mapping_id"),
        @Index(name = "idx_material_assignment", columnList = "material_assignment_id")
})
@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class ScopeEmission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ========================================================================
    // 권한 제어 및 조직 정보 (Authority & Organization)
    // ========================================================================

    @Column(name = "headquarters_id", nullable = false)
    private Long headquartersId; // 본사 ID

    @Column(name = "partner_id")
    private Long partnerId; // 협력사 ID (본사인 경우 null)

    @Column(name = "tree_path", nullable = false, length = 500)
    private String treePath; // 계층 경로 - 권한 제어용 (/1/L1-001/L2-003/)

    // ========================================================================
    // 보고 기간 정보 (Reporting Period)
    // ========================================================================

    @Column(name = "reporting_year", nullable = false)
    private Integer reportingYear; // 보고 연도

    @Column(name = "reporting_month", nullable = false)
    private Integer reportingMonth; // 보고 월

    // ========================================================================
    // Scope 분류 및 카테고리 정보 (Scope Classification & Category)
    // ========================================================================

    @Enumerated(EnumType.STRING)
    @Column(name = "scope_type", nullable = false)
    private ScopeType scopeType; // SCOPE1, SCOPE2, SCOPE3

    // Scope 1 카테고리 (프론트엔드 list1-10 매핑)
    @Column(name = "scope1_category_number")
    private Integer scope1CategoryNumber; // 1-10 (list1-10)

    @Column(name = "scope1_category_name")
    private String scope1CategoryName; // 카테고리명

    @Column(name = "scope1_category_group")
    private String scope1CategoryGroup; // 그룹명 (고정연소, 이동연소, 공정배출, 냉매누출)

    // Scope 2 카테고리 (프론트엔드 list1-2 매핑)
    @Column(name = "scope2_category_number")
    private Integer scope2CategoryNumber; // 1-2 (list1-2)

    @Column(name = "scope2_category_name")
    private String scope2CategoryName; // 카테고리명

    // Scope 3 카테고리 (프론트엔드 list1-15 매핑)
    @Column(name = "scope3_category_number")
    private Integer scope3CategoryNumber; // 1-15 (list1-15)

    @Column(name = "scope3_category_name")
    private String scope3CategoryName; // 카테고리명

    // ========================================================================
    // 자재코드 매핑 관계 (Material Code Mapping Relations)
    // ========================================================================
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "material_mapping_id", foreignKey = @ForeignKey(name = "fk_scope_material_mapping"))
    private MaterialMapping materialMapping; // 자재코드 매핑 정보
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "material_assignment_id", foreignKey = @ForeignKey(name = "fk_scope_material_assignment"))
    private MaterialAssignment materialAssignment; // 자재코드 할당 정보

    // ========================================================================
    // 프론트엔드 입력 데이터 (Frontend Input Data)
    // ========================================================================

    @Column(name = "major_category", nullable = false, length = 100)
    private String majorCategory; // 대분류

    @Column(name = "subcategory", nullable = false, length = 100)
    private String subcategory; // 구분

    @Column(name = "raw_material", nullable = false, length = 100)
    private String rawMaterial; // 원료/에너지

    @Column(name = "activity_amount", nullable = false, precision = 15, scale = 3)
    private BigDecimal activityAmount; // 수량 (quantity)

    @Column(name = "unit", nullable = false, length = 20)
    private String unit; // 단위

    @Column(name = "emission_factor", nullable = false, precision = 15, scale = 6)
    private BigDecimal emissionFactor; // 배출계수 (kgCO2eq)

    @Column(name = "total_emission", nullable = false, precision = 15, scale = 6)
    private BigDecimal totalEmission; // 총 배출량 (계산 결과)

    // ========================================================================
    // 입력 모드 제어 (Input Mode Control)
    // ========================================================================

    @Column(name = "input_type", nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private InputType inputType = InputType.MANUAL; // MANUAL, LCA

    @Column(name = "has_product_mapping", nullable = false)
    @Builder.Default
    private Boolean hasProductMapping = false; // 제품 코드 매핑 여부

    @Column(name = "factory_enabled", nullable = false)
    @Builder.Default
    private Boolean factoryEnabled = false; // 공장 설비 활성화 여부

    // ========================================================================
    // 감사 필드 (Audit Fields)
    // ========================================================================

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ========================================================================
    // 비즈니스 로직 메서드 (Business Logic Methods)
    // ========================================================================
    
    /**
     * 자재코드 매핑 정보 조회
     */
    public String getUpstreamMaterialCode() {
        return materialMapping != null ? materialMapping.getUpstreamMaterialCode() : null;
    }
    
    /**
     * 내부 자재코드 조회
     */
    public String getInternalMaterialCode() {
        return materialMapping != null ? materialMapping.getInternalMaterialCode() : null;
    }
    
    /**
     * 자재명 조회
     */
    public String getMaterialName() {
        return materialMapping != null ? materialMapping.getMaterialName() : null;
    }
    
    /**
     * 자재코드 매핑 여부 확인
     */
    public boolean hasMaterialMapping() {
        return materialMapping != null && hasProductMapping;
    }
    
    /**
     * 자재코드 매핑 설정
     */
    public ScopeEmission setMaterialMapping(MaterialMapping mapping) {
        return this.toBuilder()
                .materialMapping(mapping)
                .hasProductMapping(mapping != null)
                .build();
    }
    
    /**
     * 자재코드 할당 설정
     */
    public ScopeEmission setMaterialAssignment(MaterialAssignment assignment) {
        return this.toBuilder()
                .materialAssignment(assignment)
                .build();
    }
    
    /**
     * 체인 추적 정보 생성
     */
    public String getTrackingInfo() {
        if (!hasMaterialMapping()) {
            return "직접 입력 데이터";
        }
        
        String upstream = getUpstreamMaterialCode();
        String internal = getInternalMaterialCode();
        Long upstreamPartnerId = getUpstreamPartnerId();
        
        String upstreamInfo = upstreamPartnerId == null ? "본사" : "Partner-" + upstreamPartnerId;
        return String.format("%s[%s] → %s[Partner-%d]", upstream, upstreamInfo, internal, partnerId);
    }
    
    /**
     * 수정 가능성 검증
     */
    public boolean isModifiable() {
        // 하위에 할당된 자재코드가 있으면 수정 불가
        if (materialMapping != null && materialMapping.getHasDownstreamAssignment()) {
            return false;
        }
        return true;
    }
    
    /**
     * 삭제 가능성 검증
     */
    public boolean isDeletable() {
        // 하위에 할당된 자재코드가 있으면 삭제 불가
        if (materialMapping != null && materialMapping.getHasDownstreamAssignment()) {
            return false;
        }
        return true;
    }
    
    /**
     * 상위 협력사 ID 조회
     */
    public Long getUpstreamPartnerId() {
        return materialMapping != null ? materialMapping.getUpstreamPartnerId() : null;
    }

    /**
     * 프론트엔드 입력 데이터 검증
     */
    @PrePersist
    @PreUpdate
    private void validateInputData() {
        // 배출량 계산 검증
        if (activityAmount != null && emissionFactor != null) {
            BigDecimal calculated = activityAmount.multiply(emissionFactor);
            if (totalEmission.compareTo(calculated) != 0) {
                throw new IllegalStateException("배출량 계산이 일치하지 않습니다");
            }
        }

        // 카테고리 일치성 검증
        if (scopeType == ScopeType.SCOPE1 && scope1CategoryNumber != null) {
            Scope1Category category = Scope1Category.fromCategoryNumber(scope1CategoryNumber);
            if (!category.getCategoryName().equals(scope1CategoryName)) {
                throw new IllegalStateException("Scope 1 카테고리 정보가 일치하지 않습니다");
            }
        }
        
        // 자재코드 매핑 일관성 검증
        if (materialMapping != null && !hasProductMapping) {
            throw new IllegalStateException("자재코드 매핑이 있지만 매핑 플래그가 false입니다");
        }
        
        if (hasProductMapping && materialMapping == null) {
            throw new IllegalStateException("매핑 플래그가 true이지만 자재코드 매핑이 없습니다");
        }
        
        // Scope 1, 2에서만 자재코드 매핑 지원 검증
        if (materialMapping != null && scopeType == ScopeType.SCOPE3) {
            throw new IllegalStateException("Scope 3에서는 자재코드 매핑을 지원하지 않습니다");
        }
    }
}