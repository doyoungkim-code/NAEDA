package com.ssafy.naeda.domain.consumption.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AiConsumptionCategoryResult(
        @JsonProperty("transactionId")
        String transactionId,
        @JsonProperty("aiCategory")
        String aiCategory,
        @JsonProperty("confidence")
        Double confidence,
        @JsonProperty("fallbackUsed")
        Boolean fallbackUsed
) {
}
