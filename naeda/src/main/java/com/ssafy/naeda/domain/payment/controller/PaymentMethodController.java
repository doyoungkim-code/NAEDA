package com.ssafy.naeda.domain.payment.controller;

import com.ssafy.naeda.domain.payment.entity.PaymentMethod;
import com.ssafy.naeda.domain.payment.service.PaymentMethodService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payment-methods")
@RequiredArgsConstructor
@Tag(name = "PaymentMethod", description = "결제 수단 API")
@Validated
public class PaymentMethodController {

    private final PaymentMethodService paymentMethodService;

    /**
     * GET /api/payment-methods?userNo=1
     * 결제 수단 목록 조회.
     */
    @Operation(summary = "결제 수단 목록 조회")
    @GetMapping
    public ResponseEntity<List<PaymentMethod>> getPaymentMethods(@RequestParam @Positive Long userNo) {
        return ResponseEntity.ok(paymentMethodService.getPaymentMethods(userNo));
    }

    /**
     * PATCH /api/payment-methods/{paymentMethodId}/face-pay?userNo=1
     * 페이스페이 결제 수단 지정.
     * 기존 페이스페이 수단은 자동 해제되고 지정된 수단으로 변경됨.
     */
    @Operation(summary = "페이스페이 결제 수단 지정", description = "페이스페이 결제에 사용할 결제 수단을 지정합니다.")
    @PatchMapping("/{paymentMethodId}/face-pay")
    public ResponseEntity<Void> setFacePayMethod(
            @PathVariable Long paymentMethodId,
            @RequestParam @Positive Long userNo
    ) {
        paymentMethodService.setFacePayMethod(userNo, paymentMethodId);
        return ResponseEntity.noContent().build();
    }
}
