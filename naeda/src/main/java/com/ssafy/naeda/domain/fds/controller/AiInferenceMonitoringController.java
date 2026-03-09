package com.ssafy.naeda.domain.fds.controller;

import com.ssafy.naeda.domain.fds.dto.response.AiInferenceMetricsResponse;
import com.ssafy.naeda.domain.fds.service.AiInferenceMonitoringService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/internal/fds/monitoring")
@RequiredArgsConstructor
@Tag(name = "FDS Monitoring", description = "AI 추론 성능 모니터링 API")
public class AiInferenceMonitoringController {

    private final AiInferenceMonitoringService aiInferenceMonitoringService;

    @GetMapping("/ai")
    @Operation(summary = "AI 추론 성능 메트릭 조회", description = "BE가 집계한 AI 호출 성공/실패/지연/폴백 메트릭을 반환합니다.")
    public ResponseEntity<AiInferenceMetricsResponse> getAiInferenceMetrics() {
        return ResponseEntity.ok(aiInferenceMonitoringService.snapshot());
    }
}
