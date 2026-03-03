package com.ssafy.naeda.domain.point.repository;

import com.ssafy.naeda.domain.point.entity.PointWallet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PointWalletRepositoryTest {

    @Autowired
    private PointWalletRepository pointWalletRepository;

    private PointWallet savedWallet;

    @BeforeEach
    void setUp() {
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
    @DisplayName("userNo로 지갑 조회")
    void findByUserNo() {
        Optional<PointWallet> result = pointWalletRepository.findByUserNo(1L);

        assertThat(result).isPresent();
        assertThat(result.get().getBalance()).isEqualTo(1000L);
    }

    @Test
    @DisplayName("존재하지 않는 userNo 조회 시 빈 Optional 반환")
    void findByUserNo_notFound() {
        Optional<PointWallet> result = pointWalletRepository.findByUserNo(999L);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("비관적 락으로 지갑 조회 (FOR UPDATE)")
    void findByUserNoForUpdate() {
        Optional<PointWallet> result = pointWalletRepository.findByUserNoForUpdate(1L);

        assertThat(result).isPresent();
        assertThat(result.get().getWalletId()).isEqualTo(savedWallet.getWalletId());
    }

    @Test
    @DisplayName("지갑 포인트 적립 후 저장")
    void earnPoints() {
        PointWallet wallet = pointWalletRepository.findByUserNo(1L).orElseThrow();
        wallet.earn(500L);
        pointWalletRepository.saveAndFlush(wallet);

        PointWallet updated = pointWalletRepository.findByUserNo(1L).orElseThrow();
        assertThat(updated.getBalance()).isEqualTo(1500L);
        assertThat(updated.getTotalEarned()).isEqualTo(1500L);
    }

    @Test
    @DisplayName("지갑 포인트 사용 후 저장")
    void usePoints() {
        PointWallet wallet = pointWalletRepository.findByUserNo(1L).orElseThrow();
        wallet.use(300L);
        pointWalletRepository.saveAndFlush(wallet);

        PointWallet updated = pointWalletRepository.findByUserNo(1L).orElseThrow();
        assertThat(updated.getBalance()).isEqualTo(700L);
        assertThat(updated.getTotalUsed()).isEqualTo(300L);
    }

    @Test
    @DisplayName("userNo unique 제약 - 중복 저장 시 예외")
    void uniqueUserNo() {
        org.junit.jupiter.api.Assertions.assertThrows(Exception.class, () -> {
            pointWalletRepository.saveAndFlush(
                    PointWallet.builder()
                            .userNo(1L)
                            .build()
            );
        });
    }
}
