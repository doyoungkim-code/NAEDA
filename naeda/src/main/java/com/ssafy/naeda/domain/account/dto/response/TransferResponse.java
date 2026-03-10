package com.ssafy.naeda.domain.account.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TransferResponse {

    private String withdrawalAccountNo;
    private String depositAccountNo;
    private Long amount;
    private String transactionDate;
    private String withdrawalTransactionNo;
    private String depositTransactionNo;
}
