package com.ssafy.naeda.domain.payment.controller;

import com.ssafy.naeda.domain.payment.dto.request.PaymentLimitRequest;
import com.ssafy.naeda.domain.payment.dto.response.PaymentLimitResponse;
import com.ssafy.naeda.domain.payment.service.PaymentLimitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payment/limit")
@RequiredArgsConstructor
@Validated
@Tag(name = "PaymentLimit", description = "결제 한도 API")
public class PaymentLimitController {

    private final PaymentLimitService paymentLimitService;

    @Operation(summary = "결제 한도 조회", description = "사용자의 결제 한도를 조회합니다. 미설정 시 기본값을 반환합니다.")
    @GetMapping
    public ResponseEntity<PaymentLimitResponse> getLimit(@RequestParam @Positive Long userNo) {
        return ResponseEntity.ok(paymentLimitService.getLimit(userNo));
    }

    @Operation(summary = "결제 한도 설정/수정", description = "1일/월/1회 결제 한도를 설정하거나 수정합니다.")
    @PutMapping
    public ResponseEntity<PaymentLimitResponse> setLimit(
            @RequestParam @Positive Long userNo,
            @Valid @RequestBody PaymentLimitRequest paymentLimitRequest
    ) {
        return ResponseEntity.ok(paymentLimitService.setLimit(userNo, paymentLimitRequest));
    }
}