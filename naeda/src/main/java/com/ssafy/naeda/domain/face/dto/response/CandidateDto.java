package com.ssafy.naeda.domain.face.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "얼굴 검색 후보 정보")
public class CandidateDto {
    @Schema(description = "후보 사용자 ID", example = "user-1001")
    private String userId;
    @Schema(description = "후보 포즈", example = "left")
    private String pose;
    @Schema(description = "후보 유사도", example = "0.88")
    private float similarity;
}
