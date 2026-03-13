package com.ssafy.naeda.domain.consumption.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
import java.util.Map;

public record AiMonthlyInsightRequest(
        @JsonProperty("periodStart")
        LocalDate periodStart,
        @JsonProperty("periodEnd")
        LocalDate periodEnd,
        @JsonProperty("totalSpending")
        Long totalSpending,
        @JsonProperty("categoryBreakdown")
        Map<String, Long> categoryBreakdown,
        @JsonProperty("previousTotalSpending")
        Long previousTotalSpending,
        @JsonProperty("previousCategoryBreakdown")
        Map<String, Long> previousCategoryBreakdown
) {
}
