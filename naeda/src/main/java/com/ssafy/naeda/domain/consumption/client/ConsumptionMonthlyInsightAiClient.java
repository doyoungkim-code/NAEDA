package com.ssafy.naeda.domain.consumption.client;

import com.ssafy.naeda.domain.consumption.client.dto.AiMonthlyInsightRequest;
import com.ssafy.naeda.domain.consumption.client.dto.AiMonthlyInsightResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ConsumptionMonthlyInsightAiClient {

    private final RestClient aiRestClient;

    public List<String> generateMonthlyInsights(
            LocalDate periodStart,
            LocalDate periodEnd,
            Long totalSpending,
            Map<String, Long> categoryBreakdown,
            Long previousTotalSpending,
            Map<String, Long> previousCategoryBreakdown
    ) {
        try {
            AiMonthlyInsightResponse response = aiRestClient.post()
                    .uri("/internal/v1/consumption/reports/monthly/insights")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new AiMonthlyInsightRequest(
                            periodStart,
                            periodEnd,
                            totalSpending,
                            categoryBreakdown,
                            previousTotalSpending,
                            previousCategoryBreakdown
                    ))
                    .retrieve()
                    .onStatus(status -> status.isError(), (req, res) -> {
                        throw new IllegalStateException("Consumption monthly insight AI request failed: " + res.getStatusCode());
                    })
                    .body(AiMonthlyInsightResponse.class);

            if (response == null || response.insights() == null || response.insights().isEmpty()) {
                log.warn("월간 소비 인사이트 AI 응답이 비어 있어 fallback을 사용합니다.");
                return ConsumptionMonthlyInsightFallbackGenerator.generate(categoryBreakdown, totalSpending);
            }
            return response.insights();
        } catch (Exception e) {
            log.warn("월간 소비 인사이트 AI 호출 실패, fallback을 사용합니다: {}", e.getMessage());
            return ConsumptionMonthlyInsightFallbackGenerator.generate(categoryBreakdown, totalSpending);
        }
    }
}
