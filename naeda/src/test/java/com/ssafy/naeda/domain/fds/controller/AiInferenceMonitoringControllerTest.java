package com.ssafy.naeda.domain.fds.controller;

import com.ssafy.naeda.domain.fds.dto.response.AiInferenceMetricsResponse;
import com.ssafy.naeda.domain.fds.service.AiInferenceMonitoringService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class AiInferenceMonitoringControllerTest {

    @InjectMocks
    private AiInferenceMonitoringController aiInferenceMonitoringController;

    @Mock
    private AiInferenceMonitoringService aiInferenceMonitoringService;

    @Test
    @DisplayName("AI 추론 성능 메트릭을 반환한다")
    void getAiInferenceMetrics_returnsSnapshot() {
        given(aiInferenceMonitoringService.snapshot()).willReturn(
                AiInferenceMetricsResponse.builder()
                        .totalCalls(3)
                        .successCalls(2)
                        .failureCalls(1)
                        .averageLatencyMs(200.0)
                        .endpointCalls(Map.of("embeddings_extract", 2L))
                        .errorCounts(Map.of("headpose_check:AI_TIMEOUT", 1L))
                        .fallbackCounts(Map.of("embeddings_extract", 1L))
                        .build()
        );

        ResponseEntity<AiInferenceMetricsResponse> response = aiInferenceMonitoringController.getAiInferenceMetrics();

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getFailureCalls()).isEqualTo(1);
    }
}
