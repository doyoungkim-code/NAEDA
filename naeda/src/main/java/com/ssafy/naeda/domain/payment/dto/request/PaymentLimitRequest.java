package com.ssafy.naeda.domain.payment.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PaymentLimitRequest {

    @NotNull @Positive
    private Long dailyLimit;

    @NotNull @Positive
    private Long monthlyLimit;

    @NotNull @Positive
    private Long singleTransactionLimit;
}