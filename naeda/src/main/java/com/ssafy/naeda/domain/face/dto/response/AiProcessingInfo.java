package com.ssafy.naeda.domain.face.dto.response;

import com.ssafy.naeda.domain.face.client.dto.AiEmbeddingResult;
import com.ssafy.naeda.domain.face.client.dto.AiHeadPoseResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "AI 처리 상태 메타데이터")
public class AiProcessingInfo {

    @Schema(description = "AI 처리 상태", example = "COMPLETED")
    private String aiStatus;

    @Schema(description = "폴백 사용 여부", example = "false")
    private boolean fallbackUsed;

    @Schema(description = "클라이언트 재시도 권장 여부", example = "false")
    private boolean retryable;

    @Schema(description = "AI 처리 메시지", example = "Primary inference succeeded.")
    private String message;

    public static AiProcessingInfo from(AiEmbeddingResult result) {
        return AiProcessingInfo.builder()
                .aiStatus(result.getAiStatus())
                .fallbackUsed(result.isFallbackUsed())
                .retryable(false)
                .message(result.getMessage())
                .build();
    }

    public static AiProcessingInfo from(AiHeadPoseResponse response) {
        return AiProcessingInfo.builder()
                .aiStatus(response.getAiStatus())
                .fallbackUsed(response.isFallbackUsed())
                .retryable(false)
                .message(response.getMessage())
                .build();
    }
}
