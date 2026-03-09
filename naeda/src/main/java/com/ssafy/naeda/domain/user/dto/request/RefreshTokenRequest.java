package com.ssafy.naeda.domain.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "토큰 재발급 요청")
public class RefreshTokenRequest {

    @NotBlank
    @Schema(description = "리프레시 토큰", example = "eyjdiqwi~")
    private String refreshToken;
}
