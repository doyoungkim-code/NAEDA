package com.ssafy.naeda.domain.point.service;

import com.ssafy.naeda.domain.point.dto.request.PointEarnRequest;
import com.ssafy.naeda.domain.point.dto.request.PointUseRequest;
import com.ssafy.naeda.domain.point.dto.response.PointHistoryResponse;
import com.ssafy.naeda.domain.point.dto.response.PointWalletResponse;
import com.ssafy.naeda.domain.point.entity.PointType;
import com.ssafy.naeda.domain.point.repository.PointHistoryRepository;
import com.ssafy.naeda.domain.point.repository.PointWalletRepository;
import com.ssafy.naeda.global.exception.DuplicateException;
import com.ssafy.naeda.global.exception.InsufficientBalanceException;
import com.ssafy.naeda.global.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class PointServiceTest {

    @Autowired
    private PointService pointService;

    @Autowired
    private PointWalletRepository pointWalletRepository;

    @Autowired
    private PointHistoryRepository pointHistoryRepository;

    private static final Long USER_NO = 1L;

    @BeforeEach
    void setUp() {
        pointHistoryRepository.deleteAll();
        pointWalletRepository.deleteAll();
    }

    @Test
    @DisplayName("지갑 생성")
    void createWallet() {
        PointWalletResponse response = pointService.createWallet(USER_NO);

        assertThat(response.getWalletId()).isNotNull();
        assertThat(response.getUserNo()).isEqualTo(USER_NO);
        assertThat(response.getBalance()).isEqualTo(0L);
        assertThat(response.getTotalEarned()).isEqualTo(0L);
        assertThat(response.getTotalUsed()).isEqualTo(0L);
    }

    @Test
    @DisplayName("지갑 중복 생성 시 예외")
    void createWallet_duplicate() {
        pointService.createWallet(USER_NO);

        assertThatThrownBy(() -> pointService.createWallet(USER_NO))
                .isInstanceOf(DuplicateException.class);
    }

    @Test
    @DisplayName("지갑 조회")
    void getWallet() {
        pointService.createWallet(USER_NO);

        PointWalletResponse response = pointService.getWallet(USER_NO);

        assertThat(response.getUserNo()).isEqualTo(USER_NO);
        assertThat(response.getBalance()).isEqualTo(0L);
    }

    @Test
    @DisplayName("존재하지 않는 지갑 조회 시 예외")
    void getWallet_notFound() {
        assertThatThrownBy(() -> pointService.getWallet(999L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("포인트 적립")
    void earnPoints() {
        pointService.createWallet(USER_NO);

        PointEarnRequest request = PointEarnRequest.builder()
                .amount(1000L)
                .description("결제 적립")
                .paymentId(100L)
                .build();

        PointWalletResponse response = pointService.earnPoints(USER_NO, request);

        assertThat(response.getBalance()).isEqualTo(1000L);
        assertThat(response.getTotalEarned()).isEqualTo(1000L);
    }

    @Test
    @DisplayName("포인트 사용")
    void usePoints() {
        pointService.createWallet(USER_NO);
        pointService.earnPoints(USER_NO, PointEarnRequest.builder()
                .amount(1000L)
                .description("충전")
                .build());

        PointUseRequest request = PointUseRequest.builder()
                .amount(300L)
                .description("쿠폰 사용")
                .build();

        PointWalletResponse response = pointService.usePoints(USER_NO, request);

        assertThat(response.getBalance()).isEqualTo(700L);
        assertThat(response.getTotalUsed()).isEqualTo(300L);
    }

    @Test
    @DisplayName("잔액 부족 시 포인트 사용 예외")
    void usePoints_insufficientBalance() {
        pointService.createWallet(USER_NO);

        PointUseRequest request = PointUseRequest.builder()
                .amount(1000L)
                .description("잔액 부족 테스트")
                .build();

        assertThatThrownBy(() -> pointService.usePoints(USER_NO, request))
                .isInstanceOf(InsufficientBalanceException.class);
    }

    @Test
    @DisplayName("포인트 이력 조회 - 최신순 정렬")
    void getHistories() {
        pointService.createWallet(USER_NO);
        pointService.earnPoints(USER_NO, PointEarnRequest.builder()
                .amount(1000L)
                .description("첫 번째 적립")
                .build());
        pointService.usePoints(USER_NO, PointUseRequest.builder()
                .amount(300L)
                .description("쿠폰 사용")
                .build());

        List<PointHistoryResponse> histories = pointService.getHistories(USER_NO);

        assertThat(histories).hasSize(2);
        assertThat(histories.get(0).getType()).isEqualTo(PointType.USE_COUPON);
        assertThat(histories.get(1).getType()).isEqualTo(PointType.EARN);
    }
}
