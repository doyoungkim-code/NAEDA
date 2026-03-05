package com.ssafy.naeda.domain.point.dto.response;

import com.ssafy.naeda.domain.point.entity.PointWallet;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "포인트 지갑 응답")
public class PointWalletResponse {

    @Schema(description = "지갑 ID", example = "10")
    private Long walletId;

    @Schema(description = "사용자 번호", example = "1")
    private Long userNo;

    @Schema(description = "현재 잔액", example = "1500")
    private Long balance;

    @Schema(description = "총 적립 포인트", example = "3000")
    private Long totalEarned;

    @Schema(description = "총 사용 포인트", example = "1500")
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
