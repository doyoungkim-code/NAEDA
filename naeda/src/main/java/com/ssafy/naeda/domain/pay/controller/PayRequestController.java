package com.ssafy.naeda.domain.pay.controller;

import com.ssafy.naeda.domain.pay.dto.request.PayProcessRequest;
import com.ssafy.naeda.domain.pay.dto.request.PayRequestCreateRequest;
import com.ssafy.naeda.domain.pay.dto.PayRequestResponse;
import com.ssafy.naeda.domain.pay.dto.response.PayTransactionResponse;
import com.ssafy.naeda.domain.pay.entity.PayTransaction;
import com.ssafy.naeda.domain.pay.service.PayFacadeService;
import com.ssafy.naeda.domain.pay.service.PayRequestRedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/payment-requests")
@RequiredArgsConstructor
public class PayRequestController {

    private final PayFacadeService payFacadeService;
    private final PayRequestRedisService payRequestRedisService;

    /**
     * POST /api/payment-requests — 결제 요청 생성 (Redis PENDING)
     */
    @PostMapping
    public ResponseEntity<PayRequestResponse> createRequest(
            @RequestBody PayRequestCreateRequest request) {

        Long requestId = System.currentTimeMillis(); // TODO: ID 생성 전략 (Snowflake 등)

        payRequestRedisService.createRequest(
                requestId,
                request.getStoreId(),
                request.getUserNo(),
                request.getAmount(),
                request.getPaymentMethodId()
        );

        Map<Object, Object> data = payRequestRedisService.getRequest(requestId);
        return ResponseEntity.ok(PayRequestResponse.from(requestId, data));
    }

    /**
     * GET /api/payment-requests/{id} — 결제 요청 상태 조회 (polling)
     */
    @GetMapping("/{id}")
    public ResponseEntity<PayRequestResponse> getRequest(@PathVariable Long id) {

        Map<Object, Object> data = payRequestRedisService.getRequest(id);
        if (data == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(PayRequestResponse.from(id, data));
    }

    /**
     * POST /api/payment-requests/{id}/process — 결제 처리 (얼굴인증 + 계좌이체)
     */
    @PostMapping("/{id}/process")
    public ResponseEntity<PayTransactionResponse> processRequest(
            @PathVariable Long id,
            @RequestPart("request") PayProcessRequest request,
            @RequestPart("faceImage") MultipartFile faceImage) {

        PayTransaction tx = payFacadeService.processAccountPayment(
                id, request.getIdempotencyKey(), faceImage,
                request.getPin()
        );

        return ResponseEntity.ok(PayTransactionResponse.from(tx));
    }



    /**
     * GET /api/payment-requests?storeId= — 매장별 결제 요청 목록
     */
    @GetMapping
    public ResponseEntity<List<PayRequestResponse>> getStoreRequests(
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
