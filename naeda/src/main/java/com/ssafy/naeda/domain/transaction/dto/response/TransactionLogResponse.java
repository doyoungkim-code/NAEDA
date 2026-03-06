package com.ssafy.naeda.domain.transaction.dto.response;

import com.ssafy.naeda.domain.transaction.entity.TransactionLog;
import com.ssafy.naeda.domain.transaction.entity.TransactionType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class TransactionLogResponse {

    private Long logId;
    private Long accountId;
    private TransactionType transactionType;
    private Long amount;
    private Long balanceAfter;
    private String counterpart;
    private String memo;
    private String category;
    private String ssafyTransactionId;
    private LocalDateTime transacted;

    public static TransactionLogResponse from(TransactionLog log) {
        return TransactionLogResponse.builder()
                .logId(log.getLogId())
                .accountId(log.getAccountId())
                .transactionType(log.getTransactionType())
                .amount(log.getAmount())
                .balanceAfter(log.getBalanceAfter())
                .counterpart(log.getCounterpart())
                .memo(log.getMemo())
                .category(log.getCategory())
                .ssafyTransactionId(log.getSsafyTransactionId())
                .transacted(log.getTransacted())
                .build();
    }
}
