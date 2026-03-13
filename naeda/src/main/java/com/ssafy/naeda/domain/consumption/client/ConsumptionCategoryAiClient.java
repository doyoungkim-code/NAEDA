package com.ssafy.naeda.domain.consumption.client;

import com.ssafy.naeda.domain.consumption.client.dto.AiConsumptionCategoryBatchRequest;
import com.ssafy.naeda.domain.consumption.client.dto.AiConsumptionCategoryBatchResponse;
import com.ssafy.naeda.domain.consumption.client.dto.AiConsumptionCategoryItem;
import com.ssafy.naeda.domain.consumption.client.dto.AiConsumptionCategoryResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ConsumptionCategoryAiClient {

    private final RestClient aiRestClient;

    public Map<String, String> classifyTransactions(List<AiConsumptionCategoryItem> transactions) {
        if (transactions == null || transactions.isEmpty()) {
            return Map.of();
        }

        Map<String, String> fallbackCategories = buildFallbackCategories(transactions);

        try {
            AiConsumptionCategoryBatchResponse response = aiRestClient.post()
                    .uri("/internal/v1/consumption/categories/classify")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new AiConsumptionCategoryBatchRequest(transactions))
                    .retrieve()
                    .onStatus(status -> status.isError(), (req, res) -> {
                        throw new IllegalStateException("Consumption AI request failed: " + res.getStatusCode());
                    })
                    .body(AiConsumptionCategoryBatchResponse.class);

            if (response == null || response.results() == null) {
                log.warn("소비 카테고리 AI 응답이 비어 있어 fallback으로 대체합니다.");
                return fallbackCategories;
            }

            Map<String, String> categories = new HashMap<>(fallbackCategories);
            for (AiConsumptionCategoryResult result : response.results()) {
                if (result == null || result.transactionId() == null || result.aiCategory() == null || result.aiCategory().isBlank()) {
                    continue;
                }
                categories.put(result.transactionId(), result.aiCategory());
            }
            return categories;
        } catch (Exception e) {
            log.warn("소비 카테고리 AI 호출 실패, fallback으로 대체합니다: {}", e.getMessage());
            return fallbackCategories;
        }
    }

    private Map<String, String> buildFallbackCategories(List<AiConsumptionCategoryItem> transactions) {
        Map<String, String> fallbackCategories = new HashMap<>();
        for (AiConsumptionCategoryItem transaction : transactions) {
            if (transaction == null || transaction.transactionId() == null) {
                continue;
            }
            fallbackCategories.put(
                    transaction.transactionId(),
                    ConsumptionCategoryFallbackMapper.mapToServiceCategory(
                            transaction.rawCategory(),
                            transaction.merchantName(),
                            transaction.memo()
                    )
            );
        }
        return fallbackCategories;
    }
}
