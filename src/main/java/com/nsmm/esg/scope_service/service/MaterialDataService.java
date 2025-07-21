package com.nsmm.esg.scope_service.service;

import com.nsmm.esg.scope_service.dto.response.MaterialDataResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 자재 데이터 서비스
 * 
 * 현대자동차 기준 자동차 제조업 특화 더미 데이터 제공
 * 본사에서 관리하는 핵심 자재들의 ESG 배출량 정보 포함
 */
@Slf4j
@Service
public class MaterialDataService {

    /**
     * 현대자동차 본사용 더미 자재 데이터 생성
     * 자동차 제조업에 실제 사용되는 주요 소재들을 기반으로 구성
     */
    public List<MaterialDataResponse> getHeadquartersDummyData() {
        log.info("현대자동차 본사용 더미 자재 데이터 생성 시작");
        
        List<MaterialDataResponse> dummyData = new ArrayList<>();
        
        // 강재류 (Steel Materials) - 차체 및 구조용
        dummyData.addAll(createSteelMaterials());
        
        // 비철금속 (Non-ferrous Metals) - 경량화 및 특수용도
        dummyData.addAll(createNonFerrousMaterials());
        
        // 플라스틱 및 고분자 (Plastics & Polymers) - 내외장재
        dummyData.addAll(createPlasticMaterials());
        
        // 고무 및 탄성체 (Rubber & Elastomers) - 타이어 및 씰링
        dummyData.addAll(createRubberMaterials());
        
        // 전자부품 소재 (Electronic Components) - 전장부품
        dummyData.addAll(createElectronicMaterials());
        
        // 화학원료 (Chemical Materials) - 도료, 접착제 등
        dummyData.addAll(createChemicalMaterials());
        
        // 유리 및 세라믹 (Glass & Ceramics) - 창유리, 절연체
        dummyData.addAll(createGlassCeramicMaterials());
        
        // 텍스타일 (Textiles) - 시트, 내장재
        dummyData.addAll(createTextileMaterials());
        
        log.info("현대자동차 본사용 더미 자재 데이터 생성 완료: {}개", dummyData.size());
        return dummyData;
    }

    /**
     * 강재류 더미 데이터 생성
     */
    private List<MaterialDataResponse> createSteelMaterials() {
        List<MaterialDataResponse> steelData = new ArrayList<>();
        
        steelData.add(MaterialDataResponse.builder()
                .materialCode("ST001")
                .materialName("냉간압연강판")
                .materialCategory("강재")
                .materialSubCategory("차체용")
                .materialDescription("자동차 차체 외판용 고품질 냉간압연강판")
                .materialSpec("두께: 0.6-2.0mm, 강도: 270-550MPa")
                .emissionFactor(new BigDecimal("2.3"))
                .unit("ton")
                .scopeCategory("SCOPE3")
                .scope3CategoryNumber(1)
                .accessType(MaterialDataResponse.AccessType.full_access)
                .assignmentSource(MaterialDataResponse.AssignmentSource.dummy_data)
                .assignedBy("현대자동차 본사")
                .assignmentReason("차체 외판 제조용 주력 소재")
                .isAssigned(true)
                .isUsed(true)
                .usageCount(150)
                .lastUsedDate("2024-01-15")
                .supplierInfo("포스코, 현대제철")
                .qualityGrade("A")
                .isEcoFriendly(false)
                .build());

        steelData.add(MaterialDataResponse.builder()
                .materialCode("ST002")
                .materialName("열간압연강판")
                .materialCategory("강재")
                .materialSubCategory("프레임용")
                .materialDescription("자동차 프레임 및 구조용 열간압연강판")
                .materialSpec("두께: 2.0-6.0mm, 강도: 300-700MPa")
                .emissionFactor(new BigDecimal("2.1"))
                .unit("ton")
                .scopeCategory("SCOPE3")
                .scope3CategoryNumber(1)
                .accessType(MaterialDataResponse.AccessType.full_access)
                .assignmentSource(MaterialDataResponse.AssignmentSource.dummy_data)
                .assignedBy("현대자동차 본사")
                .assignmentReason("프레임 및 섀시 제조용")
                .isAssigned(true)
                .isUsed(true)
                .usageCount(120)
                .lastUsedDate("2024-01-14")
                .supplierInfo("포스코, 현대제철")
                .qualityGrade("A")
                .isEcoFriendly(false)
                .build());

        steelData.add(MaterialDataResponse.builder()
                .materialCode("ST003")
                .materialName("고장력강판")
                .materialCategory("강재")
                .materialSubCategory("안전부품용")
                .materialDescription("충돌 안전성 향상을 위한 고장력강판")
                .materialSpec("인장강도: 780-1500MPa, 두께: 1.0-3.0mm")
                .emissionFactor(new BigDecimal("2.8"))
                .unit("ton")
                .scopeCategory("SCOPE3")
                .scope3CategoryNumber(1)
                .accessType(MaterialDataResponse.AccessType.full_access)
                .assignmentSource(MaterialDataResponse.AssignmentSource.dummy_data)
                .assignedBy("현대자동차 본사")
                .assignmentReason("안전성 강화용 핵심 소재")
                .isAssigned(true)
                .isUsed(true)
                .usageCount(80)
                .lastUsedDate("2024-01-13")
                .supplierInfo("포스코")
                .qualityGrade("A")
                .isEcoFriendly(false)
                .build());

        steelData.add(MaterialDataResponse.builder()
                .materialCode("ST004")
                .materialName("스테인리스강")
                .materialCategory("강재")
                .materialSubCategory("배기계통용")
                .materialDescription("배기 시스템용 내식성 스테인리스강")
                .materialSpec("SUS304, SUS409L, 두께: 1.0-2.5mm")
                .emissionFactor(new BigDecimal("6.2"))
                .unit("ton")
                .scopeCategory("SCOPE3")
                .scope3CategoryNumber(1)
                .accessType(MaterialDataResponse.AccessType.full_access)
                .assignmentSource(MaterialDataResponse.AssignmentSource.dummy_data)
                .assignedBy("현대자동차 본사")
                .assignmentReason("배기 시스템 내구성 확보")
                .isAssigned(true)
                .isUsed(true)
                .usageCount(45)
                .lastUsedDate("2024-01-12")
                .supplierInfo("포스코, 현대제철")
                .qualityGrade("A")
                .isEcoFriendly(false)
                .build());

        return steelData;
    }

    /**
     * 비철금속 더미 데이터 생성
     */
    private List<MaterialDataResponse> createNonFerrousMaterials() {
        List<MaterialDataResponse> nonFerrousData = new ArrayList<>();
        
        nonFerrousData.add(MaterialDataResponse.builder()
                .materialCode("AL001")
                .materialName("알루미늄합금")
                .materialCategory("비철금속")
                .materialSubCategory("경량화용")
                .materialDescription("자동차 경량화용 고강도 알루미늄합금")
                .materialSpec("6061-T6, 밀도: 2.7g/cm³")
                .emissionFactor(new BigDecimal("11.5"))
                .unit("ton")
                .scopeCategory("SCOPE3")
                .scope3CategoryNumber(1)
                .accessType(MaterialDataResponse.AccessType.full_access)
                .assignmentSource(MaterialDataResponse.AssignmentSource.dummy_data)
                .assignedBy("현대자동차 본사")
                .assignmentReason("연비 향상을 위한 경량화")
                .isAssigned(true)
                .isUsed(true)
                .usageCount(90)
                .lastUsedDate("2024-01-11")
                .supplierInfo("한국알루미늄, 노벨리스")
                .qualityGrade("A")
                .isEcoFriendly(true)
                .build());

        nonFerrousData.add(MaterialDataResponse.builder()
                .materialCode("CU001")
                .materialName("구리선재")
                .materialCategory("비철금속")
                .materialSubCategory("전기용")
                .materialDescription("전기 배선용 고순도 구리선재")
                .materialSpec("순도: 99.9%, 직경: 0.5-5.0mm")
                .emissionFactor(new BigDecimal("4.2"))
                .unit("ton")
                .scopeCategory("SCOPE3")
                .scope3CategoryNumber(1)
                .accessType(MaterialDataResponse.AccessType.full_access)
                .assignmentSource(MaterialDataResponse.AssignmentSource.dummy_data)
                .assignedBy("현대자동차 본사")
                .assignmentReason("전기 시스템 구성")
                .isAssigned(true)
                .isUsed(true)
                .usageCount(200)
                .lastUsedDate("2024-01-10")
                .supplierInfo("LS전선, 대한전선")
                .qualityGrade("A")
                .isEcoFriendly(false)
                .build());

        nonFerrousData.add(MaterialDataResponse.builder()
                .materialCode("ZN001")
                .materialName("아연도금강판")
                .materialCategory("비철금속")
                .materialSubCategory("방청용")
                .materialDescription("차체 부식 방지용 아연도금강판")
                .materialSpec("도금량: 60-275g/m², 두께: 0.8-2.0mm")
                .emissionFactor(new BigDecimal("2.9"))
                .unit("ton")
                .scopeCategory("SCOPE3")
                .scope3CategoryNumber(1)
                .accessType(MaterialDataResponse.AccessType.full_access)
                .assignmentSource(MaterialDataResponse.AssignmentSource.dummy_data)
                .assignedBy("현대자동차 본사")
                .assignmentReason("차체 부식 방지")
                .isAssigned(true)
                .isUsed(true)
                .usageCount(110)
                .lastUsedDate("2024-01-09")
                .supplierInfo("포스코, 현대제철")
                .qualityGrade("A")
                .isEcoFriendly(false)
                .build());

        return nonFerrousData;
    }

    /**
     * 플라스틱 소재 더미 데이터 생성
     */
    private List<MaterialDataResponse> createPlasticMaterials() {
        List<MaterialDataResponse> plasticData = new ArrayList<>();
        
        plasticData.add(MaterialDataResponse.builder()
                .materialCode("PL001")
                .materialName("ABS수지")
                .materialCategory("플라스틱")
                .materialSubCategory("내장재용")
                .materialDescription("내장재용 고품질 ABS 수지")
                .materialSpec("인장강도: 40MPa, 밀도: 1.05g/cm³")
                .emissionFactor(new BigDecimal("3.8"))
                .unit("ton")
                .scopeCategory("SCOPE3")
                .scope3CategoryNumber(1)
                .accessType(MaterialDataResponse.AccessType.full_access)
                .assignmentSource(MaterialDataResponse.AssignmentSource.dummy_data)
                .assignedBy("현대자동차 본사")
                .assignmentReason("내장재 고급화")
                .isAssigned(true)
                .isUsed(true)
                .usageCount(75)
                .lastUsedDate("2024-01-08")
                .supplierInfo("LG화학, 삼성SDI")
                .qualityGrade("A")
                .isEcoFriendly(false)
                .build());

        plasticData.add(MaterialDataResponse.builder()
                .materialCode("PL002")
                .materialName("폴리프로필렌")
                .materialCategory("플라스틱")
                .materialSubCategory("범퍼용")
                .materialDescription("범퍼 및 외장부품용 PP 소재")
                .materialSpec("충격강도: 15kJ/m², 밀도: 0.9g/cm³")
                .emissionFactor(new BigDecimal("1.9"))
                .unit("ton")
                .scopeCategory("SCOPE3")
                .scope3CategoryNumber(1)
                .accessType(MaterialDataResponse.AccessType.full_access)
                .assignmentSource(MaterialDataResponse.AssignmentSource.dummy_data)
                .assignedBy("현대자동차 본사")
                .assignmentReason("범퍼 충격 흡수")
                .isAssigned(true)
                .isUsed(true)
                .usageCount(95)
                .lastUsedDate("2024-01-07")
                .supplierInfo("LG화학, SK케미칼")
                .qualityGrade("A")
                .isEcoFriendly(true)
                .build());

        return plasticData;
    }

    /**
     * 고무 소재 더미 데이터 생성
     */
    private List<MaterialDataResponse> createRubberMaterials() {
        List<MaterialDataResponse> rubberData = new ArrayList<>();
        
        rubberData.add(MaterialDataResponse.builder()
                .materialCode("RB001")
                .materialName("타이어고무")
                .materialCategory("고무")
                .materialSubCategory("타이어용")
                .materialDescription("고성능 타이어 제조용 천연/합성고무")
                .materialSpec("경도: 60-80 Shore A, 내열성: 120℃")
                .emissionFactor(new BigDecimal("3.2"))
                .unit("ton")
                .scopeCategory("SCOPE3")
                .scope3CategoryNumber(1)
                .accessType(MaterialDataResponse.AccessType.full_access)
                .assignmentSource(MaterialDataResponse.AssignmentSource.dummy_data)
                .assignedBy("현대자동차 본사")
                .assignmentReason("타이어 성능 최적화")
                .isAssigned(true)
                .isUsed(true)
                .usageCount(60)
                .lastUsedDate("2024-01-06")
                .supplierInfo("한국타이어, 넥센타이어")
                .qualityGrade("A")
                .isEcoFriendly(false)
                .build());

        return rubberData;
    }

    /**
     * 전자부품 소재 더미 데이터 생성
     */
    private List<MaterialDataResponse> createElectronicMaterials() {
        List<MaterialDataResponse> electronicData = new ArrayList<>();
        
        electronicData.add(MaterialDataResponse.builder()
                .materialCode("BT001")
                .materialName("리튬배터리")
                .materialCategory("전자부품")
                .materialSubCategory("전기차용")
                .materialDescription("전기차용 고용량 리튬이온 배터리")
                .materialSpec("용량: 100kWh, 전압: 400V")
                .emissionFactor(new BigDecimal("12.8"))
                .unit("개")
                .scopeCategory("SCOPE3")
                .scope3CategoryNumber(1)
                .accessType(MaterialDataResponse.AccessType.full_access)
                .assignmentSource(MaterialDataResponse.AssignmentSource.dummy_data)
                .assignedBy("현대자동차 본사")
                .assignmentReason("전기차 핵심 부품")
                .isAssigned(true)
                .isUsed(true)
                .usageCount(25)
                .lastUsedDate("2024-01-05")
                .supplierInfo("SK이노베이션, LG에너지솔루션")
                .qualityGrade("A")
                .isEcoFriendly(true)
                .build());

        electronicData.add(MaterialDataResponse.builder()
                .materialCode("SC001")
                .materialName("반도체칩")
                .materialCategory("전자부품")
                .materialSubCategory("ECU용")
                .materialDescription("엔진제어용 고성능 반도체칩")
                .materialSpec("32bit ARM, 동작온도: -40~125℃")
                .emissionFactor(new BigDecimal("8.5"))
                .unit("개")
                .scopeCategory("SCOPE3")
                .scope3CategoryNumber(1)
                .accessType(MaterialDataResponse.AccessType.full_access)
                .assignmentSource(MaterialDataResponse.AssignmentSource.dummy_data)
                .assignedBy("현대자동차 본사")
                .assignmentReason("엔진 제어 정밀도 향상")
                .isAssigned(true)
                .isUsed(true)
                .usageCount(500)
                .lastUsedDate("2024-01-04")
                .supplierInfo("삼성전자, SK하이닉스")
                .qualityGrade("A")
                .isEcoFriendly(false)
                .build());

        return electronicData;
    }

    /**
     * 화학원료 더미 데이터 생성
     */
    private List<MaterialDataResponse> createChemicalMaterials() {
        List<MaterialDataResponse> chemicalData = new ArrayList<>();
        
        chemicalData.add(MaterialDataResponse.builder()
                .materialCode("PA001")
                .materialName("자동차도료")
                .materialCategory("화학원료")
                .materialSubCategory("도장용")
                .materialDescription("친환경 수성 자동차 도료")
                .materialSpec("고체분: 45%, VOC: 420g/L 이하")
                .emissionFactor(new BigDecimal("2.1"))
                .unit("L")
                .scopeCategory("SCOPE3")
                .scope3CategoryNumber(1)
                .accessType(MaterialDataResponse.AccessType.full_access)
                .assignmentSource(MaterialDataResponse.AssignmentSource.dummy_data)
                .assignedBy("현대자동차 본사")
                .assignmentReason("차체 도장 품질 향상")
                .isAssigned(true)
                .isUsed(true)
                .usageCount(1200)
                .lastUsedDate("2024-01-03")
                .supplierInfo("KCC, 노루페인트")
                .qualityGrade("A")
                .isEcoFriendly(true)
                .build());

        chemicalData.add(MaterialDataResponse.builder()
                .materialCode("AD001")
                .materialName("구조용접착제")
                .materialCategory("화학원료")
                .materialSubCategory("조립용")
                .materialDescription("차체 구조 접착용 고강도 접착제")
                .materialSpec("전단강도: 25MPa, 경화온도: 180℃")
                .emissionFactor(new BigDecimal("3.7"))
                .unit("kg")
                .scopeCategory("SCOPE3")
                .scope3CategoryNumber(1)
                .accessType(MaterialDataResponse.AccessType.full_access)
                .assignmentSource(MaterialDataResponse.AssignmentSource.dummy_data)
                .assignedBy("현대자동차 본사")
                .assignmentReason("차체 강성 향상")
                .isAssigned(true)
                .isUsed(true)
                .usageCount(800)
                .lastUsedDate("2024-01-02")
                .supplierInfo("헨켈, 3M")
                .qualityGrade("A")
                .isEcoFriendly(false)
                .build());

        return chemicalData;
    }

    /**
     * 유리 및 세라믹 더미 데이터 생성
     */
    private List<MaterialDataResponse> createGlassCeramicMaterials() {
        List<MaterialDataResponse> glassData = new ArrayList<>();
        
        glassData.add(MaterialDataResponse.builder()
                .materialCode("GL001")
                .materialName("강화유리")
                .materialCategory("유리")
                .materialSubCategory("창유리용")
                .materialDescription("자동차 창유리용 강화안전유리")
                .materialSpec("두께: 3-6mm, 투과율: 70% 이상")
                .emissionFactor(new BigDecimal("0.9"))
                .unit("m²")
                .scopeCategory("SCOPE3")
                .scope3CategoryNumber(1)
                .accessType(MaterialDataResponse.AccessType.full_access)
                .assignmentSource(MaterialDataResponse.AssignmentSource.dummy_data)
                .assignedBy("현대자동차 본사")
                .assignmentReason("시야 확보 및 안전성")
                .isAssigned(true)
                .isUsed(true)
                .usageCount(300)
                .lastUsedDate("2024-01-01")
                .supplierInfo("한국유리, 피엘케이")
                .qualityGrade("A")
                .isEcoFriendly(false)
                .build());

        return glassData;
    }

    /**
     * 텍스타일 소재 더미 데이터 생성
     */
    private List<MaterialDataResponse> createTextileMaterials() {
        List<MaterialDataResponse> textileData = new ArrayList<>();
        
        textileData.add(MaterialDataResponse.builder()
                .materialCode("TX001")
                .materialName("시트원단")
                .materialCategory("텍스타일")
                .materialSubCategory("시트용")
                .materialDescription("고급 시트용 인조가죽 및 원단")
                .materialSpec("내마모성: 100,000회, 난연성: FMVSS302")
                .emissionFactor(new BigDecimal("1.8"))
                .unit("m²")
                .scopeCategory("SCOPE3")
                .scope3CategoryNumber(1)
                .accessType(MaterialDataResponse.AccessType.full_access)
                .assignmentSource(MaterialDataResponse.AssignmentSource.dummy_data)
                .assignedBy("현대자동차 본사")
                .assignmentReason("시트 품질 향상")
                .isAssigned(true)
                .isUsed(true)
                .usageCount(400)
                .lastUsedDate("2023-12-31")
                .supplierInfo("효성, 코오롱")
                .qualityGrade("A")
                .isEcoFriendly(true)
                .build());

        return textileData;
    }
}