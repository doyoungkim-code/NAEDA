package com.ssafy.naeda.domain.point.dto.response;

import com.ssafy.naeda.domain.point.entity.PointWallet;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PointWalletResponse {

    private Long walletId;

    private Long userNo;

    private Long balance;

    private Long totalEarned;

    private Long totalUsed;

    public static PointWalletResponse from(PointWallet wallet) {
        return PointWalletResponse.builder()
                .walletId(wallet.getWalletId())
                .userNo(wallet.getUserNo())
                .balance(wallet.getBalance())
                .totalEarned(wallet.getTotalEarned())
                .totalUsed(wallet.getTotalUsed())
                .build();
    }
}
