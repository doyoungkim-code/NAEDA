package com.ssafy.naeda.domain.pay.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PayCardRequest {
    private Long storeId;
    private Long paymentMethodId;
    private Long amount;
    private String idempotencyKey;
    private String pin;          // 2차인증 PIN (6자리, 선택)
}
