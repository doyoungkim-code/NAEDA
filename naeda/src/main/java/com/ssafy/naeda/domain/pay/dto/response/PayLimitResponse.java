package com.ssafy.naeda.domain.pay.dto.response;

import com.ssafy.naeda.domain.pay.entity.PayLimit;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PayLimitResponse {

    private Long userNo;
    private Long dailyLimit;
    private Long monthlyLimit;
    private Long singleTransactionLimit;

    public static PayLimitResponse from(PayLimit limit) {
        return new PayLimitResponse(
                limit.getUserNo(),
                limit.getDailyLimit(),
                limit.getMonthlyLimit(),
                limit.getSingleTransactionLimit());
    }
}
