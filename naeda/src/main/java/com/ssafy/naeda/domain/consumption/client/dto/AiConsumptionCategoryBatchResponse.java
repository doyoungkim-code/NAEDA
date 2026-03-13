package com.ssafy.naeda.domain.consumption.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AiConsumptionCategoryBatchResponse(
        @JsonProperty("results")
        List<AiConsumptionCategoryResult> results,
        @JsonProperty("modelVersion")
        String modelVersion
) {
}
