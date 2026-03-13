package com.ssafy.naeda.domain.user.dto.response;

import com.ssafy.naeda.domain.user.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
@Schema(description = "페이스페이 설정 응답")
public class FacePaySettingsResponse {

    @Schema(description = "페이스페이 등록 완료 여부", example = "true")
    private boolean faceRegistered;

    @Schema(description = "PIN 2차 인증 사용 여부", example = "false")
    private boolean secondaryAuthEnabled;

    @Schema(description = "처리 결과 메시지", example = "페이스페이 등록이 완료되었습니다.")
    private String message;

    public static FacePaySettingsResponse from(User user, String message) {
        return FacePaySettingsResponse.builder()
                .faceRegistered(Boolean.TRUE.equals(user.getFaceRegistered()))
                .secondaryAuthEnabled(Boolean.TRUE.equals(user.getSecondaryAuthEnabled()))
                .message(message)
                .build();
    }
}
