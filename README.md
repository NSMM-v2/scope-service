# Scope Service - GHG 온실가스 배출량 계산 및 관리 시스템

**포트폴리오 프로젝트**: ESG 플랫폼 - Scope 1/2/3 탄소배출량 관리 서비스

## 프로젝트 개요

Scope Service는 GHG(온실가스) 프로토콜에 따른 **Scope 1, 2, 3 탄소배출량 계산 및 관리**를 담당하는 마이크로서비스입니다. 기업의 직접배출, 간접배출, 기타간접배출을 체계적으로 관리하며 ESG 경영에 필수적인 탄소발자국 추적 시스템을 제공합니다.

### 핵심 기능

- **통합 Scope 관리**: Scope 1(직접배출), Scope 2(간접배출-에너지), Scope 3(기타간접배출) 통합 관리
- **카테고리별 배출량 계산**: Scope별 세부 카테고리 기반 정밀 배출량 산정
- **제품 코드 매핑**: 제품별 탄소배출량 추적 및 LCA(생명주기평가) 연동
- **권한 기반 데이터 관리**: 본사/협력사 계층적 권한으로 조직별 배출량 데이터 관리
- **실시간 집계**: 월별/연별 배출량 통계 및 트렌드 분석

### 기술 스택

[![Spring Boot](https://img.shields.io/badge/Framework-Spring%20Boot%203.5.0-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Language-Java%2017-orange.svg)](https://openjdk.java.net/)
[![MySQL](https://img.shields.io/badge/Database-MySQL%208.0%20%2B%20JPA%2FHibernate-blue.svg)](https://www.mysql.com/)
[![Spring Cloud](https://img.shields.io/badge/Cloud%20Platform-Spring%20Cloud%202025.0.0-green.svg)](https://spring.io/projects/spring-cloud)
[![Swagger](https://img.shields.io/badge/Documentation-Swagger%2FOpenAPI%203.0-green.svg)](https://swagger.io/)
[![Gradle](https://img.shields.io/badge/Build%20Tool-Gradle%208.x-blue.svg)](https://gradle.org/)


## 시스템 아키텍처

### 마이크로서비스 구조

```mermaid
graph TB
    subgraph "ESG Platform"
        GW[Gateway Service<br/>:8080] --> SCOPE[Scope Service<br/>:8082]
        GW --> AUTH[Auth Service<br/>:8081]
        GW --> CSDDD[CSDDD Service<br/>:8083]
        GW --> DART[DART Service]
    end
    
    subgraph "Infrastructure"
        CONFIG[Config Service<br/>:8888] --> EUREKA[Discovery Service<br/>:8761]
        EUREKA --> GW
        EUREKA --> SCOPE
        EUREKA --> AUTH
    end
    
    subgraph "Data Layer"
        SCOPE --> MYSQL[(MySQL Database)]
        SCOPE -.-> CALCULATOR[Emission Calculator]
    end
    
    subgraph "External"
        FRONTEND[Frontend<br/>Next.js] --> GW
        GITHUB[GitHub Config Repository] --> CONFIG
        LCA[LCA Database] -.-> SCOPE
    end
```

### Scope 서비스 내부 구조

```mermaid
graph LR
    subgraph "Scope Service Architecture"
        Controller[Controllers<br/>API Layer] --> Service[Services<br/>Business Logic]
        Service --> Repository[JPA Repositories<br/>Data Access]
        Service --> Utils[Utility Components<br/>Aggregation & Calculation]
        
        subgraph "Domain Model"
            Entity[ScopeEmission<br/>배출량 엔티티]
            Enum1[ScopeType<br/>Scope 분류]
            Enum2[Category Enums<br/>카테고리 분류]
            Entity -.-> Enum1
            Entity -.-> Enum2
        end
        
        Repository --> Entity
        Utils --> Entity
    end
```

## GHG 프로토콜 Scope 분류

### Scope 카테고리 구조

```mermaid
graph TD
    subgraph "GHG Protocol Scope Classification"
        SCOPE1[Scope 1<br/>직접배출<br/>10개 카테고리]
        SCOPE2[Scope 2<br/>간접배출-에너지<br/>2개 카테고리]
        SCOPE3[Scope 3<br/>기타간접배출<br/>15개 카테고리]
        
        subgraph "Scope 1 Categories"
            S1_1[1. 고정연소]
            S1_2[2. 이동연소]
            S1_3[3. 공정배출]
            S1_4[4. 냉매누출]
        end
        
        subgraph "Scope 2 Categories"
            S2_1[1. 전력]
            S2_2[2. 스팀/열]
        end
        
        subgraph "Scope 3 Categories"
            S3_1[1. 구매 제품/서비스]
            S3_2[2. 자본재]
            S3_3[3. 연료/에너지]
            S3_4[4. 운송/유통-업스트림]
            S3_15[15. 투자]
        end
        
        SCOPE1 --> S1_1
        SCOPE1 --> S1_2
        SCOPE1 --> S1_3
        SCOPE1 --> S1_4
        
        SCOPE2 --> S2_1
        SCOPE2 --> S2_2
        
        SCOPE3 --> S3_1
        SCOPE3 --> S3_2
        SCOPE3 --> S3_3
        SCOPE3 --> S3_4
        SCOPE3 --> S3_15
    end
    
    style SCOPE1 fill:#ffebee
    style SCOPE2 fill:#f3e5f5
    style SCOPE3 fill:#e8f5e8
```

## 배출량 계산 플로우

### 배출량 등록 시퀀스

```mermaid
sequenceDiagram
    participant Client as Frontend Client
    participant GW as API Gateway
    participant SCOPE as Scope Service
    participant DB as MySQL Database
    participant CALC as Emission Calculator

    Client->>GW: POST /api/v1/scope/emissions
    Note over Client,GW: { scopeType, category, activityAmount, emissionFactor }
    GW->>SCOPE: Forward with Auth Headers
    
    SCOPE->>SCOPE: Validate User Permissions
    SCOPE->>SCOPE: Validate Basic Fields
    
    alt Product Code Mapping
        SCOPE->>SCOPE: Validate Product Code (Scope 1,2 only)
        Note over SCOPE: Scope 3는 제품 코드 매핑 불가
    end
    
    SCOPE->>CALC: Calculate Total Emission
    Note over CALC: totalEmission = activityAmount × emissionFactor
    CALC-->>SCOPE: Return Calculated Emission
    
    SCOPE->>DB: Save Emission Data
    DB-->>SCOPE: Return Saved Entity
    
    SCOPE->>GW: Emission Response
    GW->>Client: Creation Success
```

### 배출량 조회 시퀀스

```mermaid
sequenceDiagram
    participant Client as Frontend Client
    participant GW as API Gateway
    participant SCOPE as Scope Service
    participant DB as MySQL Database
    participant AGG as Aggregation Service

    Client->>GW: GET /api/v1/scope/emissions/scope/{scopeType}
    Note over Client,GW: Query Parameters + Pagination
    GW->>SCOPE: Forward with Auth Headers
    
    SCOPE->>SCOPE: Validate User Permissions
    
    alt Headquarters User
        SCOPE->>DB: Query Own Headquarters Data
        Note over SCOPE: partnerId = null 조건
    else Partner User
        SCOPE->>DB: Query Own Partner Data
        Note over SCOPE: partnerId = user.partnerId 조건
    end
    
    DB-->>SCOPE: Return Emission List
    
    SCOPE->>AGG: Aggregate by Category (Optional)
    AGG-->>SCOPE: Return Aggregated Data
    
    SCOPE->>GW: Emission List Response
    GW->>Client: Query Success
```

## 데이터 모델

### 핵심 엔티티 구조

```mermaid
erDiagram
    ScopeEmission {
        bigint id PK
        bigint headquarters_id FK "본사 ID"
        bigint partner_id FK "협력사 ID"
        string tree_path "계층 경로"
        int reporting_year "보고 연도"
        int reporting_month "보고 월"
        enum scope_type "Scope 분류"
        int scope1_category_number "Scope1 카테고리 번호"
        string scope1_category_name "Scope1 카테고리명"
        string scope1_category_group "Scope1 그룹"
        int scope2_category_number "Scope2 카테고리 번호"
        string scope2_category_name "Scope2 카테고리명"
        int scope3_category_number "Scope3 카테고리 번호"
        string scope3_category_name "Scope3 카테고리명"
        string company_product_code "제품 코드"
        string product_name "제품명"
        string major_category "대분류"
        string subcategory "구분"
        string raw_material "원료/에너지"
        decimal activity_amount "활동량"
        string unit "단위"
        decimal emission_factor "배출계수"
        decimal total_emission "총 배출량"
        enum input_type "입력 모드"
        boolean has_product_mapping "제품 코드 매핑 여부"
        boolean factory_enabled "공장 설비 활성화"
        datetime created_at
        datetime updated_at
    }
```

### Scope 카테고리 매핑

| Scope Type | 카테고리 수 | 설명 | 제품 코드 매핑 |
|------------|-------------|------|----------------|
| **Scope 1** | 10개 | 직접배출 (고정연소, 이동연소, 공정배출, 냉매누출) | 지원 |
| **Scope 2** | 2개 | 간접배출-에너지 (전력, 스팀/열) | 지원 |
| **Scope 3** | 15개 | 기타간접배출 (구매 제품/서비스, 자본재, 투자 등) | 미지원 |

## 보안 및 권한

### 계층적 권한 시스템

- **TreePath 기반**: `/1/L1-001/L2-003/` 형식의 계층 구조
- **본사 권한**: 자신의 본사 데이터만 조회/수정 (하위 협력사 데이터 제외)
- **협력사 권한**: 자신의 협력사 데이터만 조회/수정
- **데이터 격리**: 조직별 완전 분리된 배출량 데이터 관리

### API 보안 헤더

```
X-USER-TYPE: HEADQUARTERS | PARTNER
X-HEADQUARTERS-ID: {본사ID}
X-PARTNER-ID: {협력사ID} (협력사인 경우)
X-TREE-PATH: {계층경로}
X-ACCOUNT-NUMBER: {계정번호}
```

## 배출량 계산 알고리즘

### 기본 계산 공식

```java
// 기본 배출량 계산
BigDecimal totalEmission = activityAmount.multiply(emissionFactor);

// 활동량 × 배출계수 = 총 배출량 (tCO2eq)
// 예: 전력 사용량 1,000 kWh × 배출계수 0.4781 kgCO2eq/kWh = 478.1 kgCO2eq
```

### 배출계수 적용 기준

| 분류 | 단위 | 배출계수 예시 | 설명 |
|------|------|---------------|------|
| **전력** | kWh | 0.4781 kgCO2eq/kWh | 국가 전력 배출계수 |
| **도시가스** | MJ | 0.0561 kgCO2eq/MJ | 연료 연소 배출계수 |
| **경유** | L | 2.6447 kgCO2eq/L | 이동연소 배출계수 |
| **냉매** | kg | 1,810 kgCO2eq/kg | 지구온난화지수(GWP) |

## API 문서

### 주요 엔드포인트

#### 배출량 관리 API

| HTTP Method | Endpoint | 설명 | 인증 | 응답 |
|-------------|----------|------|------|------|
| POST | `/api/v1/scope/emissions` | 배출량 데이터 생성 | 필요 | ScopeEmissionResponse |
| GET | `/api/v1/scope/emissions/scope/{scopeType}` | Scope별 배출량 조회 | 필요 | List<ScopeEmissionResponse> |
| PUT | `/api/v1/scope/emissions/{id}` | 배출량 데이터 수정 | 필요 | ScopeEmissionResponse |
| DELETE | `/api/v1/scope/emissions/{id}` | 배출량 데이터 삭제 | 필요 | Success Message |

#### 집계 API

| HTTP Method | Endpoint | 설명 | 인증 | 응답 |
|-------------|----------|------|------|------|
| GET | `/api/v1/scope/aggregation/monthly` | 월별 배출량 집계 | 필요 | MonthlyEmissionSummary |
| GET | `/api/v1/scope/aggregation/yearly` | 연별 배출량 집계 | 필요 | CategoryYearlyEmission |
| GET | `/api/v1/scope/aggregation/category` | 카테고리별 집계 | 필요 | CategoryMonthlyEmission |

### Swagger UI

서비스 실행 후 `http://localhost:8082/swagger-ui.html`에서 API 문서 확인 가능

## 실행 방법

### 개발 환경 구성

```bash
# 핵심 서비스 시작
./backend/run-core-services.sh

# Scope 서비스 실행
cd backend/scope-service
./gradlew bootRun
```

### 환경 변수 설정

```yaml
# application.yml
spring:
  application:
    name: scope-service
  config:
    import: optional:configserver:http://localhost:8888

server:
  port: 8082

# Swagger 설정
springdoc:
  api-docs:
    path: /api-docs
  swagger-ui:
    path: /swagger-ui.html
    operations-sorter: method
```

## 테스트

```bash
# 단위 테스트 실행
./gradlew test

# 통합 테스트 실행
./gradlew integrationTest

# 배출량 계산 검증 테스트
./gradlew calculationTest
```

## 핵심 구현 특징

### 1. GHG 프로토콜 준수

- **표준 Scope 분류**: GHG 프로토콜 표준에 따른 Scope 1/2/3 분류
- **카테고리 관리**: 각 Scope별 세부 카테고리 체계적 관리
- **배출계수 적용**: 국가 및 국제 표준 배출계수 적용

### 2. 제품별 탄소발자국 추적

```java
// Scope 1, 2에서만 제품 코드 매핑 지원
if (Boolean.TRUE.equals(request.getHasProductMapping())) {
    if (request.getScopeType() == ScopeType.SCOPE3) {
        throw new IllegalArgumentException("Scope 3는 제품 코드 매핑을 설정할 수 없습니다");
    }
    // 제품별 배출량 추적 로직
}
```

### 3. 데이터 무결성 보장

```java
@PrePersist
@PreUpdate
private void validateInputData() {
    // 배출량 계산 검증
    BigDecimal calculated = activityAmount.multiply(emissionFactor);
    if (totalEmission.compareTo(calculated) != 0) {
        throw new IllegalStateException("배출량 계산이 일치하지 않습니다");
    }
}
```

### 4. 성능 최적화 인덱스

```sql
-- 주요 인덱스 전략
CREATE INDEX idx_scope_year_month ON scope_emission(headquarters_id, reporting_year, reporting_month);
CREATE INDEX idx_scope_category ON scope_emission(scope_type, scope1_category_number, scope2_category_number, scope3_category_number);
CREATE INDEX idx_product_code ON scope_emission(headquarters_id, company_product_code, reporting_year, reporting_month);
```

## 성능 최적화

### 데이터베이스 최적화

- **인덱스 전략**: 조회 패턴에 최적화된 복합 인덱스
- **파티셔닝**: 연도별 테이블 파티셔닝으로 대용량 데이터 처리
- **집계 쿼리 최적화**: 월별/연별 집계를 위한 효율적인 쿼리

### 메모리 최적화

- **BigDecimal 사용**: 정밀한 배출량 계산을 위한 고정소수점 연산
- **엔티티 캐싱**: 자주 조회되는 카테고리 정보 캐싱
- **Connection Pool**: HikariCP 최적화 설정

## 주요 특징

- **확장성**: 새로운 Scope 카테고리 추가 용이한 설계
- **정확성**: GHG 프로토콜 표준 준수 및 정밀한 배출량 계산
- **추적성**: 제품별/카테고리별 탄소발자국 완전 추적
- **보안성**: 조직별 완전 분리된 데이터 접근 제어
- **성능**: 대용량 배출량 데이터 처리 최적화

---

**기술적 성과**:
- GHG 프로토콜 표준에 따른 체계적인 탄소배출량 관리 시스템 구현
- BigDecimal 기반 정밀한 배출량 계산 알고리즘 설계
- 제품별 LCA 연동을 위한 유연한 데이터 모델링
- 대용량 시계열 배출량 데이터 처리 최적화

