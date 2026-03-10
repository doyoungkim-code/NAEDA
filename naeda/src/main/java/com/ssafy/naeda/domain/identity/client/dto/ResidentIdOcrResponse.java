package com.ssafy.naeda.domain.identity.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ResidentIdOcrResponse {

    private String name;

    @JsonProperty("residentFront6")
    private String residentFront6;

    @JsonProperty("residentBackFirst1")
    private String residentBackFirst1;

    private String provider;
}
