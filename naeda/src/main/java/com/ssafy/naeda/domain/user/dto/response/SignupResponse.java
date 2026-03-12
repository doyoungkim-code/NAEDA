package com.ssafy.naeda.domain.user.dto.response;

import com.ssafy.naeda.domain.user.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
@Schema(description = "회원가입 응답")
public class SignupResponse {

    @Schema(description = "유저 번호", example = "1")
    private Long userNo;

    @Schema(description = "로그인 아이디", example = "hong123")
    private String userId;

    @Schema(description = "사용자 이름", example = "홍길동")
    private String username;

    @Schema(description = "SSAFY 금융망 사용자 키", example = "cf1d49ba-663b-495d-9227-fc2643aa7c5e")
    private String userKey;

    @Schema(description = "Access Token")
    private String accessToken;

    @Schema(description = "Refresh Token")
    private String refreshToken;

    @Schema(description = "페이스페이 등록 완료 여부", example = "false")
    private boolean faceRegistered;

    @Schema(description = "PIN 2차 인증 사용 여부", example = "false")
    private boolean secondaryAuthEnabled;

    public static SignupResponse of(User user, String accessToken, String refreshToken) {
        return SignupResponse.builder()
                .userNo(user.getUserNo())
                .userId(user.getUserId())
                .username(user.getUsername())
                .userKey(user.getUserKey())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .faceRegistered(Boolean.TRUE.equals(user.getFaceRegistered()))
                .secondaryAuthEnabled(Boolean.TRUE.equals(user.getSecondaryAuthEnabled()))
                .build();
    }
}
