package com.ssafy.naeda.domain.identity.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ResidentIdOcrResponse {

    @JsonProperty("documentType")
    private String documentType;

    @JsonProperty("documentMatched")
    private boolean documentMatched;

    private String name;

    @JsonProperty("residentFront6")
    private String residentFront6;

    @JsonProperty("residentBackFirst1")
    private String residentBackFirst1;

    private String provider;

    private double confidence;

    @JsonProperty("documentConfidence")
    private double documentConfidence;

    @JsonProperty("nameConfidence")
    private double nameConfidence;

    @JsonProperty("residentNumberConfidence")
    private double residentNumberConfidence;

    @JsonProperty("extractionStatus")
    private String extractionStatus;

    private List<String> warnings;
}
