package com.ssafy.naeda.domain.identity.dto.response;

import lombok.Builder;
import lombok.Getter;
import java.util.List;

@Getter
@Builder
public class ResidentIdExtractResponse {
    private String documentType;
    private boolean documentMatched;
    private String name;
    private String residentFront6;
    private String residentBackFirst1;
    private String provider;
    private double confidence;
    private double documentConfidence;
    private double nameConfidence;
    private double residentNumberConfidence;
    private String extractionStatus;
    private List<String> warnings;
}
