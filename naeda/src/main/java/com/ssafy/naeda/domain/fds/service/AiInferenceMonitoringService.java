package com.ssafy.naeda.domain.fds.service;

import com.ssafy.naeda.domain.fds.dto.response.AiInferenceMetricsResponse;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Service;

@Service
public class AiInferenceMonitoringService {

    private final AtomicLong totalCalls = new AtomicLong();
    private final AtomicLong successCalls = new AtomicLong();
    private final AtomicLong failureCalls = new AtomicLong();
    private final AtomicLong totalLatencyMs = new AtomicLong();
    private final ConcurrentHashMap<String, AtomicLong> endpointCalls = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicLong> errorCounts = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicLong> fallbackCounts = new ConcurrentHashMap<>();

    public void recordSuccess(String endpoint, long latencyMs, boolean fallbackUsed) {
        totalCalls.incrementAndGet();
        successCalls.incrementAndGet();
        totalLatencyMs.addAndGet(latencyMs);
        endpointCalls.computeIfAbsent(endpoint, key -> new AtomicLong()).incrementAndGet();
        if (fallbackUsed) {
            fallbackCounts.computeIfAbsent(endpoint, key -> new AtomicLong()).incrementAndGet();
        }
    }

    public void recordFailure(String endpoint, String errorCode, long latencyMs) {
        totalCalls.incrementAndGet();
        failureCalls.incrementAndGet();
        totalLatencyMs.addAndGet(latencyMs);
        endpointCalls.computeIfAbsent(endpoint, key -> new AtomicLong()).incrementAndGet();
        errorCounts.computeIfAbsent(endpoint + ":" + errorCode, key -> new AtomicLong()).incrementAndGet();
    }

    public AiInferenceMetricsResponse snapshot() {
        long calls = totalCalls.get();
        return AiInferenceMetricsResponse.builder()
                .totalCalls(calls)
                .successCalls(successCalls.get())
                .failureCalls(failureCalls.get())
                .averageLatencyMs(calls == 0 ? 0.0 : (double) totalLatencyMs.get() / calls)
                .endpointCalls(toMap(endpointCalls))
                .errorCounts(toMap(errorCounts))
                .fallbackCounts(toMap(fallbackCounts))
                .build();
    }

    private Map<String, Long> toMap(ConcurrentHashMap<String, AtomicLong> source) {
        Map<String, Long> result = new TreeMap<>();
        source.forEach((key, value) -> result.put(key, value.get()));
        return result;
    }
}
