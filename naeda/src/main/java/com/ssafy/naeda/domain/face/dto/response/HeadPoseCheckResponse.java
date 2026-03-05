package com.ssafy.naeda.domain.face.dto.response;

import com.ssafy.naeda.domain.face.client.dto.AiHeadPoseResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "얼굴 방향 검증 응답")
public class HeadPoseCheckResponse {

    @Schema(description = "요청한 기대 방향", example = "left")
    private String expectedDirection;

    @Schema(description = "탐지된 방향", example = "left")
    private String detectedDirection;

    @Schema(description = "기대 방향 일치 여부", example = "true")
    private boolean matched;

    @Schema(description = "좌우 방향 점수(yaw)", example = "-0.24")
    private float yaw;

    @Schema(description = "상하 방향 점수(pitch)", example = "0.03")
    private float pitch;

    @Schema(description = "탐지 신뢰도(0~1)", example = "0.82")
    private float confidence;

    public static HeadPoseCheckResponse from(AiHeadPoseResponse ai) {
        return HeadPoseCheckResponse.builder()
                .expectedDirection(ai.getExpectedDirection())
                .detectedDirection(ai.getDetectedDirection())
                .matched(ai.isMatched())
                .yaw(ai.getYaw())
                .pitch(ai.getPitch())
                .confidence(ai.getConfidence())
                .build();
    }
}

