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
    private String aiCategory;
    private String ssafyTransactionId;
    private LocalDateTime transacted;

    public static TransactionLogResponse from(TransactionLog log) {
        return TransactionLogResponse.builder()
                .logId(log.getLogId())
                .accountId(log.getAccountId())
                .transactionType(log.getTransactionType())
                .amount(log.getAmount())
                .balanceAfter(log.getBalanceAfter())
                .counterpart(resolveCounterpart(log))
                .memo(log.getMemo())
                .category(log.getCategory())
                .aiCategory(log.getAiCategory())
                .ssafyTransactionId(log.getSsafyTransactionId())
                .transacted(log.getTransacted())
                .build();
    }

    private static String resolveCounterpart(TransactionLog log) {
        String counterpart = log.getCounterpart();
        // counterpart가 숫자(계좌번호)인 경우 memo에서 매장명 추출
        if (counterpart != null && counterpart.matches("\\d{10,}")) {
            String memo = log.getMemo();
            if (memo != null && !memo.isBlank()) {
                return memo.replaceAll("\\s*(페이스페이|카드)\\s*결제$", "");
            }
        }
        return counterpart;
    }
}
