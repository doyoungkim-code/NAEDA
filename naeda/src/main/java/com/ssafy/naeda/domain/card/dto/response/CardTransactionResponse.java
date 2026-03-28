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
                .merchantName(resolveMerchantName(log))
                .transactionDate(log.getTransacted().toLocalDate().toString())
                .transactionTime(log.getTransacted().toLocalTime().toString())
                .amount(log.getAmount())
                .cardStatus(log.getMemo())
                .build();
    }

    private static String resolveMerchantName(TransactionLog log) {
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
