package com.ssafy.naeda.domain.pay.dto.request;

import com.ssafy.naeda.domain.face.dto.response.FaceMatchStatus;
import com.ssafy.naeda.domain.rba.dto.AuthMethod;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PayProcessRequest {
    @NotNull
    private Long userNo;             // 얼굴 인식으로 확정된 사용자 번호

    private String idempotencyKey;

    private String pin;              // 2차인증 PIN (6자리, 선택)
    private String phoneMiddleDigits;
    private FaceMatchStatus faceStatus;
    private Boolean livenessPassed;
    private AuthMethod selectedAuthMethod;
    private Boolean signatureConfirmed;
}
