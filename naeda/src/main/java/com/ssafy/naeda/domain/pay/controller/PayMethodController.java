package com.ssafy.naeda.domain.pay.controller;

import com.ssafy.naeda.domain.payment.entity.PaymentMethod;
import com.ssafy.naeda.domain.payment.repository.PaymentMethodRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pay-methods")
@RequiredArgsConstructor
public class PayMethodController {

    private final PaymentMethodRepository paymentMethodRepository;

    /**
     * GET /api/pay-methods?userNo= — 결제수단 목록 조회
     */
    @GetMapping
    public ResponseEntity<List<PaymentMethod>> getPaymentMethods(@RequestParam Long userNo) {
        List<PaymentMethod> methods = paymentMethodRepository.findByUserNoAndIsActiveTrue(userNo);
        return ResponseEntity.ok(methods);
    }

    /**
     * PATCH /api/pay-methods/{id}/face-pay — 페이스페이 결제수단 설정
     */
    @PatchMapping("/{id}/face-pay")
    public ResponseEntity<PaymentMethod> setFacePay(
            @PathVariable Long id,
            @RequestParam Long userNo) {

        // 1. 기존 페이스페이 해제
        paymentMethodRepository.findByUserNoAndIsFacePayTrueAndIsActiveTrue(userNo)
                .ifPresent(PaymentMethod::clearFacePay);

        // 2. 새 결제수단에 페이스페이 설정
        PaymentMethod method = paymentMethodRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("결제 수단을 찾을 수 없습니다."));

        if (!method.getUserNo().equals(userNo)) {
            throw new RuntimeException("본인의 결제 수단만 설정할 수 있습니다.");
        }

        method.setAsFacePay();
        method = paymentMethodRepository.save(method);

        return ResponseEntity.ok(method);
    }
}
