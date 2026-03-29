package com.ssafy.naeda.domain.point.entity;

import com.ssafy.naeda.global.exception.InsufficientBalanceException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PointWalletTest {

    private PointWallet buildWallet(Long balance) {
        return PointWallet.builder()
                .walletId(1L)
                .userNo(1L)
                .balance(balance)
                .totalEarned(balance)
                .totalUsed(0L)
                .build();
    }

    private PointWallet buildWalletFull(Long balance, Long totalEarned, Long totalUsed) {
        return PointWallet.builder()
                .walletId(1L)
                .userNo(1L)
                .balance(balance)
                .totalEarned(totalEarned)
                .totalUsed(totalUsed)
                .build();
    }

    @Test
    @DisplayName("적립 - 잔액이 정상적으로 증가한다")
    void earn_success() {
        PointWallet wallet = buildWallet(1000L);
        wallet.earn(500L);
        assertThat(wallet.getBalance()).isEqualTo(1500L);
        assertThat(wallet.getTotalEarned()).isEqualTo(1500L);
    }

    @Test
    @DisplayName("적립 - 0 이하 금액은 예외 발생")
    void earn_zeroOrNegative_throws() {
        PointWallet wallet = buildWallet(1000L);
        assertThatThrownBy(() -> wallet.earn(0L))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> wallet.earn(-100L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("적립 - 오버플로우 시 예외 발생")
    void earn_overflow_throws() {
        PointWallet wallet = buildWallet(Long.MAX_VALUE - 100);
        assertThatThrownBy(() -> wallet.earn(200L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("최대치");
    }

    @Test
    @DisplayName("적립 - totalEarned 오버플로우 시 예외 발생")
    void earn_totalEarnedOverflow_throws() {
        PointWallet wallet = buildWalletFull(1000L, Long.MAX_VALUE - 100, 0L);
        assertThatThrownBy(() -> wallet.earn(200L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("누적 적립");
    }

    @Test
    @DisplayName("사용 - 잔액이 정상적으로 차감된다")
    void use_success() {
        PointWallet wallet = buildWallet(1000L);
        wallet.use(300L);
        assertThat(wallet.getBalance()).isEqualTo(700L);
        assertThat(wallet.getTotalUsed()).isEqualTo(300L);
    }

    @Test
    @DisplayName("사용 - 잔액 부족 시 예외 발생")
    void use_insufficientBalance_throws() {
        PointWallet wallet = buildWallet(100L);
        assertThatThrownBy(() -> wallet.use(500L))
                .isInstanceOf(InsufficientBalanceException.class);
    }

    @Test
    @DisplayName("사용 - totalUsed 오버플로우 시 예외 발생")
    void use_totalUsedOverflow_throws() {
        PointWallet wallet = buildWalletFull(1000L, 1000L, Long.MAX_VALUE - 100);
        assertThatThrownBy(() -> wallet.use(200L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("누적 사용");
    }

    @Test
    @DisplayName("충전 - 잔액이 정상적으로 증가한다")
    void charge_success() {
        PointWallet wallet = buildWallet(1000L);
        wallet.charge(2000L);
        assertThat(wallet.getBalance()).isEqualTo(3000L);
        assertThat(wallet.getTotalEarned()).isEqualTo(3000L);
    }

    @Test
    @DisplayName("충전 - 0 이하 금액은 예외 발생")
    void charge_zeroOrNegative_throws() {
        PointWallet wallet = buildWallet(1000L);
        assertThatThrownBy(() -> wallet.charge(0L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("충전 - 오버플로우 시 예외 발생")
    void charge_overflow_throws() {
        PointWallet wallet = buildWallet(Long.MAX_VALUE - 100);
        assertThatThrownBy(() -> wallet.charge(200L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("최대치");
    }

    @Test
    @DisplayName("충전 - totalEarned 오버플로우 시 예외 발생")
    void charge_totalEarnedOverflow_throws() {
        PointWallet wallet = buildWalletFull(1000L, Long.MAX_VALUE - 100, 0L);
        assertThatThrownBy(() -> wallet.charge(200L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("누적 적립");
    }
}
