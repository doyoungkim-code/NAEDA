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
    @Schema(description = "가장 유사한 사용자 ID", example = "user-1001")
    private String bestUserId;
    @Schema(description = "최고 유사도", example = "0.92")
    private float similarity;
    @Schema(description = "매칭 기준 임계값", example = "0.7")
    private float threshold;
    @Schema(description = "상위 유사 후보 목록")
    private List<CandidateDto> candidates;
}
