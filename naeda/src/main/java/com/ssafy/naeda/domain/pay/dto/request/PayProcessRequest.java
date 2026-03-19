package com.ssafy.naeda.domain.pay.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PayProcessRequest {
    @NotBlank
    private String userId;           // 얼굴 인식으로 확정된 사용자 ID

    @NotBlank
    private String idempotencyKey;

    private String pin;              // 2차인증 PIN (6자리, 선택)
}
