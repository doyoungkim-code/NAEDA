package com.ssafy.naeda.domain.pay.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PayRequestCreateRequest {
    private Long storeId;
    private Long userNo;
    private Long amount;
    private Long paymentMethodId;
}
