package com.ssafy.naeda.domain.pay.controller;

import com.ssafy.naeda.domain.pay.dto.request.PayProcessRequest;
import com.ssafy.naeda.domain.pay.dto.request.PayRequestCreateRequest;
import com.ssafy.naeda.domain.pay.dto.PayRequestResponse;
import com.ssafy.naeda.domain.pay.dto.response.PayTransactionResponse;
import com.ssafy.naeda.domain.pay.entity.PayTransaction;
import com.ssafy.naeda.domain.pay.service.PayFacadeService;
import com.ssafy.naeda.domain.pay.service.PayRequestRedisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/pay-requests")
@RequiredArgsConstructor
@Tag(name = "[Pay] 결제 요청", description = "POS 단말기 결제 요청 생성/조회/처리 API (Redis 상태머신 + FacePay)")
public class PayRequestController {

    private final PayFacadeService payFacadeService;
    private final PayRequestRedisService payRequestRedisService;

    @PostMapping
    @Operation(summary = "결제 요청 생성",
            description = "POS 단말기가 결제 요청을 생성합니다. Redis에 PENDING 상태로 저장됩니다.")
    public ResponseEntity<PayRequestResponse> createRequest(
            @Valid @RequestBody PayRequestCreateRequest request) {

        Long requestId = System.currentTimeMillis();

        payRequestRedisService.createRequest(
                requestId,
                request.getStoreId(),
                request.getAmount()
        );

        Map<Object, Object> data = payRequestRedisService.getRequest(requestId);
        return ResponseEntity.ok(PayRequestResponse.from(requestId, data));
    }

    @GetMapping("/{id}")
    @Operation(summary = "결제 요청 상태 조회 (polling)",
            description = "결제 요청 ID로 현재 상태를 조회합니다. 앱에서 polling용으로 사용합니다. "
                    + "상태: PENDING -> PROCESSING -> SUCCESS/FAILED/BLOCKED/PAUSED")
    public ResponseEntity<PayRequestResponse> getRequest(
            @Parameter(description = "결제 요청 ID") @PathVariable Long id) {

        Map<Object, Object> data = payRequestRedisService.getRequest(id);
        if (data == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(PayRequestResponse.from(id, data));
    }

    @PostMapping("/{id}/process")
    @Operation(summary = "페이스페이 결제 처리",
            description = "얼굴 인식으로 사용자가 확정된 후 호출합니다. "
                    + "userId로 사용자 조회 → 등록된 FacePay 결제수단 자동 조회 → "
                    + "결제수단 타입에 따라 자동 분기 (신용카드/체크카드 → 카드결제, 계좌 → 계좌이체) → "
                    + "분산락으로 동시 처리 차단.")
    public ResponseEntity<PayTransactionResponse> processRequest(
            @Parameter(description = "결제 요청 ID") @PathVariable Long id,
            @Valid @RequestBody PayProcessRequest request) {

        PayTransaction tx = payFacadeService.processFacePayment(
                id, request.getUserNo(), request.getIdempotencyKey(), request.getPin()
        );

        return ResponseEntity.ok(PayTransactionResponse.from(tx));
    }

    @GetMapping
    @Operation(summary = "매장별 결제 요청 목록",
            description = "특정 매장의 활성 결제 요청 목록을 조회합니다. POS 단말기에서 대기 중인 요청 확인용.")
    public ResponseEntity<List<PayRequestResponse>> getStoreRequests(
            @Parameter(description = "매장 ID", required = true)
            @RequestParam Long storeId) {

        Set<String> requestIds = payRequestRedisService.getStoreRequests(storeId);
        if (requestIds == null || requestIds.isEmpty()) {
            return ResponseEntity.ok(Collections.emptyList());
        }

        List<PayRequestResponse> responses = requestIds.stream()
                .map(idStr -> {
                    Long reqId = Long.parseLong(idStr);
                    Map<Object, Object> data = payRequestRedisService.getRequest(reqId);
                    return data != null ? PayRequestResponse.from(reqId, data) : null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }
}
