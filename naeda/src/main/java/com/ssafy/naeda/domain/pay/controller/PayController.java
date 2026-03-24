package com.ssafy.naeda.domain.pay.controller;

import com.ssafy.naeda.domain.pay.dto.response.CurrentMonthSpendingAnalysisResponse;
import com.ssafy.naeda.domain.pay.dto.response.PayTransactionResponse;
import com.ssafy.naeda.domain.pay.service.PayFacadeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/pay")
@RequiredArgsConstructor
@Tag(name = "[Pay] 결제 내역", description = "결제 내역 조회 API")
public class PayController {

    private final PayFacadeService payFacadeService;

    @GetMapping
    @Operation(summary = "결제 내역 목록 조회",
            description = "해당 사용자의 전체 결제 내역을 최신순으로 조회합니다. from/to로 기간 필터링 가능.")
    public ResponseEntity<List<PayTransactionResponse>> getPayments(
            @Parameter(description = "사용자 번호", required = true)
            @RequestHeader("X-User-No") Long userNo,
            @Parameter(description = "조회 시작 일시 (ISO 8601)")
            @RequestParam(required = false) LocalDateTime from,
            @Parameter(description = "조회 종료 일시 (ISO 8601)")
            @RequestParam(required = false) LocalDateTime to) {
        return ResponseEntity.ok(payFacadeService.getPaymentResponses(userNo, from, to));
    }

    @GetMapping("/{id}")
    @Operation(summary = "결제 상세 조회",
            description = "결제 ID로 단건 상세 조회합니다. 본인의 결제 내역만 조회 가능.")
    public ResponseEntity<PayTransactionResponse> getPayment(
            @Parameter(description = "사용자 번호", required = true)
            @RequestHeader("X-User-No") Long userNo,
            @Parameter(description = "결제 트랜잭션 ID") @PathVariable Long id) {
        return ResponseEntity.ok(payFacadeService.getPaymentResponse(userNo, id));
    }

    @GetMapping("/analysis/current-month")
    @Operation(
            summary = "이번 달 소비 분석 조회",
            description = "해당 사용자의 이번 달 성공 결제 내역을 카테고리별로 집계하고 분석 문구를 반환합니다."
    )
    public ResponseEntity<CurrentMonthSpendingAnalysisResponse> getCurrentMonthSpendingAnalysis(
            @Parameter(description = "사용자 번호", required = true)
            @RequestHeader("X-User-No") Long userNo) {
        return ResponseEntity.ok(payFacadeService.getCurrentMonthSpendingAnalysis(userNo));
    }
}
