package com.ssafy.naeda.domain.consumption.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record AiConsumptionCategoryBatchRequest(
        @JsonProperty("transactions")
        List<AiConsumptionCategoryItem> transactions
) {
}
