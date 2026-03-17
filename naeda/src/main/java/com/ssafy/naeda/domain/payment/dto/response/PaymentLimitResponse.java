package com.ssafy.naeda.domain.payment.dto.response;

import com.ssafy.naeda.domain.payment.entity.PaymentLimit;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PaymentLimitResponse {

    private Long userNo;
    private Long dailyLimit;
    private Long monthlyLimit;
    private Long singleTransactionLimit;

    public static PaymentLimitResponse from (PaymentLimit paymentLimit) {
        return PaymentLimitResponse.builder()
                .userNo(paymentLimit.getUserNo())
                .dailyLimit(paymentLimit.getDailyLimit())
                .monthlyLimit(paymentLimit.getMonthlyLimit())
                .singleTransactionLimit(paymentLimit.getSingleTransactionLimit())
                .build();
    }
}
