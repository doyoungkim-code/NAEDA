package com.ssafy.naeda.domain.pay.controller;

import com.ssafy.naeda.domain.pay.dto.request.PayLimitRequest;
import com.ssafy.naeda.domain.pay.dto.response.PayLimitResponse;
import com.ssafy.naeda.domain.pay.service.PayLimitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/pay/limit")
@RequiredArgsConstructor
@Tag(name = "[Pay] 결제 한도", description = "결제 한도 조회/설정 API")
public class PayLimitController {

    private final PayLimitService payLimitService;

    @GetMapping
    @Operation(summary = "결제 한도 조회", description = "사용자의 결제 한도를 조회합니다. 미설정 시 기본값 반환.")
    public ResponseEntity<PayLimitResponse> getLimit(
            @Parameter(description = "사용자 번호", required = true)
            @RequestParam @Positive Long userNo) {
        return ResponseEntity.ok(payLimitService.getLimit(userNo));
    }

    @PutMapping
    @Operation(summary = "결제 한도 설정/수정", description = "1일/월/1회 결제 한도를 설정하거나 수정합니다.")
    public ResponseEntity<PayLimitResponse> setLimit(
            @Parameter(description = "사용자 번호", required = true)
            @RequestParam @Positive Long userNo,
            @Valid @RequestBody PayLimitRequest request) {
        return ResponseEntity.ok(payLimitService.setLimit(userNo, request));
    }
}
