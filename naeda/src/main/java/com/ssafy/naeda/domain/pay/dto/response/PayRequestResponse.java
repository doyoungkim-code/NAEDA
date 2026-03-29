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
    private Long amount;
    private String transactionId;
    private String failureReason;

    public static PayRequestResponse from(Long requestId, Map<Object, Object> data) {
        if (data == null) return null;

        return PayRequestResponse.builder()
                .requestId(requestId)
                .status((String) data.get("status"))
                .storeId(parseLong(data.get("storeId")))
                .amount(parseLong(data.get("amount")))
                .transactionId((String) data.get("transactionId"))
                .failureReason((String) data.get("failureReason"))
                .build();
    }

    private static Long parseLong(Object value) {
        if (value == null) return null;
        try {
            return Long.parseLong((String) value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
