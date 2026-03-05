package com.ssafy.naeda.domain.face.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@Schema(description = "얼굴 검색 응답")
public class SearchResponse {

    @Schema(description = "임계값 이상 매칭 여부", example = "true")
    private boolean matched;
<<<<<<< HEAD
=======
    @Schema(description = "매칭 판정 상태", example = "MATCH")
    private FaceMatchStatus status;
    @Schema(description = "다음 단계 액션", example = "PASS")
    private String nextAction;
>>>>>>> b041ecc5c38cb905e9bbb0e660a2c3b3d41da422
    @Schema(description = "가장 유사한 사용자 ID", example = "user-1001")
    private String bestUserId;
    @Schema(description = "최고 유사도", example = "0.92")
    private float similarity;
<<<<<<< HEAD
    @Schema(description = "매칭 기준 임계값", example = "0.7")
    private float threshold;
=======
    @Schema(description = "MATCH 기준 임계값", example = "0.7")
    private float matchThreshold;
    @Schema(description = "AMBIGUOUS 하한 임계값", example = "0.65")
    private float ambiguousThreshold;
    @Schema(description = "AI 품질 점수(0~1)", example = "0.93")
    private float qualityScore;
    @Schema(description = "AI 추정 yaw(도)", example = "1.2")
    private float yaw;
    @Schema(description = "AI 추정 pitch(도)", example = "-0.8")
    private float pitch;
    @Schema(description = "AI 추정 roll(도)", example = "0.1")
    private float roll;
>>>>>>> b041ecc5c38cb905e9bbb0e660a2c3b3d41da422
    @Schema(description = "상위 유사 후보 목록")
    private List<CandidateDto> candidates;
}
