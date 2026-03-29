package com.ssafy.naeda.domain.fds.dto.request;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class FdsEvaluationRequest {

    private final Long userNo;
    private final Long storeId;
    private final Long amount;

    private final LocalDateTime paymentTime;

    private final int recentPaymentCount;

    private final long dailyAverageAmount;
}
