package com.ssafy.naeda.domain.account.dto.ssafy;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class SsafyAccountListResponse {

    @JsonProperty("REC")
    private List<SsafyAccountRec> rec;

    @Getter
    @NoArgsConstructor
    public static class SsafyAccountRec {

        private String bankCode;
        private String bankName;
        private String userName;
        private String accountNo;
        private String accountName;
        private String accountTypeCode;
        private String accountTypeName;
        private String accountCreatedDate;
        private String accountExpiryDate;
        private String dailyTransferLimit;
        private String oneTimeTransferLimit;
        private String accountBalance;
        private String lastTransactionDate;
        private String currency;

    }
}
