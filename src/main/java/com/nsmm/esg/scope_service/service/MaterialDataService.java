package com.nsmm.esg.scope_service.service;

import com.nsmm.esg.scope_service.dto.response.MaterialAssignmentResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 자재 데이터 서비스
 * 
 * 현대자동차 기준 자동차 제조업 특화 더미 데이터 제공
 * MaterialAssignment 엔티티 필드만을 사용한 본사 더미 데이터 생성
 */
@Slf4j
@Service
public class MaterialDataService {

    /**
     * 현대자동차 본사용 더미 자재 데이터 생성
     * MaterialAssignment 엔티티 필드만 사용하여 구성
     */
    public List<MaterialAssignmentResponse> getHeadquartersDummyData() {
        log.info("현대자동차 본사용 더미 자재 데이터 생성 시작");
        
        List<MaterialAssignmentResponse> dummyData = new ArrayList<>();
        
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
    private List<MaterialAssignmentResponse> createSteelMaterials() {
        List<MaterialAssignmentResponse> steelData = new ArrayList<>();
        
        steelData.add(MaterialAssignmentResponse.builder()
                .id(1L)
                .headquartersId(1L)
                .fromPartnerId(null) // 본사가 직접 할당
                .toPartnerId("DUMMY_PARTNER_01")
                .fromLevel(0) // 본사 레벨
                .toLevel(1) // 1차 협력사
                .materialCode("ST001")
                .materialName("냉간압연강판")
                .materialCategory("강재")
                .materialDescription("자동차 차체 외판용 고품질 냉간압연강판")
                .isActive(true)
                .isMapped(false)
                .createdAt(LocalDateTime.now().minusDays(30))
                .updatedAt(LocalDateTime.now().minusDays(10))
                .mappingCount(0)
                .activeMappingCount(0L)
                .assignmentInfo("본사 → 협력사(DUMMY_PARTNER_01) : ST001")
                .isModifiable(true)
                .isDeletable(true)
                .build());

        steelData.add(MaterialAssignmentResponse.builder()
                .id(2L)
                .headquartersId(1L)
                .fromPartnerId(null)
                .toPartnerId("DUMMY_PARTNER_02")
                .fromLevel(0)
                .toLevel(1)
                .materialCode("ST002")
                .materialName("열간압연강판")
                .materialCategory("강재")
                .materialDescription("자동차 프레임 및 구조용 열간압연강판")
                .isActive(true)
                .isMapped(false)
                .createdAt(LocalDateTime.now().minusDays(25))
                .updatedAt(LocalDateTime.now().minusDays(8))
                .mappingCount(0)
                .activeMappingCount(0L)
                .assignmentInfo("본사 → 협력사(DUMMY_PARTNER_02) : ST002")
                .isModifiable(true)
                .isDeletable(true)
                .build());

        steelData.add(MaterialAssignmentResponse.builder()
                .id(3L)
                .headquartersId(1L)
                .fromPartnerId(null)
                .toPartnerId("DUMMY_PARTNER_03")
                .fromLevel(0)
                .toLevel(1)
                .materialCode("ST003")
                .materialName("고장력강판")
                .materialCategory("강재")
                .materialDescription("충돌 안전성 향상을 위한 고장력강판")
                .isActive(true)
                .isMapped(false)
                .createdAt(LocalDateTime.now().minusDays(20))
                .updatedAt(LocalDateTime.now().minusDays(5))
                .mappingCount(0)
                .activeMappingCount(0L)
                .assignmentInfo("본사 → 협력사(DUMMY_PARTNER_03) : ST003")
                .isModifiable(true)
                .isDeletable(true)
                .build());

        steelData.add(MaterialAssignmentResponse.builder()
                .id(4L)
                .headquartersId(1L)
                .fromPartnerId(null)
                .toPartnerId("DUMMY_PARTNER_04")
                .fromLevel(0)
                .toLevel(1)
                .materialCode("ST004")
                .materialName("스테인리스강")
                .materialCategory("강재")
                .materialDescription("배기 시스템용 내식성 스테인리스강")
                .isActive(true)
                .isMapped(false)
                .createdAt(LocalDateTime.now().minusDays(18))
                .updatedAt(LocalDateTime.now().minusDays(3))
                .mappingCount(0)
                .activeMappingCount(0L)
                .assignmentInfo("본사 → 협력사(DUMMY_PARTNER_04) : ST004")
                .isModifiable(true)
                .isDeletable(true)
                .build());

        return steelData;
    }

    /**
     * 비철금속 더미 데이터 생성
     */
    private List<MaterialAssignmentResponse> createNonFerrousMaterials() {
        List<MaterialAssignmentResponse> nonFerrousData = new ArrayList<>();
        
        nonFerrousData.add(MaterialAssignmentResponse.builder()
                .id(5L)
                .headquartersId(1L)
                .fromPartnerId(null)
                .toPartnerId("DUMMY_PARTNER_05")
                .fromLevel(0)
                .toLevel(1)
                .materialCode("AL001")
                .materialName("알루미늄합금")
                .materialCategory("비철금속")
                .materialDescription("자동차 경량화용 고강도 알루미늄합금")
                .isActive(true)
                .isMapped(false)
                .createdAt(LocalDateTime.now().minusDays(15))
                .updatedAt(LocalDateTime.now().minusDays(2))
                .mappingCount(0)
                .activeMappingCount(0L)
                .assignmentInfo("본사 → 협력사(DUMMY_PARTNER_05) : AL001")
                .isModifiable(true)
                .isDeletable(true)
                .build());

        nonFerrousData.add(MaterialAssignmentResponse.builder()
                .id(6L)
                .headquartersId(1L)
                .fromPartnerId(null)
                .toPartnerId("DUMMY_PARTNER_06")
                .fromLevel(0)
                .toLevel(1)
                .materialCode("CU001")
                .materialName("구리선재")
                .materialCategory("비철금속")
                .materialDescription("전기 배선용 고순도 구리선재")
                .isActive(true)
                .isMapped(false)
                .createdAt(LocalDateTime.now().minusDays(12))
                .updatedAt(LocalDateTime.now().minusDays(1))
                .mappingCount(0)
                .activeMappingCount(0L)
                .assignmentInfo("본사 → 협력사(DUMMY_PARTNER_06) : CU001")
                .isModifiable(true)
                .isDeletable(true)
                .build());

        nonFerrousData.add(MaterialAssignmentResponse.builder()
                .id(7L)
                .headquartersId(1L)
                .fromPartnerId(null)
                .toPartnerId("DUMMY_PARTNER_07")
                .fromLevel(0)
                .toLevel(1)
                .materialCode("ZN001")
                .materialName("아연도금강판")
                .materialCategory("비철금속")
                .materialDescription("차체 부식 방지용 아연도금강판")
                .isActive(true)
                .isMapped(false)
                .createdAt(LocalDateTime.now().minusDays(10))
                .updatedAt(LocalDateTime.now())
                .mappingCount(0)
                .activeMappingCount(0L)
                .assignmentInfo("본사 → 협력사(DUMMY_PARTNER_07) : ZN001")
                .isModifiable(true)
                .isDeletable(true)
                .build());

        return nonFerrousData;
    }

    /**
     * 플라스틱 소재 더미 데이터 생성
     */
    private List<MaterialAssignmentResponse> createPlasticMaterials() {
        List<MaterialAssignmentResponse> plasticData = new ArrayList<>();
        
        plasticData.add(MaterialAssignmentResponse.builder()
                .id(8L)
                .headquartersId(1L)
                .fromPartnerId(null)
                .toPartnerId("DUMMY_PARTNER_08")
                .fromLevel(0)
                .toLevel(1)
                .materialCode("PL001")
                .materialName("ABS수지")
                .materialCategory("플라스틱")
                .materialDescription("내장재용 고품질 ABS 수지")
                .isActive(true)
                .isMapped(false)
                .createdAt(LocalDateTime.now().minusDays(8))
                .updatedAt(LocalDateTime.now())
                .mappingCount(0)
                .activeMappingCount(0L)
                .assignmentInfo("본사 → 협력사(DUMMY_PARTNER_08) : PL001")
                .isModifiable(true)
                .isDeletable(true)
                .build());

        plasticData.add(MaterialAssignmentResponse.builder()
                .id(9L)
                .headquartersId(1L)
                .fromPartnerId(null)
                .toPartnerId("DUMMY_PARTNER_09")
                .fromLevel(0)
                .toLevel(1)
                .materialCode("PL002")
                .materialName("폴리프로필렌")
                .materialCategory("플라스틱")
                .materialDescription("범퍼 및 외장부품용 PP 소재")
                .isActive(true)
                .isMapped(false)
                .createdAt(LocalDateTime.now().minusDays(6))
                .updatedAt(LocalDateTime.now())
                .mappingCount(0)
                .activeMappingCount(0L)
                .assignmentInfo("본사 → 협력사(DUMMY_PARTNER_09) : PL002")
                .isModifiable(true)
                .isDeletable(true)
                .build());

        return plasticData;
    }

    /**
     * 고무 소재 더미 데이터 생성
     */
    private List<MaterialAssignmentResponse> createRubberMaterials() {
        List<MaterialAssignmentResponse> rubberData = new ArrayList<>();
        
        rubberData.add(MaterialAssignmentResponse.builder()
                .id(10L)
                .headquartersId(1L)
                .fromPartnerId(null)
                .toPartnerId("DUMMY_PARTNER_10")
                .fromLevel(0)
                .toLevel(1)
                .materialCode("RB001")
                .materialName("타이어고무")
                .materialCategory("고무")
                .materialDescription("고성능 타이어 제조용 천연/합성고무")
                .isActive(true)
                .isMapped(false)
                .createdAt(LocalDateTime.now().minusDays(4))
                .updatedAt(LocalDateTime.now())
                .mappingCount(0)
                .activeMappingCount(0L)
                .assignmentInfo("본사 → 협력사(DUMMY_PARTNER_10) : RB001")
                .isModifiable(true)
                .isDeletable(true)
                .build());

        return rubberData;
    }

    /**
     * 전자부품 소재 더미 데이터 생성
     */
    private List<MaterialAssignmentResponse> createElectronicMaterials() {
        List<MaterialAssignmentResponse> electronicData = new ArrayList<>();
        
        electronicData.add(MaterialAssignmentResponse.builder()
                .id(11L)
                .headquartersId(1L)
                .fromPartnerId(null)
                .toPartnerId("DUMMY_PARTNER_11")
                .fromLevel(0)
                .toLevel(1)
                .materialCode("BT001")
                .materialName("리튬배터리")
                .materialCategory("전자부품")
                .materialDescription("전기차용 고용량 리튬이온 배터리")
                .isActive(true)
                .isMapped(false)
                .createdAt(LocalDateTime.now().minusDays(2))
                .updatedAt(LocalDateTime.now())
                .mappingCount(0)
                .activeMappingCount(0L)
                .assignmentInfo("본사 → 협력사(DUMMY_PARTNER_11) : BT001")
                .isModifiable(true)
                .isDeletable(true)
                .build());

        electronicData.add(MaterialAssignmentResponse.builder()
                .id(12L)
                .headquartersId(1L)
                .fromPartnerId(null)
                .toPartnerId("DUMMY_PARTNER_12")
                .fromLevel(0)
                .toLevel(1)
                .materialCode("SC001")
                .materialName("반도체칩")
                .materialCategory("전자부품")
                .materialDescription("엔진제어용 고성능 반도체칩")
                .isActive(true)
                .isMapped(false)
                .createdAt(LocalDateTime.now().minusDays(1))
                .updatedAt(LocalDateTime.now())
                .mappingCount(0)
                .activeMappingCount(0L)
                .assignmentInfo("본사 → 협력사(DUMMY_PARTNER_12) : SC001")
                .isModifiable(true)
                .isDeletable(true)
                .build());

        return electronicData;
    }

    /**
     * 화학원료 더미 데이터 생성
     */
    private List<MaterialAssignmentResponse> createChemicalMaterials() {
        List<MaterialAssignmentResponse> chemicalData = new ArrayList<>();
        
        chemicalData.add(MaterialAssignmentResponse.builder()
                .id(13L)
                .headquartersId(1L)
                .fromPartnerId(null)
                .toPartnerId("DUMMY_PARTNER_13")
                .fromLevel(0)
                .toLevel(1)
                .materialCode("PA001")
                .materialName("자동차도료")
                .materialCategory("화학원료")
                .materialDescription("친환경 수성 자동차 도료")
                .isActive(true)
                .isMapped(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .mappingCount(0)
                .activeMappingCount(0L)
                .assignmentInfo("본사 → 협력사(DUMMY_PARTNER_13) : PA001")
                .isModifiable(true)
                .isDeletable(true)
                .build());

        chemicalData.add(MaterialAssignmentResponse.builder()
                .id(14L)
                .headquartersId(1L)
                .fromPartnerId(null)
                .toPartnerId("DUMMY_PARTNER_14")
                .fromLevel(0)
                .toLevel(1)
                .materialCode("AD001")
                .materialName("구조용접착제")
                .materialCategory("화학원료")
                .materialDescription("차체 구조 접착용 고강도 접착제")
                .isActive(true)
                .isMapped(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .mappingCount(0)
                .activeMappingCount(0L)
                .assignmentInfo("본사 → 협력사(DUMMY_PARTNER_14) : AD001")
                .isModifiable(true)
                .isDeletable(true)
                .build());

        return chemicalData;
    }

    /**
     * 유리 및 세라믹 더미 데이터 생성
     */
    private List<MaterialAssignmentResponse> createGlassCeramicMaterials() {
        List<MaterialAssignmentResponse> glassData = new ArrayList<>();
        
        glassData.add(MaterialAssignmentResponse.builder()
                .id(15L)
                .headquartersId(1L)
                .fromPartnerId(null)
                .toPartnerId("DUMMY_PARTNER_15")
                .fromLevel(0)
                .toLevel(1)
                .materialCode("GL001")
                .materialName("강화유리")
                .materialCategory("유리")
                .materialDescription("자동차 창유리용 강화안전유리")
                .isActive(true)
                .isMapped(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .mappingCount(0)
                .activeMappingCount(0L)
                .assignmentInfo("본사 → 협력사(DUMMY_PARTNER_15) : GL001")
                .isModifiable(true)
                .isDeletable(true)
                .build());

        return glassData;
    }

    /**
     * 텍스타일 소재 더미 데이터 생성
     */
    private List<MaterialAssignmentResponse> createTextileMaterials() {
        List<MaterialAssignmentResponse> textileData = new ArrayList<>();
        
        textileData.add(MaterialAssignmentResponse.builder()
                .id(16L)
                .headquartersId(1L)
                .fromPartnerId(null)
                .toPartnerId("DUMMY_PARTNER_16")
                .fromLevel(0)
                .toLevel(1)
                .materialCode("TX001")
                .materialName("시트원단")
                .materialCategory("텍스타일")
                .materialDescription("고급 시트용 인조가죽 및 원단")
                .isActive(true)
                .isMapped(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .mappingCount(0)
                .activeMappingCount(0L)
                .assignmentInfo("본사 → 협력사(DUMMY_PARTNER_16) : TX001")
                .isModifiable(true)
                .isDeletable(true)
                .build());

        return textileData;
    }
}