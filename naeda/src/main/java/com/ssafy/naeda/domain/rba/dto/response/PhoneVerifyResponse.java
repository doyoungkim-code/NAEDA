package com.ssafy.naeda.domain.rba.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "전화번호 가운데 4자리 검증 응답")
public class PhoneVerifyResponse {

    @Schema(description = "검증 성공 여부", example = "true")
    private boolean verified;

    @Schema(description = "처리 결과 메시지", example = "전화번호 확인이 완료되었습니다.")
    private String message;
}
