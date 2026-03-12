package com.ssafy.naeda.domain.fds.dto.response;

import java.util.Map;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AiInferenceMetricsResponse {
    private long totalCalls;
    private long successCalls;
    private long failureCalls;
    private double averageLatencyMs;
    private Map<String, Long> endpointCalls;
    private Map<String, Long> errorCounts;
    private Map<String, Long> fallbackCounts;
}
