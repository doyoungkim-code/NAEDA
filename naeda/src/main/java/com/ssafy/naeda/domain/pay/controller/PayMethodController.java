package com.ssafy.naeda.domain.pay.controller;

import com.ssafy.naeda.domain.pay.entity.PayMethod;
import com.ssafy.naeda.domain.pay.service.PayMethodService;
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

    private final PayMethodService payMethodService;

    @GetMapping
    @Operation(summary = "결제수단 목록 조회", description = "해당 사용자의 활성 결제수단(신용카드/체크카드/계좌) 목록을 조회합니다.")
    public ResponseEntity<List<PayMethod>> getPaymentMethods(
            @Parameter(description = "사용자 번호", required = true)
            @RequestParam Long userNo) {
        return ResponseEntity.ok(payMethodService.getActiveMethods(userNo));
    }

    @PatchMapping("/{id}/default")
    @Operation(summary = "대표 결제수단 설정",
            description = "지정한 결제수단을 대표 결제수단으로 설정합니다. 기존 대표 결제수단은 자동 해제됩니다.")
    public ResponseEntity<PayMethod> setDefault(
            @Parameter(description = "결제수단 ID") @PathVariable Long id,
            @Parameter(description = "사용자 번호", required = true) @RequestParam Long userNo) {
        return ResponseEntity.ok(payMethodService.setDefault(userNo, id));
    }

    @PatchMapping("/{id}/face-pay")
    @Operation(summary = "페이스페이 결제수단 설정",
            description = "지정한 결제수단의 페이스페이 사용 여부를 설정합니다. enabled=true면 기존 페이스페이 설정은 자동 해제되고, enabled=false면 해당 결제수단의 페이스페이 사용만 해제됩니다.")
    public ResponseEntity<PayMethod> setFacePay(
            @Parameter(description = "결제수단 ID") @PathVariable Long id,
            @Parameter(description = "사용자 번호", required = true) @RequestParam Long userNo,
            @Parameter(description = "페이스페이 사용 여부") @RequestParam(defaultValue = "true") boolean enabled) {
        return ResponseEntity.ok(payMethodService.updateFacePay(userNo, id, enabled));
    }
}