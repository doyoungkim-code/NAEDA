package com.ssafy.naeda.domain.account.dto.response;

import com.ssafy.naeda.domain.account.dto.ssafy.SsafyAccountListResponse.SsafyAccountRec;
import com.ssafy.naeda.domain.account.entity.Account;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AccountResponse {

    private Long accountId;
    private String bankCode;
    private String bankName;
    private String accountNo;
    private String accountName;
    private Long accountBalance;
    private String currency;

    public static AccountResponse of (Account account, SsafyAccountRec rec) {
        return AccountResponse.builder()
                .accountId(account.getAccountId())
                .bankCode(rec.getBankCode())
                .bankName(rec.getBankName())
                .accountNo(rec.getAccountNo())
                .accountName(
                        account.getAccountName() != null
                            ? account.getAccountName() : rec.getAccountName()
                )
                .accountBalance(parseLong(rec.getAccountBalance()))
                .currency(rec.getCurrency())
                .build();
    }

    /**
     * SSAFY 응답만으로 DTO 생성 (우리 DB에 아직 저장 안 된 경우).
     */
    public static AccountResponse ofSsafy(SsafyAccountRec rec) {
        return AccountResponse.builder()
                .bankCode(rec.getBankCode())
                .bankName(rec.getBankName())
                .accountNo(rec.getAccountNo())
                .accountName(rec.getAccountName())
                .accountBalance(parseLong(rec.getAccountBalance()))
                .currency(rec.getCurrency())
                .build();
    }

    private static Long parseLong(String value) {
        if (value == null || value.isBlank()) return 0L;
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
}
