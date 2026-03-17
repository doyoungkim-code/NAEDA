package com.ssafy.naeda.domain.pay.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder
public class PayRequestResponse {

    private Long requestId;
    private String status;
    private Long storeId;
    private Long userNo;
    private Long amount;
    private Long paymentMethodId;
    private String transactionId;

    public static PayRequestResponse from(Long requestId, Map<Object, Object> data) {
        if (data == null) return null;

        return PayRequestResponse.builder()
                .requestId(requestId)
                .status((String) data.get("status"))
                .storeId(Long.parseLong((String) data.get("storeId")))
                .userNo(Long.parseLong((String) data.get("userNo")))
                .amount(Long.parseLong((String) data.get("amount")))
                .paymentMethodId(Long.parseLong((String) data.get("paymentMethodId")))
                .transactionId((String) data.get("transactionId"))
                .build();
    }
}
