package com.nsmm.esg.scope_service.service;

import com.nsmm.esg.scope_service.dto.response.Scope3SpecialAggregationResponse;
import com.nsmm.esg.scope_service.repository.ScopeEmissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Scope 3 특수 집계 서비스
 * 
 * 특수 집계 규칙:
 * - Cat.1: (Scope1 전체 - 이동연소 - 공장설비 - 폐수처리) + (Scope2 - 공장설비) + Scope3 Cat.1
 * - Cat.2: Scope1 공장설비 + Scope2 공장설비 + Scope3 Cat.2
 * - Cat.4: Scope1 이동연소 + Scope3 Cat.4
 * - Cat.5: Scope1 폐수처리 + Scope3 Cat.5
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class Scope3SpecialAggregationService {

    private final ScopeEmissionRepository scopeEmissionRepository;

    /**
     * 특수 집계 실행 - 로그인된 사용자 기준 (계층적 권한 지원)
     */
    @Transactional(readOnly = true)
    public Scope3SpecialAggregationResponse getSpecialAggregation(
            Integer year,
            Integer month,
            Long headquartersId,
            String userType,
            Long partnerId,
            String treePath) {

        log.info("Scope 3 특수 집계 시작 - 연도: {}, 월: {}, 본사ID: {}, 사용자타입: {}, 협력사ID: {}, TreePath: {}",
                year, month, headquartersId, userType, partnerId, treePath);

        // 1. Cat.1 집계
        Scope3SpecialAggregationResponse.Category1Detail category1Detail = calculateCategory1(
                year, month, headquartersId, userType, partnerId, treePath);

        // 2. Cat.2 집계
        Scope3SpecialAggregationResponse.Category2Detail category2Detail = calculateCategory2(
                year, month, headquartersId, userType, partnerId, treePath);

        // 3. Cat.4 집계
        Scope3SpecialAggregationResponse.Category4Detail category4Detail = calculateCategory4(
                year, month, headquartersId, userType, partnerId, treePath);

        // 4. Cat.5 집계
        Scope3SpecialAggregationResponse.Category5Detail category5Detail = calculateCategory5(
                year, month, headquartersId, userType, partnerId, treePath);

        // 5. 응답 생성
        return Scope3SpecialAggregationResponse.builder()
                .reportingYear(year)
                .reportingMonth(month)
                .userType(userType)
                .organizationId("HEADQUARTERS".equals(userType) ? headquartersId : partnerId)
                .category1TotalEmission(category1Detail.getFinalTotal())
                .category1Detail(category1Detail)
                .category2TotalEmission(category2Detail.getFinalTotal())
                .category2Detail(category2Detail)
                .category4TotalEmission(category4Detail.getFinalTotal())
                .category4Detail(category4Detail)
                .category5TotalEmission(category5Detail.getFinalTotal())
                .category5Detail(category5Detail)
                .build();
    }

    /**
     * Cat.1 집계: (Scope1 전체 - 이동연소 - 공장설비 - 폐수처리) + (Scope2 - 공장설비) + Scope3 Cat.1 + 하위 조직 Cat.1 finalTotal
     */
    private Scope3SpecialAggregationResponse.Category1Detail calculateCategory1(
            Integer year, Integer month, Long headquartersId, String userType, Long partnerId, String treePath) {

        boolean isHeadquarters = "HEADQUARTERS".equals(userType);

        // 본인 조직의 데이터 수집
        BigDecimal scope1Total, scope1MobileCombustion, scope1Factory, scope1WasteWater;
        BigDecimal scope2Total, scope2Factory, scope3Category1;

        if (isHeadquarters) {
            // 본사는 본사 직접 입력 데이터만
            scope1Total = scopeEmissionRepository.sumScope1TotalEmissionsByYearAndMonthForHeadquarters(headquartersId, year, month);
            scope1MobileCombustion = scopeEmissionRepository.sumScope1MobileCombustionEmissionsByYearAndMonthForHeadquarters(headquartersId, year, month);
            scope1Factory = scopeEmissionRepository.sumScope1FactoryEmissionsByYearAndMonthForHeadquarters(headquartersId, year, month);
            scope1WasteWater = scopeEmissionRepository.sumScope1WasteWaterTreatmentEmissionsByYearAndMonthForHeadquarters(headquartersId, year, month);
            scope2Total = scopeEmissionRepository.sumScope2TotalEmissionsByYearAndMonthForHeadquarters(headquartersId, year, month);
            scope2Factory = scopeEmissionRepository.sumScope2FactoryEmissionsByYearAndMonthForHeadquarters(headquartersId, year, month);
            scope3Category1 = scopeEmissionRepository.sumScope3CategoryEmissionsByYearAndMonthForHeadquarters(headquartersId, 1, year, month);
        } else {
            // 협력사는 본인 데이터만
            scope1Total = scopeEmissionRepository.sumScope1TotalEmissionsByYearAndMonthForPartner(headquartersId, partnerId, year, month);
            scope1MobileCombustion = scopeEmissionRepository.sumScope1MobileCombustionEmissionsByYearAndMonthForPartner(headquartersId, partnerId, year, month);
            scope1Factory = scopeEmissionRepository.sumScope1FactoryEmissionsByYearAndMonthForPartner(headquartersId, partnerId, year, month);
            scope1WasteWater = scopeEmissionRepository.sumScope1WasteWaterTreatmentEmissionsByYearAndMonthForPartner(headquartersId, partnerId, year, month);
            scope2Total = scopeEmissionRepository.sumScope2TotalEmissionsByYearAndMonthForPartner(headquartersId, partnerId, year, month);
            scope2Factory = scopeEmissionRepository.sumScope2FactoryEmissionsByYearAndMonthForPartner(headquartersId, partnerId, year, month);
            scope3Category1 = scopeEmissionRepository.sumScope3CategoryEmissionsByYearAndMonthForPartner(headquartersId, partnerId, 1, year, month);
        }

        // 하위 조직들의 Cat.1 finalTotal 합계 계산
        BigDecimal childOrganizationsCat1Total = calculateChildOrganizationsCat1Total(
                year, month, headquartersId, userType, partnerId, treePath);

        // 계산 수행
        BigDecimal scope1Remaining = scope1Total
                .subtract(scope1MobileCombustion)
                .subtract(scope1Factory)
                .subtract(scope1WasteWater);

        BigDecimal scope2Remaining = scope2Total.subtract(scope2Factory);

        // finalTotal 계산 - 협력사는 하위 조직 데이터만, 본사는 본인 + 하위 조직
        BigDecimal finalTotal;
        if (isHeadquarters) {
            // 본사: 본인 계산 결과 + 하위 조직들의 Cat.1 finalTotal
            finalTotal = scope1Remaining
                    .add(scope2Remaining)
                    .add(scope3Category1)
                    .add(childOrganizationsCat1Total);
        } else {
            // 협력사: 하위 조직들의 Cat.1 finalTotal만 (본인 데이터는 업스트림용)
            finalTotal = childOrganizationsCat1Total;
        }

        return Scope3SpecialAggregationResponse.Category1Detail.builder()
                .scope1Total(scope1Total)
                .scope1MobileCombustion(scope1MobileCombustion)
                .scope1Factory(scope1Factory)
                .scope1WasteWater(scope1WasteWater)
                .scope1Remaining(scope1Remaining)
                .scope2Total(scope2Total)
                .scope2Factory(scope2Factory)
                .scope2Remaining(scope2Remaining)
                .scope3Category1(isHeadquarters ? scope3Category1.add(childOrganizationsCat1Total) : scope3Category1) // 협력사는 본인 데이터만
                .finalTotal(finalTotal)
                .build();
    }

    /**
     * Cat.2 집계: Scope1 공장설비 + Scope2 공장설비 + Scope3 Cat.2 + 하위 조직 Cat.2 finalTotal
     */
    private Scope3SpecialAggregationResponse.Category2Detail calculateCategory2(
            Integer year, Integer month, Long headquartersId, String userType, Long partnerId, String treePath) {

        boolean isHeadquarters = "HEADQUARTERS".equals(userType);

        BigDecimal scope1Factory = isHeadquarters
                ? scopeEmissionRepository.sumScope1FactoryEmissionsByYearAndMonthForHeadquarters(headquartersId, year, month)
                : scopeEmissionRepository.sumScope1FactoryEmissionsByYearAndMonthForPartner(headquartersId, partnerId, year, month);

        BigDecimal scope2Factory = isHeadquarters
                ? scopeEmissionRepository.sumScope2FactoryEmissionsByYearAndMonthForHeadquarters(headquartersId, year, month)
                : scopeEmissionRepository.sumScope2FactoryEmissionsByYearAndMonthForPartner(headquartersId, partnerId, year, month);

        BigDecimal scope3Category2 = isHeadquarters
                ? scopeEmissionRepository.sumScope3CategoryEmissionsByYearAndMonthForHeadquarters(headquartersId, 2, year, month)
                : scopeEmissionRepository.sumScope3CategoryEmissionsByYearAndMonthForPartner(headquartersId, partnerId, 2, year, month);

        // 하위 조직들의 Cat.2 finalTotal 합계 계산
        BigDecimal childOrganizationsCat2Total = calculateChildOrganizationsCat2Total(
                year, month, headquartersId, userType, partnerId, treePath);

        // finalTotal 계산 - 협력사는 하위 조직 데이터만, 본사는 본인 + 하위 조직
        BigDecimal finalTotal;
        if (isHeadquarters) {
            // 본사: 본인 계산 결과 + 하위 조직들의 Cat.2 finalTotal
            finalTotal = scope1Factory.add(scope2Factory).add(scope3Category2).add(childOrganizationsCat2Total);
        } else {
            // 협력사: 하위 조직들의 Cat.2 finalTotal만 (본인 데이터는 업스트림용)
            finalTotal = childOrganizationsCat2Total;
        }

        return Scope3SpecialAggregationResponse.Category2Detail.builder()
                .scope1Factory(scope1Factory)
                .scope2Factory(scope2Factory)
                .scope3Category2(isHeadquarters ? scope3Category2.add(childOrganizationsCat2Total) : scope3Category2) // 협력사는 본인 데이터만
                .finalTotal(finalTotal)
                .build();
    }

    /**
     * Cat.4 집계: Scope1 이동연소 + Scope3 Cat.4 + 하위 조직 Cat.4 finalTotal
     */
    private Scope3SpecialAggregationResponse.Category4Detail calculateCategory4(
            Integer year, Integer month, Long headquartersId, String userType, Long partnerId, String treePath) {

        boolean isHeadquarters = "HEADQUARTERS".equals(userType);

        BigDecimal scope1MobileCombustion = isHeadquarters
                ? scopeEmissionRepository.sumScope1MobileCombustionEmissionsByYearAndMonthForHeadquarters(headquartersId, year, month)
                : scopeEmissionRepository.sumScope1MobileCombustionEmissionsByYearAndMonthForPartner(headquartersId, partnerId, year, month);

        BigDecimal scope3Category4 = isHeadquarters
                ? scopeEmissionRepository.sumScope3CategoryEmissionsByYearAndMonthForHeadquarters(headquartersId, 4, year, month)
                : scopeEmissionRepository.sumScope3CategoryEmissionsByYearAndMonthForPartner(headquartersId, partnerId, 4, year, month);

        // 하위 조직들의 Cat.4 finalTotal 합계 계산
        BigDecimal childOrganizationsCat4Total = calculateChildOrganizationsCat4Total(
                year, month, headquartersId, userType, partnerId, treePath);

        // finalTotal 계산 - 협력사는 하위 조직 데이터만, 본사는 본인 + 하위 조직
        BigDecimal finalTotal;
        if (isHeadquarters) {
            // 본사: 본인 계산 결과 + 하위 조직들의 Cat.4 finalTotal
            finalTotal = scope1MobileCombustion.add(scope3Category4).add(childOrganizationsCat4Total);
        } else {
            // 협력사: 하위 조직들의 Cat.4 finalTotal만 (본인 데이터는 업스트림용)
            finalTotal = childOrganizationsCat4Total;
        }

        return Scope3SpecialAggregationResponse.Category4Detail.builder()
                .scope1MobileCombustion(scope1MobileCombustion)
                .scope3Category4(isHeadquarters ? scope3Category4.add(childOrganizationsCat4Total) : scope3Category4) // 협력사는 본인 데이터만
                .finalTotal(finalTotal)
                .build();
    }

    /**
     * Cat.5 집계: Scope1 폐수처리 + Scope3 Cat.5 + 하위 조직 Cat.5 finalTotal
     */
    private Scope3SpecialAggregationResponse.Category5Detail calculateCategory5(
            Integer year, Integer month, Long headquartersId, String userType, Long partnerId, String treePath) {

        boolean isHeadquarters = "HEADQUARTERS".equals(userType);

        BigDecimal scope1WasteWater = isHeadquarters
                ? scopeEmissionRepository.sumScope1WasteWaterTreatmentEmissionsByYearAndMonthForHeadquarters(headquartersId, year, month)
                : scopeEmissionRepository.sumScope1WasteWaterTreatmentEmissionsByYearAndMonthForPartner(headquartersId, partnerId, year, month);

        BigDecimal scope3Category5 = isHeadquarters
                ? scopeEmissionRepository.sumScope3CategoryEmissionsByYearAndMonthForHeadquarters(headquartersId, 5, year, month)
                : scopeEmissionRepository.sumScope3CategoryEmissionsByYearAndMonthForPartner(headquartersId, partnerId, 5, year, month);

        // 하위 조직들의 Cat.5 finalTotal 합계 계산
        BigDecimal childOrganizationsCat5Total = calculateChildOrganizationsCat5Total(
                year, month, headquartersId, userType, partnerId, treePath);

        // finalTotal 계산 - 협력사는 하위 조직 데이터만, 본사는 본인 + 하위 조직
        BigDecimal finalTotal;
        if (isHeadquarters) {
            // 본사: 본인 계산 결과 + 하위 조직들의 Cat.5 finalTotal
            finalTotal = scope1WasteWater.add(scope3Category5).add(childOrganizationsCat5Total);
        } else {
            // 협력사: 하위 조직들의 Cat.5 finalTotal만 (본인 데이터는 업스트림용)
            finalTotal = childOrganizationsCat5Total;
        }

        return Scope3SpecialAggregationResponse.Category5Detail.builder()
                .scope1WasteWater(scope1WasteWater)
                .scope3Category5(isHeadquarters ? scope3Category5.add(childOrganizationsCat5Total) : scope3Category5) // 협력사는 본인 데이터만
                .finalTotal(finalTotal)
                .build();
    }

    // ========================================================================
    // 하위 조직의 특수 집계 finalTotal 계산 헬퍼 메서드들
    // ========================================================================

    /**
     * 하위 조직들의 Cat.1 finalTotal 합계 계산
     */
    private BigDecimal calculateChildOrganizationsCat1Total(
            Integer year, Integer month, Long headquartersId, String userType, Long partnerId, String treePath) {
        
        if ("HEADQUARTERS".equals(userType)) {
            // 본사인 경우 모든 협력사의 Cat.1 finalTotal 합계 반환
            List<Long> allChildPartnerIds = scopeEmissionRepository.findAllChildPartnerIds(headquartersId, "/");
            return calculateCat1TotalForPartnerList(allChildPartnerIds, year, month, headquartersId);
        } else if (treePath != null) {
            // 협력사인 경우 하위 조직들의 Cat.1 finalTotal 합계 반환
            List<Long> childPartnerIds = scopeEmissionRepository.findAllChildPartnerIds(headquartersId, treePath);
            return calculateCat1TotalForPartnerList(childPartnerIds, year, month, headquartersId);
        }
        
        return BigDecimal.ZERO;
    }

    /**
     * 하위 조직들의 Cat.2 finalTotal 합계 계산
     */
    private BigDecimal calculateChildOrganizationsCat2Total(
            Integer year, Integer month, Long headquartersId, String userType, Long partnerId, String treePath) {
        
        if ("HEADQUARTERS".equals(userType)) {
            List<Long> allChildPartnerIds = scopeEmissionRepository.findAllChildPartnerIds(headquartersId, "/");
            return calculateCat2TotalForPartnerList(allChildPartnerIds, year, month, headquartersId);
        } else if (treePath != null) {
            List<Long> childPartnerIds = scopeEmissionRepository.findAllChildPartnerIds(headquartersId, treePath);
            return calculateCat2TotalForPartnerList(childPartnerIds, year, month, headquartersId);
        }
        
        return BigDecimal.ZERO;
    }

    /**
     * 하위 조직들의 Cat.4 finalTotal 합계 계산
     */
    private BigDecimal calculateChildOrganizationsCat4Total(
            Integer year, Integer month, Long headquartersId, String userType, Long partnerId, String treePath) {
        
        if ("HEADQUARTERS".equals(userType)) {
            List<Long> allChildPartnerIds = scopeEmissionRepository.findAllChildPartnerIds(headquartersId, "/");
            return calculateCat4TotalForPartnerList(allChildPartnerIds, year, month, headquartersId);
        } else if (treePath != null) {
            List<Long> childPartnerIds = scopeEmissionRepository.findAllChildPartnerIds(headquartersId, treePath);
            return calculateCat4TotalForPartnerList(childPartnerIds, year, month, headquartersId);
        }
        
        return BigDecimal.ZERO;
    }

    /**
     * 하위 조직들의 Cat.5 finalTotal 합계 계산
     */
    private BigDecimal calculateChildOrganizationsCat5Total(
            Integer year, Integer month, Long headquartersId, String userType, Long partnerId, String treePath) {
        
        if ("HEADQUARTERS".equals(userType)) {
            List<Long> allChildPartnerIds = scopeEmissionRepository.findAllChildPartnerIds(headquartersId, "/");
            return calculateCat5TotalForPartnerList(allChildPartnerIds, year, month, headquartersId);
        } else if (treePath != null) {
            List<Long> childPartnerIds = scopeEmissionRepository.findAllChildPartnerIds(headquartersId, treePath);
            return calculateCat5TotalForPartnerList(childPartnerIds, year, month, headquartersId);
        }
        
        return BigDecimal.ZERO;
    }

    // ========================================================================
    // 개별 협력사들의 특수 집계 계산 헬퍼 메서드들
    // ========================================================================

    /**
     * 협력사 목록의 Cat.1 finalTotal 합계 계산
     */
    private BigDecimal calculateCat1TotalForPartnerList(List<Long> partnerIds, Integer year, Integer month, Long headquartersId) {
        return partnerIds.stream()
                .map(pId -> calculateSinglePartnerCat1Total(pId, year, month, headquartersId))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 협력사 목록의 Cat.2 finalTotal 합계 계산
     */
    private BigDecimal calculateCat2TotalForPartnerList(List<Long> partnerIds, Integer year, Integer month, Long headquartersId) {
        return partnerIds.stream()
                .map(pId -> calculateSinglePartnerCat2Total(pId, year, month, headquartersId))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 협력사 목록의 Cat.4 finalTotal 합계 계산
     */
    private BigDecimal calculateCat4TotalForPartnerList(List<Long> partnerIds, Integer year, Integer month, Long headquartersId) {
        return partnerIds.stream()
                .map(pId -> calculateSinglePartnerCat4Total(pId, year, month, headquartersId))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 협력사 목록의 Cat.5 finalTotal 합계 계산
     */
    private BigDecimal calculateCat5TotalForPartnerList(List<Long> partnerIds, Integer year, Integer month, Long headquartersId) {
        return partnerIds.stream()
                .map(pId -> calculateSinglePartnerCat5Total(pId, year, month, headquartersId))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 단일 협력사의 Cat.1 finalTotal 계산
     */
    private BigDecimal calculateSinglePartnerCat1Total(Long partnerId, Integer year, Integer month, Long headquartersId) {
        BigDecimal scope1Total = scopeEmissionRepository.sumScope1TotalEmissionsByYearAndMonthForPartner(headquartersId, partnerId, year, month);
        BigDecimal scope1MobileCombustion = scopeEmissionRepository.sumScope1MobileCombustionEmissionsByYearAndMonthForPartner(headquartersId, partnerId, year, month);
        BigDecimal scope1Factory = scopeEmissionRepository.sumScope1FactoryEmissionsByYearAndMonthForPartner(headquartersId, partnerId, year, month);
        BigDecimal scope1WasteWater = scopeEmissionRepository.sumScope1WasteWaterTreatmentEmissionsByYearAndMonthForPartner(headquartersId, partnerId, year, month);
        BigDecimal scope2Total = scopeEmissionRepository.sumScope2TotalEmissionsByYearAndMonthForPartner(headquartersId, partnerId, year, month);
        BigDecimal scope2Factory = scopeEmissionRepository.sumScope2FactoryEmissionsByYearAndMonthForPartner(headquartersId, partnerId, year, month);
        BigDecimal scope3Category1 = scopeEmissionRepository.sumScope3CategoryEmissionsByYearAndMonthForPartner(headquartersId, partnerId, 1, year, month);

        BigDecimal scope1Remaining = scope1Total.subtract(scope1MobileCombustion).subtract(scope1Factory).subtract(scope1WasteWater);
        BigDecimal scope2Remaining = scope2Total.subtract(scope2Factory);
        
        return scope1Remaining.add(scope2Remaining).add(scope3Category1);
    }

    /**
     * 단일 협력사의 Cat.2 finalTotal 계산
     */
    private BigDecimal calculateSinglePartnerCat2Total(Long partnerId, Integer year, Integer month, Long headquartersId) {
        BigDecimal scope1Factory = scopeEmissionRepository.sumScope1FactoryEmissionsByYearAndMonthForPartner(headquartersId, partnerId, year, month);
        BigDecimal scope2Factory = scopeEmissionRepository.sumScope2FactoryEmissionsByYearAndMonthForPartner(headquartersId, partnerId, year, month);
        BigDecimal scope3Category2 = scopeEmissionRepository.sumScope3CategoryEmissionsByYearAndMonthForPartner(headquartersId, partnerId, 2, year, month);
        
        return scope1Factory.add(scope2Factory).add(scope3Category2);
    }

    /**
     * 단일 협력사의 Cat.4 finalTotal 계산
     */
    private BigDecimal calculateSinglePartnerCat4Total(Long partnerId, Integer year, Integer month, Long headquartersId) {
        BigDecimal scope1MobileCombustion = scopeEmissionRepository.sumScope1MobileCombustionEmissionsByYearAndMonthForPartner(headquartersId, partnerId, year, month);
        BigDecimal scope3Category4 = scopeEmissionRepository.sumScope3CategoryEmissionsByYearAndMonthForPartner(headquartersId, partnerId, 4, year, month);
        
        return scope1MobileCombustion.add(scope3Category4);
    }

    /**
     * 단일 협력사의 Cat.5 finalTotal 계산
     */
    private BigDecimal calculateSinglePartnerCat5Total(Long partnerId, Integer year, Integer month, Long headquartersId) {
        BigDecimal scope1WasteWater = scopeEmissionRepository.sumScope1WasteWaterTreatmentEmissionsByYearAndMonthForPartner(headquartersId, partnerId, year, month);
        BigDecimal scope3Category5 = scopeEmissionRepository.sumScope3CategoryEmissionsByYearAndMonthForPartner(headquartersId, partnerId, 5, year, month);
        
        return scope1WasteWater.add(scope3Category5);
    }
}