package com.ssafy.naeda.domain.fds.service;

import com.ssafy.naeda.domain.fds.dto.response.AiInferenceMetricsResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AiInferenceMonitoringServiceTest {

    private final AiInferenceMonitoringService aiInferenceMonitoringService = new AiInferenceMonitoringService();

    @Test
    @DisplayName("AI 호출 성공/실패/폴백 메트릭을 집계한다")
    void snapshot_aggregatesMetrics() {
        aiInferenceMonitoringService.recordSuccess("embeddings_extract", 120, false);
        aiInferenceMonitoringService.recordSuccess("embeddings_extract", 180, true);
        aiInferenceMonitoringService.recordFailure("headpose_check", "AI_TIMEOUT", 300);

        AiInferenceMetricsResponse snapshot = aiInferenceMonitoringService.snapshot();

        assertThat(snapshot.getTotalCalls()).isEqualTo(3);
        assertThat(snapshot.getSuccessCalls()).isEqualTo(2);
        assertThat(snapshot.getFailureCalls()).isEqualTo(1);
        assertThat(snapshot.getAverageLatencyMs()).isGreaterThan(0);
        assertThat(snapshot.getEndpointCalls()).containsEntry("embeddings_extract", 2L);
        assertThat(snapshot.getErrorCounts()).containsEntry("headpose_check:AI_TIMEOUT", 1L);
        assertThat(snapshot.getFallbackCounts()).containsEntry("embeddings_extract", 1L);
    }
}
