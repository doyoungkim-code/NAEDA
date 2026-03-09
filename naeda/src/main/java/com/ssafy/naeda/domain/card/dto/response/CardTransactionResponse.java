package com.ssafy.naeda.domain.card.dto.response;

import com.ssafy.naeda.domain.transaction.entity.TransactionLog;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class CardTransactionResponse {

    private long logId;
    private String transactionUniqueNo;
    private String categoryName;
    private String aiCategory;
    private String merchantName;
    private String transactionDate;
    private String transactionTime;
    private Long amount;
    private String cardStatus;

    public static CardTransactionResponse from(TransactionLog log) {
        return builder()
                .logId(log.getLogId())
                .transactionUniqueNo(log.getSsafyTransactionId())
                .categoryName(log.getCategory())
                .aiCategory(log.getAiCategory())
                .merchantName(log.getCounterpart())
                .transactionDate(log.getTransacted().toLocalDate().toString())
                .transactionTime(log.getTransacted().toLocalTime().toString())
                .amount(log.getAmount())
                .cardStatus(log.getMemo())
                .build();
    }
}
