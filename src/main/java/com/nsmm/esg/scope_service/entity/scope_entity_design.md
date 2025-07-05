# ESG Scope 통합 엔티티 설계 문서 (프론트엔드 연동 기반)

## 1. 프로젝트 개요

### 1.1 설계 목표

- **통합 관리**: Scope 1, 2, 3 배출량 데이터를 하나의 테이블로 통합 관리
- **프론트엔드 연동**: 실제 프론트엔드 폼 구조와 1:1 매핑되는 엔티티 설계
- **사용자 직접 입력**: 프론트엔드에서 활동량, 배출계수, 배출량 모두 직접 입력
- **계층적 집계**: 하청업체 → 상위 조직 → 본사로 자동 집계 지원
- **카테고리별 관리**: 각 Scope별 세부 카테고리 지원
- **권한 제어**: TreePath 기반 계층적 권한 관리
- **제품 코드 매핑**: 대시보드에서 제품별 집계 지원
- **성능 최적화**: 단일 테이블 조회로 성능 향상

### 1.2 프론트엔드 분석 결과

#### 1.2.1 Scope 1 폼 구조
- **카테고리**: 고정연소(list1-3), 이동연소(list4-6), 공정배출(list7-8), 냉매누출(list9-10) - 총 10개
- **입력 필드**: 대분류(majorCategory), 구분(subcategory), 원료/에너지(rawMaterial), 수량(quantity), 단위(unit), 배출계수(kgCO2eq)
- **계산**: 총 배출량 = 수량 × 배출계수 (프론트엔드에서 계산)
- **모드**: 수동 입력 모드와 Excel 업로드 모드 지원

#### 1.2.2 Scope 2 폼 구조
- **카테고리**: 전력(list1), 스팀(list2) - 총 2개
- **입력 필드**: 대분류(majorCategory), 구분(subcategory), 원료/에너지(rawMaterial), 수량(quantity), 단위(unit), 배출계수(kgCO2eq)
- **계산**: 총 배출량 = 수량 × 배출계수 (프론트엔드에서 계산)
- **모드**: 수동 입력 모드와 Excel 업로드 모드 지원

#### 1.2.3 Scope 3 폼 구조
- **카테고리**: 15개 카테고리 (list1-15)
- **입력 필드**: 대분류(majorCategory), 구분(subcategory), 원료/에너지(rawMaterial), 수량(quantity), 단위(unit), 배출계수(kgCO2eq)
- **계산**: 총 배출량 = 수량 × 배출계수 (프론트엔드에서 계산)
- **모드**: 수동 입력 모드와 Excel 업로드 모드 지원

#### 1.2.4 대시보드 제품 코드 요구사항
- **제품 선택**: 협력사별 제품 코드 매핑 지원 (휠-L01, 엔진-L02, 차체-L03)
- **집계 기능**: 제품별 Scope 1, 2, 3 배출량 집계
- **필터링**: 제품 코드 기반 데이터 필터링

### 1.3 API 연동 구조 분석

#### 1.3.1 Scope 3 API 엔드포인트 (실제 구현)
- `GET /api/v1/scope3/emissions/year/{year}/month/{month}` - 연도/월별 전체 조회
- `GET /api/v1/scope3/emissions/summary/year/{year}/month/{month}` - 카테고리별 요약
- `POST /api/v1/scope3/emissions` - 배출량 데이터 생성
- `PUT /api/v1/scope3/emissions/{id}` - 배출량 데이터 수정
- `DELETE /api/v1/scope3/emissions/{id}` - 배출량 데이터 삭제

#### 1.3.2 프론트엔드 요청/응답 데이터 구조
```typescript
// 프론트엔드 요청 데이터 (실제 구조)
interface ScopeEmissionRequest {
  scope3CategoryNumber: number // 1-15
  reportingYear: number
  reportingMonth: number
  majorCategory: string // 대분류
  subcategory: string // 구분
  rawMaterial: string // 원료/에너지
  activityAmount: number // 수량 (quantity)
  unit: string // 단위
  emissionFactor: number // 배출계수 (kgCO2eq)
  totalEmission: number // 계산 결과
  isManualInput: boolean // 수동 입력 여부
}
```

### 1.4 통합 집계 규칙

- **Scope 3 카테고리 1**: 이동연소 + 고정연소 + Scope3-Cat1 + Scope3-Cat4
- **Scope 3 카테고리 2**: 이동연소 + 고정연소 + Scope3-Cat2 + Scope3-Cat4
- **집계 방식**: 최하단 협력사에서 입력한 데이터가 상위 협력사로 집계되고, 최종적으로 본사에 Scope 3까지 포함하여 집계됨

### 1.5 제품 코드 매핑 요구사항

- **Scope 1, 2, 3 카테고리**: 수동 입력 및 LCA 입력을 지원하며, 제품명과 제품코드를 입력받아 각 회사별로 다른 제품코드를 매핑
- **제품 코드 매핑**: 각 회사는 제품명을 통해 각기 다른 제품코드를 입력하며, 이를 통해 회사별로 다른 제품코드를 매핑할 수 있음
- **대시보드 연동**: 본사 제품 코드 기준으로 협력사 데이터 집계

## 2. 엔티티 설계

### 2.1 메인 데이터 테이블 - ScopeEmission 엔티티

```java
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
    @Index(name = "idx_product_code", columnList = "headquarters_id, company_product_code, reporting_year, reporting_month"),
    @Index(name = "idx_tree_path", columnList = "tree_path"),
    @Index(name = "idx_aggregation", columnList = "is_aggregated, aggregation_level"),
    @Index(name = "idx_partner_scope", columnList = "partner_id, scope_type, reporting_year, reporting_month")
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
    // 제품 코드 매핑 정보 (Product Code Mapping)
    // ========================================================================
    
    @Column(name = "company_product_code", length = 50)
    private String companyProductCode; // 각 회사별 제품 코드 (L01, L02, L03 등)

    @Column(name = "product_name", length = 100)
    private String productName; // 제품명 (휠, 엔진, 차체 등)

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
    // 입력 모드 및 집계 제어 (Input Mode & Aggregation Control)
    // ========================================================================
    
    @Column(name = "is_manual_input", nullable = false)
    @Builder.Default
    private Boolean isManualInput = true; // 수동 입력 여부

    @Column(name = "is_direct_input", nullable = false)
    @Builder.Default
    private Boolean isDirectInput = true; // 직접 입력 여부

    @Column(name = "is_aggregated", nullable = false)
    @Builder.Default
    private Boolean isAggregated = false; // 집계 데이터 여부

    @Column(name = "aggregation_level", nullable = false)
    @Builder.Default
    private Integer aggregationLevel = 0; // 집계 레벨

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
     * Scope 1 카테고리 설정
     */
    public ScopeEmission withScope1Category(Scope1Category category) {
        return this.toBuilder()
            .scope1CategoryNumber(category.getCategoryNumber())
            .scope1CategoryName(category.getCategoryName())
            .scope1CategoryGroup(category.getGroupName())
            .build();
    }

    /**
     * Scope 2 카테고리 설정
     */
    public ScopeEmission withScope2Category(Scope2Category category) {
        return this.toBuilder()
            .scope2CategoryNumber(category.getCategoryNumber())
            .scope2CategoryName(category.getCategoryName())
            .build();
    }

    /**
     * Scope 3 카테고리 설정
     */
    public ScopeEmission withScope3Category(Scope3Category category) {
        return this.toBuilder()
            .scope3CategoryNumber(category.getCategoryNumber())
            .scope3CategoryName(category.getCategoryName())
            .build();
    }

    /**
     * 제품 코드 매핑 설정
     */
    public ScopeEmission withProductCode(String companyProductCode, String productName) {
        return this.toBuilder()
            .companyProductCode(companyProductCode)
            .productName(productName)
            .build();
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
    }
}
```

### 2.2 제품 코드 매핑 테이블 - ProductCodeMapping 엔티티

```java
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
```

### 2.3 대시보드 연동 예시

```java
// 대시보드 집계 예시 (2024년 12월, 본사 제품코드 W250)
List<ProductCodeMapping> mappings = productCodeMappingRepository.findActiveMappings(
    headquartersId, "W250", 2024, 12);

List<ScopeEmission> emissions = scopeEmissionRepository.findByCompanyProductCodes(
    mappings.stream().map(ProductCodeMapping::getCompanyProductCode).toList(),
    2024, 12
);

// 매핑된 데이터만 집계
BigDecimal total = emissions.stream()
    .map(ScopeEmission::getTotalEmission)
    .reduce(BigDecimal.ZERO, BigDecimal::add);
```
```

이제 요구사항과 엔티티 설계 부분이 프론트엔드 구조와 일치하도록 수정되었습니다. 주요 변경사항은:

1. **프론트엔드 분석 결과 추가**: 실제 폼 구조와 API 연동 구조 반영
2. **카테고리 수정**: Scope 1(10개), Scope 2(2개), Scope 3(15개)로 정확히 매핑
3. **필드명 일치**: 프론트엔드에서 사용하는 필드명과 백엔드 엔티티 필드명 매핑
4. **제품 코드 구체화**: 대시보드에서 사용하는 제품 코드 예시 추가
5. **입력 모드 지원**: `isManualInput` 필드 추가로 수동/자동 입력 모드 구분