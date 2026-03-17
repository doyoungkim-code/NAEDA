package com.ssafy.naeda.domain.payment.dto.response;

import com.ssafy.naeda.domain.payment.entity.PaymentLimit;

public record PaymentLimitResponse(
        Long userNo,
        Long dailyLimit,
        Long monthlyLimit,
        Long singleTransactionLimit
) {
    public static PaymentLimitResponse from(PaymentLimit paymentLimit) {
        return new PaymentLimitResponse(
                paymentLimit.getUserNo(),
                paymentLimit.getDailyLimit(),
                paymentLimit.getMonthlyLimit(),
                paymentLimit.getSingleTransactionLimit()
        );
    }
}
