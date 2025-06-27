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

### 2.1 메인 데이터 테이블 - ScopeEmission 엔티티 (사용자 직접 입력 기반)

```java
/**
 * 통합 Scope 배출량 엔티티 - 사용자 직접 입력 기반
 *
 * 특징:
 * - 사용자가 프론트에서 활동량, 배출계수, 배출량 모두 직접 입력
 * - 배출계수 마스터 테이블 불필요
 * - 단순하고 직관적인 구조
 *
 * @author ESG Project Team
 * @version 1.0
 * @since 2024
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

    // ========================================================================
    // 권한 제어 및 계층 구조
    // ========================================================================

    @Column(name = "headquarters_id", nullable = false)
    private Long headquartersId;

    @Column(name = "partner_id")
    private Long partnerId;

    @Column(name = "tree_path", nullable = false, length = 500)
    private String treePath;

    @Column(name = "company_name", nullable = false)
    private String companyName;

    @Column(name = "reporting_year", nullable = false)
    private Integer reportingYear;

    @Column(name = "reporting_month", nullable = false)
    private Integer reportingMonth;

    // ========================================================================
    // Scope 분류 및 카테고리
    // ========================================================================

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

    // ========================================================================
    // 사용자 직접 입력 데이터 (User Input Data)
    // ========================================================================

    @Column(name = "major_category", nullable = false)
    private String majorCategory; // 대분류 (사용자 입력)

    @Column(name = "subcategory", nullable = false)
    private String subcategory; // 구분 (사용자 입력)

    @Column(name = "raw_material", nullable = false)
    private String rawMaterial; // 원료/에너지 (사용자 입력)

    @Column(name = "activity_amount", nullable = false, precision = 15, scale = 3)
    private BigDecimal activityAmount; // 활동량 (사용자 입력)

    @Column(name = "unit", nullable = false, length = 20)
    private String unit; // 단위 (사용자 입력)

    @Column(name = "emission_factor", nullable = false, precision = 15, scale = 6)
    private BigDecimal emissionFactor; // 배출계수 (사용자 입력)

    @Column(name = "total_emission", nullable = false, precision = 15, scale = 6)
    private BigDecimal totalEmission; // 총 배출량 (프론트에서 계산된 값)

    // ========================================================================
    // 집계 제어
    // ========================================================================

    @Column(name = "is_direct_input", nullable = false)
    @Builder.Default
    private Boolean isDirectInput = true; // 사용자 직접 입력 여부

    @Column(name = "is_aggregated", nullable = false)
    @Builder.Default
    private Boolean isAggregated = false; // 집계 데이터 여부

    @Column(name = "aggregation_level", nullable = false)
    @Builder.Default
    private Integer aggregationLevel = 0; // 집계 레벨

    // ========================================================================
    // 감사 필드
    // ========================================================================

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by", nullable = false, length = 36)
    private String createdBy;

    @Column(name = "updated_by", nullable = false, length = 36)
    private String updatedBy;

    // ========================================================================
    // 비즈니스 메서드 (Business Methods)
    // ========================================================================

    /**
     * Scope 1 카테고리 설정
     */
    public ScopeEmission withScope1Category(Scope1Category category) {
        return this.toBuilder()
                .scope1CategoryNumber(category.getCategoryNumber())
                .scope1CategoryName(category.getCategoryName())
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
     * 배출량 재계산 (사용자 입력값 기반)
     */
    public ScopeEmission recalculateEmission() {
        BigDecimal newEmission = this.activityAmount.multiply(this.emissionFactor);
        return this.toBuilder()
                .totalEmission(newEmission)
                .build();
    }

    /**
     * 집계 데이터 생성
     */
    public ScopeEmission createAggregation(List<ScopeEmission> childEmissions, String aggregatedBy) {
        BigDecimal totalAggregatedEmission = childEmissions.stream()
                .map(ScopeEmission::getTotalEmission)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return ScopeEmission.builder()
                .headquartersId(this.headquartersId)
                .partnerId(this.partnerId)
                .treePath(this.treePath)
                .companyName(this.companyName)
                .reportingYear(this.reportingYear)
                .reportingMonth(this.reportingMonth)
                .scopeType(this.scopeType)
                .scope1CategoryNumber(this.scope1CategoryNumber)
                .scope1CategoryName(this.scope1CategoryName)
                .scope2CategoryNumber(this.scope2CategoryNumber)
                .scope2CategoryName(this.scope2CategoryName)
                .scope3CategoryNumber(this.scope3CategoryNumber)
                .scope3CategoryName(this.scope3CategoryName)
                .majorCategory(this.majorCategory)
                .subcategory(this.subcategory)
                .rawMaterial(this.rawMaterial)
                .activityAmount(BigDecimal.ZERO) // 집계 데이터는 활동량 없음
                .unit(this.unit)
                .emissionFactor(BigDecimal.ZERO) // 집계 데이터는 배출계수 없음
                .totalEmission(totalAggregatedEmission)
                .isDirectInput(false)
                .isAggregated(true)
                .aggregationLevel(this.aggregationLevel + 1)
                .createdBy(aggregatedBy)
                .updatedBy(aggregatedBy)
                .build();
    }

    /**
     * 사용자 입력 데이터 유효성 검증
     */
    public boolean isValidUserInput() {
        return activityAmount != null && activityAmount.compareTo(BigDecimal.ZERO) >= 0 &&
               emissionFactor != null && emissionFactor.compareTo(BigDecimal.ZERO) >= 0 &&
               totalEmission != null && totalEmission.compareTo(BigDecimal.ZERO) >= 0 &&
               majorCategory != null && !majorCategory.trim().isEmpty() &&
               subcategory != null && !subcategory.trim().isEmpty() &&
               rawMaterial != null && !rawMaterial.trim().isEmpty() &&
               unit != null && !unit.trim().isEmpty();
    }
}
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

## 4. 데이터베이스 테이블 DDL

### 4.1 메인 데이터 테이블 (scope_emission) - 사용자 직접 입력 기반

```sql
-- 통합 Scope 배출량 테이블 (사용자 직접 입력 기반)
CREATE TABLE scope_emission (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    -- 권한 제어 및 계층 구조
    headquarters_id BIGINT NOT NULL COMMENT '소속 본사 ID',
    partner_id BIGINT NULL COMMENT '협력사 ID',
    tree_path VARCHAR(500) NOT NULL COMMENT '계층 경로',
    company_name VARCHAR(255) NOT NULL COMMENT '회사명',
    reporting_year INT NOT NULL COMMENT '보고 연도',
    reporting_month INT NOT NULL COMMENT '보고 월',

    -- Scope 분류 및 카테고리
    scope_type ENUM('SCOPE1', 'SCOPE2', 'SCOPE3') NOT NULL COMMENT 'Scope 타입',
    scope1_category_number INT NULL COMMENT 'Scope 1 카테고리 번호 (1-4)',
    scope1_category_name VARCHAR(255) NULL COMMENT 'Scope 1 카테고리명',
    scope2_category_number INT NULL COMMENT 'Scope 2 카테고리 번호 (1-3)',
    scope2_category_name VARCHAR(255) NULL COMMENT 'Scope 2 카테고리명',
    scope3_category_number INT NULL COMMENT 'Scope 3 카테고리 번호 (1-15)',
    scope3_category_name VARCHAR(255) NULL COMMENT 'Scope 3 카테고리명',

    -- 사용자 직접 입력 데이터
    major_category VARCHAR(255) NOT NULL COMMENT '대분류 (사용자 입력)',
    subcategory VARCHAR(255) NOT NULL COMMENT '구분 (사용자 입력)',
    raw_material VARCHAR(255) NOT NULL COMMENT '원료/에너지 (사용자 입력)',
    activity_amount DECIMAL(15,3) NOT NULL COMMENT '활동량 (사용자 입력)',
    unit VARCHAR(20) NOT NULL COMMENT '단위 (사용자 입력)',
    emission_factor DECIMAL(15,6) NOT NULL COMMENT '배출계수 (사용자 입력)',
    total_emission DECIMAL(15,6) NOT NULL COMMENT '총 배출량 (프론트에서 계산)',

    -- 집계 제어
    is_direct_input BOOLEAN NOT NULL DEFAULT TRUE COMMENT '사용자 직접 입력 여부',
    is_aggregated BOOLEAN NOT NULL DEFAULT FALSE COMMENT '집계 데이터 여부',
    aggregation_level INT NOT NULL DEFAULT 0 COMMENT '집계 레벨',

    -- 감사 필드
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(36) NOT NULL COMMENT '생성자 UUID',
    updated_by VARCHAR(36) NOT NULL COMMENT '수정자 UUID',

    -- 인덱스
    INDEX idx_headquarters_year_month (headquarters_id, reporting_year, reporting_month),
    INDEX idx_partner_year_month (partner_id, reporting_year, reporting_month),
    INDEX idx_tree_path (tree_path),
    INDEX idx_scope_type (scope_type),
    INDEX idx_scope1_category (scope1_category_number),
    INDEX idx_scope2_category (scope2_category_number),
    INDEX idx_scope3_category (scope3_category_number),
    INDEX idx_aggregation (is_aggregated, aggregation_level),
    INDEX idx_created_at (created_at),

    -- 복합 인덱스 (성능 최적화)
    INDEX idx_hierarchy_scope (headquarters_id, tree_path, scope_type, reporting_year, reporting_month),
    INDEX idx_category_aggregation (headquarters_id, scope_type, reporting_year, reporting_month),
    INDEX idx_user_input (is_direct_input, headquarters_id, reporting_year),

    -- 제약조건 (데이터 무결성)
    CONSTRAINT chk_scope1_category CHECK (
        (scope_type = 'SCOPE1' AND scope1_category_number BETWEEN 1 AND 4) OR
        (scope_type != 'SCOPE1' AND scope1_category_number IS NULL)
    ),
    CONSTRAINT chk_scope2_category CHECK (
        (scope_type = 'SCOPE2' AND scope2_category_number BETWEEN 1 AND 3) OR
        (scope_type != 'SCOPE2' AND scope2_category_number IS NULL)
    ),
    CONSTRAINT chk_scope3_category CHECK (
        (scope_type = 'SCOPE3' AND scope3_category_number BETWEEN 1 AND 15) OR
        (scope_type != 'SCOPE3' AND scope3_category_number IS NULL)
    ),
    CONSTRAINT chk_positive_amounts CHECK (
        activity_amount >= 0 AND
        emission_factor >= 0 AND
        total_emission >= 0
    ),
    CONSTRAINT chk_user_input_required CHECK (
        (is_direct_input = TRUE AND major_category IS NOT NULL AND subcategory IS NOT NULL AND raw_material IS NOT NULL) OR
        (is_direct_input = FALSE)
    )
);
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
    .createdBy("user-uuid-123")
    .updatedBy("user-uuid-123")
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
    .createdBy("user-uuid-123")
    .updatedBy("user-uuid-123")
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
    .createdBy("user-uuid-123")
    .updatedBy("user-uuid-123")
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
