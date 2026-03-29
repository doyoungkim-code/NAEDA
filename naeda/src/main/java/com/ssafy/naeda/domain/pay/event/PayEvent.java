package com.ssafy.naeda.domain.pay.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayEvent {

    private Long transactionId;
    private Long userNo;
    private Long storeId;
    private Long amount;
    private String status;
    private String authMethod;
    private Integer fdsScore;
    private String fdsAction;
    private Long earnedPoints;
    private String ssafyTransactionId;
}
