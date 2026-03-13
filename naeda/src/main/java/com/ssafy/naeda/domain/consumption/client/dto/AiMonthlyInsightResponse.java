package com.ssafy.naeda.domain.consumption.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AiMonthlyInsightResponse(
        @JsonProperty("insights")
        List<String> insights,
        @JsonProperty("modelVersion")
        String modelVersion
) {
}
