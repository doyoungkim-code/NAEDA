package com.ssafy.naeda.domain.consumption.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

public record AiConsumptionCategoryItem(
        @JsonProperty("transactionId")
        String transactionId,
        @JsonProperty("merchantName")
        String merchantName,
        @JsonProperty("rawCategory")
        String rawCategory,
        @JsonProperty("memo")
        String memo,
        @JsonProperty("amount")
        Long amount,
        @JsonProperty("transactedAt")
        LocalDateTime transactedAt
) {
}
