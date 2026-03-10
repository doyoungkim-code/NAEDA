package com.ssafy.naeda.domain.identity.dto.response;

import lombok.Builder;
import lombok.Getter;

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
}
