package com.ssafy.naeda.domain.pay.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayLimitRequest {
    @NotNull
    @Positive
    private Long dailyLimit;

    @NotNull
    @Positive
    private Long monthlyLimit;

    @NotNull
    @Positive
    private Long singleTransactionLimit;
}
