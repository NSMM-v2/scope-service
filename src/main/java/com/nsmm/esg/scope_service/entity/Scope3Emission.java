package com.nsmm.esg.scope_service.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Scope 3 배출량 엔티티 - Scope 3
 * 
 * 기타 간접 배출량 관리 (15개 카테고리):
 * - 업스트림 카테고리 (1-8): 기업 운영 이전 단계 배출
 * - 다운스트림 카테고리 (9-15): 기업 운영 이후 단계 배출
 * - CSV 기반 분류 체계 (대분류, 구분, 원료/에너지)
 * - 간소화된 계산 공식: 활동량 × 배출계수 = 총 배출량
 * 
 * @author ESG Project Team
 * @version 1.0
 * @since 2024
 */
@Entity
@Table(name = "scope3_emission")
@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Scope3Emission {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id; // Scope 3 배출량 고유 식별자

  // ========================================================================
  // 프론트엔드 입력 구조와 1:1 매핑되는 주요 필드 (입력 순서 기준)
  // ========================================================================

  @Column(name = "major_category", nullable = false)
  private String majorCategory; // 대분류 (프론트엔드 category)

  @Column(name = "subcategory", nullable = false)
  private String subcategory; // 구분 (프론트엔드 separate)

  @Column(name = "raw_material", nullable = false)
  private String rawMaterial; // 원료/에너지 (프론트엔드 rawMaterial)

  @Column(name = "unit", nullable = false, length = 20)
  private String unit; // 단위 (프론트엔드 unit)

  @Column(name = "emission_factor", nullable = false, precision = 15, scale = 6)
  private BigDecimal emissionFactor; // 배출계수 (프론트엔드 emissionFactor)

  @Column(name = "activity_amount", nullable = false, precision = 15, scale = 3)
  private BigDecimal activityAmount; // 수량(활동량, 프론트엔드 quantity)

  @Column(name = "total_emission", nullable = false, precision = 15, scale = 6)
  private BigDecimal totalEmission; // 계산된 배출량 (프론트엔드 totalEmission)

  // ========================================================================
  // 권한 제어 및 기타 정보 (Authorization & Metadata)
  // ========================================================================

  @Column(name = "headquarters_id", nullable = false)
  private Long headquartersId; // 소속 본사 ID

  @Column(name = "partner_id")
  private Long partnerId; // 협력사 ID (협력사인 경우에만 사용)

  @Column(name = "tree_path", nullable = false, length = 500)
  private String treePath; // 계층 경로 - 권한 제어용

  @Column(name = "reporting_year", nullable = false)
  private Integer reportingYear; // 보고 연도

  @Column(name = "reporting_month", nullable = false)
  private Integer reportingMonth; // 보고 월

  @Column(name = "category_number", nullable = false)
  private Integer categoryNumber; // 카테고리 번호 (1-15)

  @Column(name = "category_name", nullable = false)
  private String categoryName; // 카테고리 한국어 명칭

  // ========================================================================
  // 감사 필드 (Audit Fields)
  // ========================================================================

  @CreatedDate
  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt; // 생성 일시

  @LastModifiedDate
  @Column(name = "updated_at")
  private LocalDateTime updatedAt; // 수정 일시

  // ========================================================================
  // 비즈니스 메서드 (Business Methods)
  // ========================================================================

  /**
   * Scope 3 데이터 업데이트 (불변성 보장)
   * 
   * @param activityAmount 새로운 활동량
   * @param updatedBy      수정자 UUID
   * @return 업데이트된 새 인스턴스
   */
  public Scope3Emission updateData(BigDecimal activityAmount, String updatedBy) {
    // 배출량 재계산: 활동량 × 배출계수
    BigDecimal newTotalEmission = activityAmount.multiply(this.emissionFactor);

    return Scope3Emission.builder()
        .id(this.id)
        .headquartersId(this.headquartersId)
        .partnerId(this.partnerId)
        .treePath(this.treePath)
        .reportingYear(this.reportingYear)
        .reportingMonth(this.reportingMonth)
        .categoryNumber(this.categoryNumber)
        .categoryName(this.categoryName)
        .majorCategory(this.majorCategory)
        .subcategory(this.subcategory)
        .rawMaterial(this.rawMaterial)
        .activityAmount(activityAmount != null ? activityAmount : this.activityAmount)
        .unit(this.unit)
        .emissionFactor(this.emissionFactor)
        .totalEmission(newTotalEmission)
        .createdAt(this.createdAt)
        .build();
  }

  /**
   * 배출계수 업데이트 (불변성 보장)
   * 
   * @param emissionFactor 새로운 배출계수
   * @param updatedBy      수정자 UUID
   * @return 업데이트된 새 인스턴스
   */
  public Scope3Emission updateEmissionFactor(BigDecimal emissionFactor, String updatedBy) {
    // 배출량 재계산: 기존 활동량 × 새 배출계수
    BigDecimal newTotalEmission = this.activityAmount.multiply(emissionFactor);

    return Scope3Emission.builder()
        .id(this.id)
        .headquartersId(this.headquartersId)
        .partnerId(this.partnerId)
        .treePath(this.treePath)
        .reportingYear(this.reportingYear)
        .reportingMonth(this.reportingMonth)
        .categoryNumber(this.categoryNumber)
        .categoryName(this.categoryName)
        .majorCategory(this.majorCategory)
        .subcategory(this.subcategory)
        .rawMaterial(this.rawMaterial)
        .activityAmount(this.activityAmount)
        .unit(this.unit)
        .emissionFactor(emissionFactor)
        .totalEmission(newTotalEmission)
        .createdAt(this.createdAt)
        .build();
  }

  /**
   * 카테고리 타입 확인: 업스트림 여부
   */
  public boolean isUpstreamCategory() {
    return categoryNumber >= 1 && categoryNumber <= 8;
  }

  /**
   * 카테고리 타입 확인: 다운스트림 여부
   */
  public boolean isDownstreamCategory() {
    return categoryNumber >= 9 && categoryNumber <= 15;
  }

  /**
   * Scope3Category Enum으로 변환
   */
  public Scope3Category toScope3Category() {
    return Scope3Category.fromCategoryNumber(this.categoryNumber);
  }

}