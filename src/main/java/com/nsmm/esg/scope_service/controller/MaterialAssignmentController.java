package com.nsmm.esg.scope_service.controller;

import com.nsmm.esg.scope_service.dto.ApiResponse;
import com.nsmm.esg.scope_service.dto.request.MaterialAssignmentBatchRequest;
import com.nsmm.esg.scope_service.dto.request.MaterialAssignmentRequest;
import com.nsmm.esg.scope_service.dto.response.MaterialAssignmentResponse;
import com.nsmm.esg.scope_service.enums.ErrorCode;
import com.nsmm.esg.scope_service.service.MaterialAssignmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 자재코드 할당 관리 REST API 컨트롤러
 * 
 * 협력사에게 자재코드를 할당하고 관리하는 API를 제공합니다.
 * 
 * 게이트웨이 헤더:
 * - X-USER-TYPE: 사용자 타입 (HEADQUARTERS | PARTNER)
 * - X-COMPANY-NAME: 회사명
 * - X-ACCOUNT-NUMBER: 계정 번호 (HQ001, L1-001 등)
 * - X-HEADQUARTERS-ID: 본사 ID
 * - X-PARTNER-ID: 파트너사 ID (파트너사인 경우만)
 * - X-TREE-PATH: 계층 경로
 * - X-LEVEL: 계층 레벨
 * 
 */
@Tag(name = "MaterialAssignment", description = "자재코드 할당 관리 API")
@Slf4j
@RestController
@RequestMapping("/api/v1/scope/material-assignments")
@RequiredArgsConstructor
public class MaterialAssignmentController {

    private final MaterialAssignmentService materialAssignmentService;

    // 헤더 정보 로깅 유틸리티 메서드
    private void logHeaders(String methodName, String userType, String headquartersId, String partnerId,
                           String treePath) {
        log.debug("=== {} 요청 헤더 정보 ===", methodName);
        log.debug("X-USER-TYPE: {}", userType);
        log.debug("X-HEADQUARTERS-ID: {}", headquartersId);
        log.debug("X-PARTNER-ID: {}", partnerId);
        log.debug("X-TREE-PATH: {}", treePath);
    }

    // ========================================================================
    // 조회 API (Query APIs)
    // ========================================================================

    @Operation(summary = "협력사별 할당된 자재코드 조회", description = "특정 협력사에게 할당된 자재코드 목록을 조회합니다.")
    @GetMapping("/partner/{partnerUuid}")
    public ResponseEntity<ApiResponse<List<MaterialAssignmentResponse>>> getAssignmentsByPartner(
            @PathVariable String partnerUuid,
            @RequestHeader(value = "X-USER-TYPE", required = false) String userType,
            @RequestHeader(value = "X-HEADQUARTERS-ID", required = false) String headquartersId,
            @RequestHeader(value = "X-PARTNER-ID", required = false) String currentPartnerId,
            @RequestHeader(value = "X-TREE-PATH", required = false) String treePath) {

        log.info("협력사 {} 자재코드 할당 목록 조회 요청", partnerUuid);
        logHeaders("협력사별 자재코드 할당 조회", userType, headquartersId, currentPartnerId, treePath);

        try {
            List<MaterialAssignmentResponse> assignments = materialAssignmentService
                    .getAssignmentsByPartner(partnerUuid);

            return ResponseEntity.ok(ApiResponse.success(assignments, 
                    String.format("협력사 %s의 자재코드 할당 목록을 조회했습니다. (총 %d개)", 
                                partnerUuid, assignments.size())));

        } catch (IllegalArgumentException e) {
            log.error("협력사 {} 자재코드 할당 조회 실패: {}", currentPartnerId, e.getMessage());
            if (e.getMessage().contains("권한")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.error(e.getMessage(), ErrorCode.ACCESS_DENIED.getCode()));
            } else {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error(e.getMessage(), ErrorCode.VALIDATION_ERROR.getCode()));
            }
        } catch (Exception e) {
            log.error("협력사 {} 자재코드 할당 조회 중 서버 오류: {}", currentPartnerId, e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("서버 내부 오류가 발생했습니다.", ErrorCode.INTERNAL_SERVER_ERROR.getCode()));
        }
    }

    @Operation(summary = "본사별 모든 자재코드 할당 조회", description = "본사의 모든 자재코드 할당 목록을 조회합니다.")
    @GetMapping("/headquarters")
    public ResponseEntity<ApiResponse<List<MaterialAssignmentResponse>>> getAssignmentsByHeadquarters(
            @RequestHeader(value = "X-USER-TYPE", required = false) String userType,
            @RequestHeader(value = "X-HEADQUARTERS-ID", required = false) String headquartersId,
            @RequestHeader(value = "X-PARTNER-ID", required = false) String partnerId,
            @RequestHeader(value = "X-TREE-PATH", required = false) String treePath) {

        log.info("본사 {} 전체 자재코드 할당 목록 조회 요청", headquartersId);
        logHeaders("본사별 자재코드 할당 조회", userType, headquartersId, partnerId, treePath);

        try {
            Long hqId = Long.parseLong(headquartersId);
            List<MaterialAssignmentResponse> assignments = materialAssignmentService
                    .getAssignmentsByHeadquarters(hqId, userType);

            return ResponseEntity.ok(ApiResponse.success(assignments, 
                    String.format("본사 %s의 전체 자재코드 할당 목록을 조회했습니다. (총 %d개)", 
                                headquartersId, assignments.size())));

        } catch (IllegalArgumentException e) {
            log.error("본사 {} 자재코드 할당 조회 실패: {}", headquartersId, e.getMessage());
            if (e.getMessage().contains("권한")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.error(e.getMessage(), ErrorCode.ACCESS_DENIED.getCode()));
            } else {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error(e.getMessage(), ErrorCode.VALIDATION_ERROR.getCode()));
            }
        } catch (Exception e) {
            log.error("본사 {} 자재코드 할당 조회 중 서버 오류: {}", headquartersId, e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("서버 내부 오류가 발생했습니다.", ErrorCode.INTERNAL_SERVER_ERROR.getCode()));
        }
    }

    // ========================================================================
    // 생성 API (Creation APIs)
    // ========================================================================

    @Operation(summary = "자재코드 할당 생성", description = "협력사에게 자재코드를 할당합니다.")
    @PostMapping
    public ResponseEntity<ApiResponse<MaterialAssignmentResponse>> createAssignment(
            @Valid @RequestBody MaterialAssignmentRequest request,
            @RequestHeader(value = "X-USER-TYPE", required = false) String userType,
            @RequestHeader(value = "X-HEADQUARTERS-ID", required = false) String headquartersId,
            @RequestHeader(value = "X-PARTNER-ID", required = false) String partnerId,
            @RequestHeader(value = "X-TREE-PATH", required = false) String treePath) {

        log.info("자재코드 할당 생성 요청: 협력사 {}, 자재코드 {}", 
                request.getToPartnerId(), request.getMaterialCode());
        logHeaders("자재코드 할당 생성", userType, headquartersId, partnerId, treePath);

        try {
            MaterialAssignmentResponse response = materialAssignmentService
                    .createAssignment(request, userType, headquartersId, partnerId);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(response, 
                            String.format("협력사 %s에 자재코드 %s를 성공적으로 할당했습니다.", 
                                        request.getToPartnerId(), request.getMaterialCode())));

        } catch (IllegalArgumentException e) {
            log.error("자재코드 할당 생성 실패: {}", e.getMessage());
            if (e.getMessage().contains("권한")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.error(e.getMessage(), ErrorCode.ACCESS_DENIED.getCode()));
            } else if (e.getMessage().contains("이미")) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(ApiResponse.error(e.getMessage(), ErrorCode.DUPLICATE_EMISSION_DATA.getCode()));
            } else {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error(e.getMessage(), ErrorCode.VALIDATION_ERROR.getCode()));
            }
        } catch (Exception e) {
            log.error("자재코드 할당 생성 중 서버 오류: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("서버 내부 오류가 발생했습니다.", ErrorCode.INTERNAL_SERVER_ERROR.getCode()));
        }
    }

    @Operation(summary = "자재코드 일괄 할당", description = "협력사에게 여러 자재코드를 일괄 할당합니다.")
    @PostMapping("/batch")
    public ResponseEntity<ApiResponse<List<MaterialAssignmentResponse>>> createBatchAssignments(
            @Valid @RequestBody MaterialAssignmentBatchRequest request,
            @RequestHeader(value = "X-USER-TYPE", required = false) String userType,
            @RequestHeader(value = "X-HEADQUARTERS-ID", required = false) String headquartersId,
            @RequestHeader(value = "X-PARTNER-ID", required = false) String partnerId,
            @RequestHeader(value = "X-TREE-PATH", required = false) String treePath) {

        log.info("자재코드 일괄 할당 요청: 협력사 {}, {}개 자재코드", 
                request.getToPartnerId(), request.getMaterialCodes().size());
        logHeaders("자재코드 일괄 할당", userType, headquartersId, partnerId, treePath);

        try {
            List<MaterialAssignmentResponse> responses = materialAssignmentService
                    .createBatchAssignments(request, userType, headquartersId, partnerId);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(responses, 
                            String.format("협력사 %s에 %d개의 자재코드를 성공적으로 할당했습니다.", 
                                        request.getToPartnerId(), responses.size())));

        } catch (IllegalArgumentException e) {
            log.error("자재코드 일괄 할당 실패: {}", e.getMessage());
            if (e.getMessage().contains("권한")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.error(e.getMessage(), ErrorCode.ACCESS_DENIED.getCode()));
            } else {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error(e.getMessage(), ErrorCode.VALIDATION_ERROR.getCode()));
            }
        } catch (Exception e) {
            log.error("자재코드 일괄 할당 중 서버 오류: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("서버 내부 오류가 발생했습니다.", ErrorCode.INTERNAL_SERVER_ERROR.getCode()));
        }
    }

    // ========================================================================
    // 수정 API (Update APIs)
    // ========================================================================

    @Operation(summary = "자재코드 할당 수정", description = "기존 자재코드 할당 정보를 수정합니다.")
    @PutMapping("/{assignmentId}")
    public ResponseEntity<ApiResponse<MaterialAssignmentResponse>> updateAssignment(
            @PathVariable Long assignmentId,
            @Valid @RequestBody MaterialAssignmentRequest request,
            @RequestHeader(value = "X-USER-TYPE", required = false) String userType,
            @RequestHeader(value = "X-HEADQUARTERS-ID", required = false) String headquartersId,
            @RequestHeader(value = "X-PARTNER-ID", required = false) String partnerId,
            @RequestHeader(value = "X-TREE-PATH", required = false) String treePath) {

        log.info("자재코드 할당 수정 요청: ID {}, 자재코드 {}", assignmentId, request.getMaterialCode());
        logHeaders("자재코드 할당 수정", userType, headquartersId, partnerId, treePath);

        try {
            MaterialAssignmentResponse response = materialAssignmentService
                    .updateAssignment(assignmentId, request);

            return ResponseEntity.ok(ApiResponse.success(response, 
                    String.format("자재코드 할당 %d를 성공적으로 수정했습니다.", assignmentId)));

        } catch (IllegalArgumentException e) {
            log.error("자재코드 할당 수정 실패: {}", e.getMessage());
            if (e.getMessage().contains("권한")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.error(e.getMessage(), ErrorCode.ACCESS_DENIED.getCode()));
            } else if (e.getMessage().contains("찾을 수 없습니다")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error(e.getMessage(), ErrorCode.DATA_NOT_FOUND.getCode()));
            } else if (e.getMessage().contains("이미")) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(ApiResponse.error(e.getMessage(), ErrorCode.DUPLICATE_EMISSION_DATA.getCode()));
            } else {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error(e.getMessage(), ErrorCode.VALIDATION_ERROR.getCode()));
            }
        } catch (Exception e) {
            log.error("자재코드 할당 수정 중 서버 오류: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("서버 내부 오류가 발생했습니다.", ErrorCode.INTERNAL_SERVER_ERROR.getCode()));
        }
    }

    // ========================================================================
    // 삭제 API (Delete APIs)
    // ========================================================================

    @Operation(summary = "자재코드 할당 삭제 가능 여부 확인", 
              description = "자재코드가 Scope 계산기에서 사용 중인지 확인하여 삭제 가능 여부를 판단합니다.")
    @GetMapping("/{assignmentId}/can-delete")
    public ResponseEntity<ApiResponse<Map<String, Object>>> canDeleteAssignment(
            @PathVariable Long assignmentId,
            @RequestHeader(value = "X-USER-TYPE", required = false) String userType,
            @RequestHeader(value = "X-HEADQUARTERS-ID", required = false) String headquartersId,
            @RequestHeader(value = "X-PARTNER-ID", required = false) String partnerId,
            @RequestHeader(value = "X-TREE-PATH", required = false) String treePath) {

        log.info("자재코드 할당 삭제 가능 여부 확인 요청: ID {}", assignmentId);
        logHeaders("자재코드 할당 삭제 가능 여부 확인", userType, headquartersId, partnerId, treePath);

        try {
            Map<String, Object> result = materialAssignmentService
                    .canDeleteAssignment(assignmentId);

            return ResponseEntity.ok(ApiResponse.success(result, 
                    String.format("자재코드 할당 %d의 삭제 가능 여부를 확인했습니다.", assignmentId)));

        } catch (IllegalArgumentException e) {
            log.error("자재코드 할당 삭제 가능 여부 확인 실패: {}", e.getMessage());
            if (e.getMessage().contains("권한")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.error(e.getMessage(), ErrorCode.ACCESS_DENIED.getCode()));
            } else if (e.getMessage().contains("찾을 수 없습니다")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error(e.getMessage(), ErrorCode.DATA_NOT_FOUND.getCode()));
            } else {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error(e.getMessage(), ErrorCode.VALIDATION_ERROR.getCode()));
            }
        } catch (Exception e) {
            log.error("자재코드 할당 삭제 가능 여부 확인 중 서버 오류: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("서버 내부 오류가 발생했습니다.", ErrorCode.INTERNAL_SERVER_ERROR.getCode()));
        }
    }

    @Operation(summary = "자재코드 할당 삭제", description = "자재코드 할당을 삭제합니다.")
    @DeleteMapping("/{assignmentId}")
    public ResponseEntity<ApiResponse<String>> deleteAssignment(
            @PathVariable Long assignmentId,
            @RequestHeader(value = "X-USER-TYPE", required = false) String userType,
            @RequestHeader(value = "X-HEADQUARTERS-ID", required = false) String headquartersId,
            @RequestHeader(value = "X-PARTNER-ID", required = false) String partnerId,
            @RequestHeader(value = "X-TREE-PATH", required = false) String treePath) {

        log.info("자재코드 할당 삭제 요청: ID {}", assignmentId);
        logHeaders("자재코드 할당 삭제", userType, headquartersId, partnerId, treePath);

        try {
            materialAssignmentService.deleteAssignment(assignmentId);

            return ResponseEntity.ok(ApiResponse.success("삭제 완료", 
                    String.format("자재코드 할당 %d를 성공적으로 삭제했습니다.", assignmentId)));

        } catch (IllegalArgumentException e) {
            log.error("자재코드 할당 삭제 실패: {}", e.getMessage());
            if (e.getMessage().contains("권한")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.error(e.getMessage(), ErrorCode.ACCESS_DENIED.getCode()));
            } else if (e.getMessage().contains("찾을 수 없습니다")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error(e.getMessage(), ErrorCode.DATA_NOT_FOUND.getCode()));
            } else {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error(e.getMessage(), ErrorCode.VALIDATION_ERROR.getCode()));
            }
        } catch (Exception e) {
            log.error("자재코드 할당 삭제 중 서버 오류: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("서버 내부 오류가 발생했습니다.", ErrorCode.INTERNAL_SERVER_ERROR.getCode()));
        }
    }
}