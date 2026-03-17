package com.ssafy.naeda.domain.pay.controller;

import com.ssafy.naeda.domain.payment.entity.PaymentMethod;
import com.ssafy.naeda.domain.payment.repository.PaymentMethodRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pay-methods")
@RequiredArgsConstructor
@Tag(name = "[Pay] 결제수단", description = "결제수단 조회 및 페이스페이 결제수단 설정 API")
public class PayMethodController {

    private final PaymentMethodRepository paymentMethodRepository;

    @GetMapping
    @Operation(summary = "결제수단 목록 조회", description = "해당 사용자의 활성 결제수단(신용카드/체크카드/계좌) 목록을 조회합니다.")
    public ResponseEntity<List<PaymentMethod>> getPaymentMethods(
            @Parameter(description = "사용자 번호", required = true)
            @RequestParam Long userNo) {
        List<PaymentMethod> methods = paymentMethodRepository.findByUserNoAndIsActiveTrue(userNo);
        return ResponseEntity.ok(methods);
    }

    @PatchMapping("/{id}/face-pay")
    @Operation(summary = "페이스페이 결제수단 설정",
            description = "지정한 결제수단을 페이스페이 기본 결제수단으로 설정합니다. 기존 페이스페이 설정은 자동 해제됩니다.")
    public ResponseEntity<PaymentMethod> setFacePay(
            @Parameter(description = "결제수단 ID") @PathVariable Long id,
            @Parameter(description = "사용자 번호", required = true) @RequestParam Long userNo) {

        paymentMethodRepository.findByUserNoAndIsFacePayTrueAndIsActiveTrue(userNo)
                .ifPresent(PaymentMethod::clearFacePay);

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
