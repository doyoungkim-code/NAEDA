package com.ssafy.naeda.domain.face.client.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AiEmbeddingResult {
    private float[] embedding;
    private float qualityScore;
    private String model;
    private int faceCount;
    private float yaw;
    private float pitch;
    private float roll;
    private boolean fallbackUsed;
    private String aiStatus;
    private String message;
}
