package com.ssafy.naeda.domain.point.repository;

import com.ssafy.naeda.domain.point.entity.PointHistory;
import com.ssafy.naeda.domain.point.entity.PointType;
import com.ssafy.naeda.domain.point.entity.PointWallet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PointHistoryRepositoryTest {

    @Autowired
    private PointHistoryRepository pointHistoryRepository;

    @Autowired
    private PointWalletRepository pointWalletRepository;

    private PointWallet savedWallet;

    @BeforeEach
    void setUp() {
        pointHistoryRepository.deleteAll();
        pointWalletRepository.deleteAll();
        savedWallet = pointWalletRepository.save(
                PointWallet.builder()
                        .userNo(1L)
                        .balance(1000L)
                        .totalEarned(1000L)
                        .totalUsed(0L)
                        .build()
        );
    }

    @Test
    @DisplayName("포인트 이력 저장 및 조회")
    void saveAndFind() {
        PointHistory history = pointHistoryRepository.save(
                PointHistory.builder()
                        .walletId(savedWallet.getWalletId())
                        .type(PointType.EARN)
                        .amount(500L)
                        .balanceAfter(1500L)
                        .description("결제 적립")
                        .build()
        );

        PointHistory found = pointHistoryRepository.findById(history.getHistoryId()).orElseThrow();
        assertThat(found.getType()).isEqualTo(PointType.EARN);
        assertThat(found.getAmount()).isEqualTo(500L);
        assertThat(found.getBalanceAfter()).isEqualTo(1500L);
        assertThat(found.getCreated()).isNotNull();
    }

    @Test
    @DisplayName("walletId로 이력 조회")
    void findByWalletId() {
        pointHistoryRepository.save(
                PointHistory.builder()
                        .walletId(savedWallet.getWalletId())
                        .type(PointType.EARN)
                        .amount(500L)
                        .balanceAfter(1500L)
                        .description("결제 적립")
                        .build()
        );
        pointHistoryRepository.save(
                PointHistory.builder()
                        .walletId(savedWallet.getWalletId())
                        .type(PointType.USE_COUPON)
                        .amount(200L)
                        .balanceAfter(1300L)
                        .description("쿠폰 사용")
                        .build()
        );

        List<PointHistory> histories = pointHistoryRepository.findAll();
        List<PointHistory> walletHistories = histories.stream()
                .filter(h -> h.getWalletId().equals(savedWallet.getWalletId()))
                .toList();

        assertThat(walletHistories).hasSize(2);
    }

    @Test
    @DisplayName("paymentId가 null 허용되는지 확인")
    void paymentIdNullable() {
        PointHistory history = pointHistoryRepository.save(
                PointHistory.builder()
                        .walletId(savedWallet.getWalletId())
                        .type(PointType.CHARGE)
                        .amount(10000L)
                        .balanceAfter(11000L)
                        .description("포인트 충전")
                        .build()
        );

        PointHistory found = pointHistoryRepository.findById(history.getHistoryId()).orElseThrow();
        assertThat(found.getPaymentId()).isNull();
    }

    @Test
    @DisplayName("PointType enum 값 정상 저장")
    void enumTypePersistence() {
        for (PointType type : PointType.values()) {
            pointHistoryRepository.save(
                    PointHistory.builder()
                            .walletId(savedWallet.getWalletId())
                            .type(type)
                            .amount(100L)
                            .balanceAfter(1000L)
                            .build()
            );
        }

        List<PointHistory> all = pointHistoryRepository.findAll();
        assertThat(all).hasSize(3);
        assertThat(all).extracting(PointHistory::getType)
                .containsExactlyInAnyOrder(PointType.EARN, PointType.USE_COUPON, PointType.CHARGE);
    }
}
