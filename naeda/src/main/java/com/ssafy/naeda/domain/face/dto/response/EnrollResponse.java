package com.ssafy.naeda.domain.face.dto.response;

import com.ssafy.naeda.domain.face.entity.FaceEmbedding;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@Schema(description = "얼굴 등록 응답")
public class EnrollResponse {

    @Schema(description = "성공 여부", example = "true")
    private boolean success;
    @Schema(description = "등록된 사용자 ID", example = "user-1001")
    private String userId;
    @Schema(description = "등록된 포즈", example = "front")
    private String pose;
    @Schema(description = "저장 시각", example = "2026-03-04T11:20:30")
    private LocalDateTime savedAt;
    @Schema(description = "AI 처리 메타데이터")
    private AiProcessingInfo aiProcessing;

    public static EnrollResponse from(FaceEmbedding entity, AiProcessingInfo aiProcessing) {
        return EnrollResponse.builder()
                .success(true)
                .userId(entity.getUserId())
                .pose(entity.getPose())
                .savedAt(entity.getUpdatedAt())
                .aiProcessing(aiProcessing)
                .build();
    }
}
