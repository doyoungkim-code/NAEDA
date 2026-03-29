package com.ssafy.naeda.domain.identity.client.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ResidentIdOcrResponse {

    @JsonProperty("documentType")
    @JsonAlias("document_type")
    private String documentType;

    @JsonProperty("documentMatched")
    @JsonAlias("document_matched")
    private boolean documentMatched;

    private String name;

    @JsonProperty("residentFront6")
    @JsonAlias("resident_front6")
    private String residentFront6;

    @JsonProperty("residentBackFirst1")
    @JsonAlias({"resident_back_first1", "residentBackFirst1"})
    private String residentBackFirst1;

    private String provider;

    private double confidence;

    @JsonProperty("documentConfidence")
    @JsonAlias("document_confidence")
    private double documentConfidence;

    @JsonProperty("nameConfidence")
    @JsonAlias("name_confidence")
    private double nameConfidence;

    @JsonProperty("residentNumberConfidence")
    @JsonAlias("resident_number_confidence")
    private double residentNumberConfidence;

    @JsonProperty("extractionStatus")
    @JsonAlias("extraction_status")
    private String extractionStatus;

    private List<String> warnings;
}
