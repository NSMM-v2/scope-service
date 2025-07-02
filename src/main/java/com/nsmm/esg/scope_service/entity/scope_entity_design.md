# ESG Scope 통합 엔티티 설계 문서 (비정규화 단일 테이블)

## 1. 프로젝트 개요

### 1.1 설계 목표

- **통합 관리**: Scope 1, 2, 3 배출량 데이터를 하나의 테이블로 통합 관리
- **사용자 직접 입력**: 프론트엔드에서 활동량, 배출계수, 배출량 모두 직접 입력
- **계층적 집계**: 하청업체 → 상위 조직 → 본사로 자동 집계 지원
- **카테고리별 관리**: 각 Scope별 세부 카테고리 지원
- **권한 제어**: TreePath 기반 계층적 권한 관리
- **성능 최적화**: 단일 테이블 조회로 성능 향상

### 1.2 Scope 카테고리 구조

- **Scope 1 (직접 배출)**: 고정연소, 이동연소, 공정배출, 냉매누출 (4개)
- **Scope 2 (간접 배출)**: 간접배출, 에너지사용, 외부배출 (3개)
- **Scope 3 (기타 간접)**: 구매한 상품 및 서비스 등 15개 카테고리

### 1.3 통합 집계 규칙

- **Scope 3 카테고리 1**: 이동연소 + 고정연소 + Scope3-Cat1 + Scope3-Cat5
- **Scope 3 카테고리 2**: 이동연소 + 고정연소 + Scope3-Cat2 + Scope3-Cat5

### 1.4 비정규화 설계 원칙

- **단순성 우선**: 복잡한 조인 없이 단일 테이블에서 모든 정보 관리
- **성능 중심**: 읽기 성능 최적화를 위한 비정규화 구조
- **사용자 편의성**: 프론트엔드에서 직접 입력하는 모든 값 저장
- **MSA 독립성**: 다른 서비스 의존성 최소화

## 2. 엔티티 설계

### 2.1 메인 데이터 테이블 - ScopeEmission 엔티티 (제품 코드 매핑 지원)

```java
/**
 * 통합 Scope 배출량 엔티티 - 제품 코드 매핑 지원
 *
 * 특징:
 * - 회사별 제품코드(companyProductCode) 필드 추가
 * - ProductCodeMapping 엔티티와 연동(참조)
 * - 보고 연도/월 필수, 생성일/수정일만 감사 필드로 유지
 * - treePath, companyName, created_by, updated_by 등 불필요한 필드 제거
 */
@Entity
@Table(name = "scope_emission")
@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class ScopeEmission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 권한 제어
    @Column(name = "headquarters_id", nullable = false)
    private Long headquartersId;

    @Column(name = "partner_id")
    private Long partnerId;

    @Column(name = "reporting_year", nullable = false)
    private Integer reportingYear;

    @Column(name = "reporting_month", nullable = false)
    private Integer reportingMonth;

    // Scope 분류 및 카테고리
    @Enumerated(EnumType.STRING)
    @Column(name = "scope_type", nullable = false)
    private ScopeType scopeType; // SCOPE1, SCOPE2, SCOPE3

    @Column(name = "scope1_category_number")
    private Integer scope1CategoryNumber; // 1-4

    @Column(name = "scope1_category_name")
    private String scope1CategoryName;

    @Column(name = "scope2_category_number")
    private Integer scope2CategoryNumber; // 1-3

    @Column(name = "scope2_category_name")
    private String scope2CategoryName;

    @Column(name = "scope3_category_number")
    private Integer scope3CategoryNumber; // 1-15

    @Column(name = "scope3_category_name")
    private String scope3CategoryName;

    // 회사별 제품 코드 (매핑)
    @Column(name = "company_product_code", length = 50)
    private String companyProductCode; // 각 회사별 제품 코드 (예: P100, GP100, FE100, W250)

    // 사용자 직접 입력 데이터
    @Column(name = "major_category", nullable = false)
    private String majorCategory;

    @Column(name = "subcategory", nullable = false)
    private String subcategory;

    @Column(name = "raw_material", nullable = false)
    private String rawMaterial;

    @Column(name = "activity_amount", nullable = false, precision = 15, scale = 3)
    private BigDecimal activityAmount;

    @Column(name = "unit", nullable = false, length = 20)
    private String unit;

    @Column(name = "emission_factor", nullable = false, precision = 15, scale = 6)
    private BigDecimal emissionFactor;

    @Column(name = "total_emission", nullable = false, precision = 15, scale = 6)
    private BigDecimal totalEmission;

    // 집계 제어
    @Column(name = "is_direct_input", nullable = false)
    @Builder.Default
    private Boolean isDirectInput = true;

    @Column(name = "is_aggregated", nullable = false)
    @Builder.Default
    private Boolean isAggregated = false;

    @Column(name = "aggregation_level", nullable = false)
    @Builder.Default
    private Integer aggregationLevel = 0;

    // 감사 필드 (생성일/수정일만)
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
```

### 2.2 제품 코드 매핑 테이블 - ProductCodeMapping 엔티티

```java
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
```

### 2.3 ScopeEmission와 ProductCodeMapping 연동 및 대시보드 활용 예시

- ScopeEmission의 companyProductCode와 ProductCodeMapping의 companyProductCode, headquartersProductCode를 활용해 본사 기준으로 협력사 데이터를 집계할 수 있음
- 대시보드에서는 본사 제품 코드(W250 등) 기준으로 ProductCodeMapping에서 활성 매핑만 조회하여, 해당 매핑에 연결된 ScopeEmission 데이터만 집계

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

## 3. 열거형 정의

### 3.1 ScopeType 열거형

```java
/**
 * Scope 타입 열거형
 */
public enum ScopeType {
    SCOPE1("직접 배출"),
    SCOPE2("간접 배출 - 에너지"),
    SCOPE3("기타 간접 배출");

    private final String description;

    ScopeType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
```

### 3.2 Scope1Category 열거형

```java
/**
 * Scope 1 카테고리 열거형 - 직접 온실가스 배출
 */
public enum Scope1Category {
    STATIONARY_COMBUSTION(1, "고정연소", "보일러, 발전기 등 고정 설비에서의 연료 연소"),
    MOBILE_COMBUSTION(2, "이동연소", "차량, 선박, 항공기 등 이동 수단에서의 연료 연소"),
    PROCESS_EMISSIONS(3, "공정배출", "화학반응, 물리적 변화 등 산업공정에서 발생하는 배출"),
    REFRIGERANT_LEAKAGE(4, "냉매누출", "냉장, 냉동, 에어컨 등에서 냉매가스 누출");

    private final int categoryNumber;
    private final String categoryName;
    private final String description;

    Scope1Category(int categoryNumber, String categoryName, String description) {
        this.categoryNumber = categoryNumber;
        this.categoryName = categoryName;
        this.description = description;
    }

    public int getCategoryNumber() { return categoryNumber; }
    public String getCategoryName() { return categoryName; }
    public String getDescription() { return description; }

    public static Scope1Category fromCategoryNumber(int categoryNumber) {
        for (Scope1Category category : values()) {
            if (category.getCategoryNumber() == categoryNumber) {
                return category;
            }
        }
        throw new IllegalArgumentException("유효하지 않은 Scope 1 카테고리 번호: " + categoryNumber);
    }
}
```

### 3.3 Scope2Category 열거형

```java
/**
 * Scope 2 카테고리 열거형 - 간접 온실가스 배출 (에너지)
 */
public enum Scope2Category {
    INDIRECT_EMISSIONS(1, "간접배출", "구매한 전력 사용으로 인한 간접 배출"),
    ENERGY_USE(2, "에너지사용", "스팀, 냉난방 등 구매한 에너지 사용"),
    EXTERNAL_EMISSIONS(3, "외부배출", "외부에서 공급받는 기타 에너지원 사용");

    private final int categoryNumber;
    private final String categoryName;
    private final String description;

    Scope2Category(int categoryNumber, String categoryName, String description) {
        this.categoryNumber = categoryNumber;
        this.categoryName = categoryName;
        this.description = description;
    }

    public int getCategoryNumber() { return categoryNumber; }
    public String getCategoryName() { return categoryName; }
    public String getDescription() { return description; }

    public static Scope2Category fromCategoryNumber(int categoryNumber) {
        for (Scope2Category category : values()) {
            if (category.getCategoryNumber() == categoryNumber) {
                return category;
            }
        }
        throw new IllegalArgumentException("유효하지 않은 Scope 2 카테고리 번호: " + categoryNumber);
    }
}
```

### 3.4 Scope3Category 열거형

```java
/**
 * Scope 3 카테고리 열거형 - GHG 프로토콜 기준 15개 카테고리
 */
public enum Scope3Category {
    // 업스트림 카테고리 (1-8)
    PURCHASED_GOODS_SERVICES(1, "구매한 상품 및 서비스"),
    CAPITAL_GOODS(2, "자본재"),
    FUEL_ENERGY_ACTIVITIES(3, "연료 및 에너지 관련 활동"),
    UPSTREAM_TRANSPORTATION(4, "업스트림 운송 및 유통"),
    WASTE_GENERATED(5, "폐기물 처리"),
    BUSINESS_TRAVEL(6, "사업장 관련 활동"),
    EMPLOYEE_COMMUTING(7, "직원 통근"),
    UPSTREAM_LEASED_ASSETS(8, "업스트림 임대 자산"),

    // 다운스트림 카테고리 (9-15)
    DOWNSTREAM_TRANSPORTATION(9, "다운스트림 운송 및 유통"),
    PROCESSING_SOLD_PRODUCTS(10, "판매 후 처리"),
    USE_SOLD_PRODUCTS(11, "제품 사용"),
    END_OF_LIFE_SOLD_PRODUCTS(12, "제품 폐기"),
    DOWNSTREAM_LEASED_ASSETS(13, "다운스트림 임대 자산"),
    FRANCHISES(14, "프랜차이즈"),
    INVESTMENTS(15, "투자");

    private final int categoryNumber;
    private final String categoryName;

    Scope3Category(int categoryNumber, String categoryName) {
        this.categoryNumber = categoryNumber;
        this.categoryName = categoryName;
    }

    public int getCategoryNumber() { return categoryNumber; }
    public String getCategoryName() { return categoryName; }

    public static Scope3Category fromCategoryNumber(int categoryNumber) {
        for (Scope3Category category : values()) {
            if (category.getCategoryNumber() == categoryNumber) {
                return category;
            }
        }
        throw new IllegalArgumentException("유효하지 않은 Scope 3 카테고리 번호: " + categoryNumber);
    }

    public boolean isUpstream() {
        return categoryNumber >= 1 && categoryNumber <= 8;
    }

    public boolean isDownstream() {
        return categoryNumber >= 9 && categoryNumber <= 15;
    }
}
```

## 5. 테이블 요약

### 5.1 최종 테이블 구조 (비정규화)

| 테이블명         | 용도        | 엔티티          | 설명                                             |
| ---------------- | ----------- | --------------- | ------------------------------------------------ |
| `scope_emission` | 메인 데이터 | `ScopeEmission` | 모든 Scope 배출량 데이터 통합 관리 (사용자 입력) |

### 5.2 열거형 요약

| 열거형명         | 카테고리 수 | 설명                                   |
| ---------------- | ----------- | -------------------------------------- |
| `ScopeType`      | 3개         | Scope 1, 2, 3 타입                     |
| `Scope1Category` | 4개         | 고정연소, 이동연소, 공정배출, 냉매누출 |
| `Scope2Category` | 3개         | 간접배출, 에너지사용, 외부배출         |
| `Scope3Category` | 15개        | GHG 프로토콜 기준 15개 카테고리        |

## 6. 사용 예시

### 6.1 Scope 1 데이터 생성 (사용자 직접 입력)

```java
// 고정연소 데이터 생성 - 사용자가 모든 값 직접 입력
ScopeEmission scope1Data = ScopeEmission.builder()
    .headquartersId(1L)
    .partnerId(101L)
    .treePath("/1/L1-001/L2-003/")
    .companyName("3차 하청업체")
    .reportingYear(2024)
    .reportingMonth(12)
    .scopeType(ScopeType.SCOPE1)
    .majorCategory("에너지") // 사용자 입력
    .subcategory("보일러") // 사용자 입력
    .rawMaterial("LNG") // 사용자 입력
    .activityAmount(new BigDecimal("1000")) // 사용자 입력
    .unit("kg") // 사용자 입력
    .emissionFactor(new BigDecimal("2.75")) // 사용자 입력
    .totalEmission(new BigDecimal("2750")) // 프론트에서 계산
    .isDirectInput(true)
    .build()
    .withScope1Category(Scope1Category.STATIONARY_COMBUSTION);
```

### 6.2 Scope 2 데이터 생성 (사용자 직접 입력)

```java
// 전력 사용 데이터 생성 - 사용자가 모든 값 직접 입력
ScopeEmission scope2Data = ScopeEmission.builder()
    .headquartersId(1L)
    .partnerId(101L)
    .treePath("/1/L1-001/L2-003/")
    .companyName("3차 하청업체")
    .reportingYear(2024)
    .reportingMonth(12)
    .scopeType(ScopeType.SCOPE2)
    .majorCategory("전력") // 사용자 입력
    .subcategory("사무실") // 사용자 입력
    .rawMaterial("전기") // 사용자 입력
    .activityAmount(new BigDecimal("5000")) // 사용자 입력 (kWh)
    .unit("kWh") // 사용자 입력
    .emissionFactor(new BigDecimal("0.4781")) // 사용자 입력 (전력 배출계수)
    .totalEmission(new BigDecimal("2390.5")) // 프론트에서 계산
    .isDirectInput(true)
    .build()
    .withScope2Category(Scope2Category.INDIRECT_EMISSIONS);
```

### 6.3 Scope 3 데이터 생성 (사용자 직접 입력)

```java
// Scope 3 데이터 생성 - 사용자가 모든 값 직접 입력
ScopeEmission scope3Data = ScopeEmission.builder()
    .headquartersId(1L)
    .partnerId(101L)
    .treePath("/1/L1-001/L2-003/")
    .companyName("3차 하청업체")
    .reportingYear(2024)
    .reportingMonth(12)
    .scopeType(ScopeType.SCOPE3)
    .majorCategory("운송") // 사용자 입력
    .subcategory("출장") // 사용자 입력
    .rawMaterial("항공연료") // 사용자 입력
    .activityAmount(new BigDecimal("500")) // 사용자 입력 (km)
    .unit("km") // 사용자 입력
    .emissionFactor(new BigDecimal("0.255")) // 사용자 입력
    .totalEmission(new BigDecimal("127.5")) // 프론트에서 계산
    .isDirectInput(true)
    .build()
    .withScope3Category(Scope3Category.BUSINESS_TRAVEL);
```

### 6.4 집계 데이터 생성

```java
// 하위 조직 데이터들의 집계
List<ScopeEmission> childEmissions = Arrays.asList(scope1Data, scope2Data, scope3Data);

ScopeEmission aggregatedData = scope1Data.createAggregation(
    childEmissions,
    "system-aggregation"
);
```

## 7. 비정규화 설계의 장점

### 7.1 성능상 장점

1. **단일 테이블 조회**: 조인 없이 모든 정보를 한 번에 조회 가능
2. **인덱스 효율성**: 단일 테이블 인덱스로 빠른 검색 성능
3. **캐싱 단순화**: 단일 엔티티 캐싱으로 메모리 효율성 향상
4. **집계 성능**: TreePath 기반 집계 시 조인 오버헤드 없음

### 7.2 개발상 장점

1. **단순한 구조**: 복잡한 관계 설정 없이 직관적인 엔티티 구조
2. **프론트엔드 호환성**: 사용자 입력 폼과 1:1 매핑되는 구조
3. **MSA 독립성**: 다른 마이크로서비스 의존성 최소화
4. **유지보수 용이성**: 단일 테이블 관리로 변경 영향도 최소화

### 7.3 비즈니스상 장점

1. **사용자 편의성**: 모든 값을 직접 입력하여 유연성 제공
2. **데이터 투명성**: 입력한 값 그대로 저장되어 추적 가능
3. **확장성**: 새로운 카테고리나 필드 추가 시 영향 최소화
4. **데이터 일관성**: 입력 시점의 데이터 스냅샷 보존

## 8. 주의사항 및 고려사항

### 8.1 데이터 중복 관리

```java
// 카테고리명 검증 로직
@PrePersist
@PreUpdate
private void validateCategoryData() {
    // Enum과 저장된 카테고리명 일치성 검증
    if (scopeType == ScopeType.SCOPE1 && scope1CategoryNumber != null) {
        Scope1Category category = Scope1Category.fromCategoryNumber(scope1CategoryNumber);
        if (!category.getCategoryName().equals(scope1CategoryName)) {
            throw new IllegalStateException("Scope 1 카테고리 불일치");
        }
    }
    // Scope 2, 3에 대해서도 동일한 검증 로직 적용
}
```

### 8.2 데이터 품질 보장

```java
// 사용자 입력 검증
public void validateUserInput() {
    if (!isValidUserInput()) {
        throw new IllegalArgumentException("필수 입력값이 누락되었습니다");
    }

    // 계산값 검증
    BigDecimal calculatedEmission = activityAmount.multiply(emissionFactor);
    if (totalEmission.compareTo(calculatedEmission) != 0) {
        throw new IllegalArgumentException("배출량 계산이 일치하지 않습니다");
    }
}
```

### 8.3 성능 최적화 전략

1. **적절한 인덱싱**: 자주 조회되는 컬럼 조합에 대한 복합 인덱스
2. **페이징 처리**: 대량 데이터 조회 시 페이징 적용
3. **캐싱 전략**: 자주 조회되는 집계 데이터 캐싱
4. **배치 처리**: 대량 집계 작업은 비동기 배치로 처리

## 9. 최종 정리

### 9.1 설계 요약

**총 테이블 수: 1개**

- 메인 데이터 테이블: `scope_emission` (사용자 직접 입력 기반)
  **총 엔티티/열거형: 5개**

- 메인 엔티티: 1개 (`ScopeEmission`)
- 열거형: 4개 (`ScopeType`, `Scope1Category`, `Scope2Category`, `Scope3Category`)

### 9.2 핵심 특징

1. **비정규화된 단일 테이블**: 모든 Scope 데이터를 하나의 테이블로 통합 관리
2. **사용자 직접 입력**: 배출계수 마스터 없이 모든 값을 프론트엔드에서 입력
3. **성능 최적화**: 조인 없는 단일 테이블 조회로 뛰어난 성능
4. **계층적 집계**: TreePath 기반 상향 집계 지원
5. **권한 기반 접근**: JWT + TreePath로 데이터 격리
6. **단순한 CRUD**: PK 기반 수정/삭제로 복잡성 최소화

이 설계는 **성능과 단순성을 최우선**으로 하여, 사용자가 모든 값을 직접 입력하는 환경에 최적화된 비정규화 구조입니다.
