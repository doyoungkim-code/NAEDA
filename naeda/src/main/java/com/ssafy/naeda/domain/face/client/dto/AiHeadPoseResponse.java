package com.ssafy.naeda.domain.face.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class AiHeadPoseResponse {

    @JsonProperty("expectedDirection")
    private String expectedDirection;

    @JsonProperty("detectedDirection")
    private String detectedDirection;

    private boolean matched;
    private float yaw;
    private float pitch;
    private float confidence;
}

