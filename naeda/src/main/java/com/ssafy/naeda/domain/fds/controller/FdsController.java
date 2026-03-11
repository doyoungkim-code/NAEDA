package com.ssafy.naeda.domain.fds.controller;

import com.ssafy.naeda.domain.fds.dto.response.FdsLogResponse;
import com.ssafy.naeda.domain.fds.entity.FdsLog;
import com.ssafy.naeda.domain.fds.service.FdsRuleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/fds")
@RequiredArgsConstructor
@Validated
@Tag(name = "FDS", description = "FDS 이상거래 탐지 API")
public class FdsController {

    private final FdsRuleService fdsRuleService;

    @GetMapping("/logs")
    @Operation(summary = "사용자별 FDS 로그 조회", description = "특정 사용자의 이상거래 탐지 이력을 조회합니다.")
    public ResponseEntity<List<FdsLogResponse>> getLogsByUserNo(@RequestParam @Positive Long userNo) {
        List<FdsLogResponse> logs = fdsRuleService.getLogsByUserNo(userNo).stream()
                .map(FdsLogResponse::from)
                .toList();
        return ResponseEntity.ok(logs);
    }

    @GetMapping("/logs/payment/{paymentId}")
    @Operation(summary = "결제별 FDS 로그 조회", description = "특정 결제 건의 이상거래 탐지 결과를 조회합니다.")
    public ResponseEntity<FdsLogResponse> getLogByPaymentId(@PathVariable @Positive Long paymentId) {
        FdsLog log = fdsRuleService.getLogByPaymentId(paymentId);
        return ResponseEntity.ok(FdsLogResponse.from(log));
    }
}
