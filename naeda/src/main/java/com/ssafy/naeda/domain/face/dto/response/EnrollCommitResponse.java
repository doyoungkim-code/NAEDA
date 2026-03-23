package com.ssafy.naeda.domain.face.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "얼굴 등록 커밋 응답")
public class EnrollCommitResponse {

    @Schema(description = "성공 여부", example = "true")
    private boolean success;

    @Schema(description = "등록된 사용자 ID", example = "user-1001")
    private String userId;

    @Schema(description = "DB에 저장된 포즈 목록")
    private List<String> poses;

    @Schema(description = "저장된 포즈 개수", example = "7")
    private int savedCount;

    @Schema(description = "저장 완료 시각", example = "2026-03-23T22:30:00")
    private LocalDateTime savedAt;

    @Schema(description = "안내 메시지", example = "테스트용 얼굴 등록이 DB에 저장되었습니다.")
    private String message;
}
