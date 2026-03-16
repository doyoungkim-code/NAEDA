package com.ssafy.naeda.domain.pay.controller;

import com.ssafy.naeda.domain.pay.dto.request.PayCardRequest;
import com.ssafy.naeda.domain.pay.dto.response.PayTransactionResponse;
import com.ssafy.naeda.domain.pay.entity.PayTransaction;
import com.ssafy.naeda.domain.pay.service.PayFacadeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/pay")
@RequiredArgsConstructor
public class PayController {

    private final PayFacadeService payFacadeService;

    /**
     * POST /api/payments — 카드 FacePay 결제
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PayTransactionResponse> pay(
            @RequestHeader("X-User-No") Long userNo,
            @RequestPart("request") PayCardRequest request,
            @RequestPart("faceImage") MultipartFile faceImage) {

        PayTransaction tx = payFacadeService.processCardPayment(
                userNo, request.getStoreId(), request.getPaymentMethodId(),
                request.getAmount(), request.getIdempotencyKey(), faceImage,
                request.getPin()
        );
        return ResponseEntity.ok(PayTransactionResponse.from(tx));
    }



    /**
     * GET /api/payments — 결제 내역 목록
     */
    @GetMapping
    public ResponseEntity<List<PayTransactionResponse>> getPayments(
            @RequestHeader("X-User-No") Long userNo) {

        List<PayTransactionResponse> responses = payFacadeService.getPayments(userNo)
                .stream()
                .map(PayTransactionResponse::from)
                .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    /**
     * GET /api/payments/{id} — 결제 상세 조회
     */
    @GetMapping("/{id}")
    public ResponseEntity<PayTransactionResponse> getPayment(@PathVariable Long id) {

        PayTransaction tx = payFacadeService.getPayment(id);
        return ResponseEntity.ok(PayTransactionResponse.from(tx));
    }
}
