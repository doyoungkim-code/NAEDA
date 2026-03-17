package com.ssafy.naeda.domain.payment.service;

import com.ssafy.naeda.domain.payment.dto.request.PaymentLimitRequest;
import com.ssafy.naeda.domain.payment.dto.response.PaymentLimitResponse;
import com.ssafy.naeda.domain.payment.entity.PaymentLimit;
import com.ssafy.naeda.domain.payment.repository.PaymentLimitRepository;
import com.ssafy.naeda.global.exception.BadRequestException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PaymentLimitServiceTest {

    @Mock
    private PaymentLimitRepository paymentLimitRepository;

    @InjectMocks
    private PaymentLimitService paymentLimitService;

    private static final Long USER_NO = 1L;

    // ── 헬퍼 ──

    private PaymentLimit buildLimit(Long userNo, Long daily, Long monthly, Long single) {
        return PaymentLimit.builder()
                .userNo(userNo)
                .dailyLimit(daily)
                .monthlyLimit(monthly)
                .singleTransactionLimit(single)
                .build();
    }

    private PaymentLimitRequest buildRequest(Long daily, Long monthly, Long single) throws Exception {
        PaymentLimitRequest request = new PaymentLimitRequest();
        setField(request, "dailyLimit", daily);
        setField(request, "monthlyLimit", monthly);
        setField(request, "singleTransactionLimit", single);
        return request;
    }

    private void setField(Object target, String name, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(target, value);
    }

    // ── getLimit ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("한도 조회 - 설정된 한도를 반환한다")
    void getLimit_success() {
        PaymentLimit limit = buildLimit(USER_NO, 500_000L, 5_000_000L, 100_000L);
        given(paymentLimitRepository.findByUserNo(USER_NO)).willReturn(Optional.of(limit));

        PaymentLimitResponse result = paymentLimitService.getLimit(USER_NO);

        assertThat(result.getUserNo()).isEqualTo(USER_NO);
        assertThat(result.getDailyLimit()).isEqualTo(500_000L);
        assertThat(result.getMonthlyLimit()).isEqualTo(5_000_000L);
        assertThat(result.getSingleTransactionLimit()).isEqualTo(100_000L);
    }

    @Test
    @DisplayName("한도 조회 - 설정이 없으면 기본값을 반환한다")
    void getLimit_default() {
        given(paymentLimitRepository.findByUserNo(USER_NO)).willReturn(Optional.empty());

        PaymentLimitResponse result = paymentLimitService.getLimit(USER_NO);

        assertThat(result.getUserNo()).isEqualTo(USER_NO);
        assertThat(result.getDailyLimit()).isEqualTo(500_000L);
        assertThat(result.getMonthlyLimit()).isEqualTo(3_000_000L);
        assertThat(result.getSingleTransactionLimit()).isEqualTo(300_000L);
    }

    // ── setLimit ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("한도 설정 - 기존 설정이 없으면 새로 생성한다")
    void setLimit_create() throws Exception {
        PaymentLimitRequest request = buildRequest(500_000L, 5_000_000L, 100_000L);
        PaymentLimit saved = buildLimit(USER_NO, 500_000L, 5_000_000L, 100_000L);

        given(paymentLimitRepository.findByUserNo(USER_NO)).willReturn(Optional.empty());
        given(paymentLimitRepository.save(any(PaymentLimit.class))).willReturn(saved);

        PaymentLimitResponse result = paymentLimitService.setLimit(USER_NO, request);

        assertThat(result.getDailyLimit()).isEqualTo(500_000L);
        assertThat(result.getMonthlyLimit()).isEqualTo(5_000_000L);
        assertThat(result.getSingleTransactionLimit()).isEqualTo(100_000L);
        verify(paymentLimitRepository).save(any(PaymentLimit.class));
    }

    @Test
    @DisplayName("한도 수정 - 기존 설정이 있으면 업데이트한다")
    void setLimit_update() throws Exception {
        PaymentLimitRequest request = buildRequest(1_000_000L, 10_000_000L, 200_000L);
        PaymentLimit existing = buildLimit(USER_NO, 500_000L, 5_000_000L, 100_000L);

        given(paymentLimitRepository.findByUserNo(USER_NO)).willReturn(Optional.of(existing));

        PaymentLimitResponse result = paymentLimitService.setLimit(USER_NO, request);

        assertThat(result.getDailyLimit()).isEqualTo(1_000_000L);
        assertThat(result.getMonthlyLimit()).isEqualTo(10_000_000L);
        assertThat(result.getSingleTransactionLimit()).isEqualTo(200_000L);
        verify(paymentLimitRepository, never()).save(any());
    }

    @Test
    @DisplayName("한도 설정 실패 - 1일 한도가 상한선을 초과하면 BadRequestException")
    void setLimit_exceedDailyMax() throws Exception {
        PaymentLimitRequest request = buildRequest(10_000_000L, 3_000_000L, 300_000L);

        assertThatThrownBy(() -> paymentLimitService.setLimit(USER_NO, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("1일 한도는 최대");
    }

    @Test
    @DisplayName("한도 설정 실패 - 월 한도가 상한선을 초과하면 BadRequestException")
    void setLimit_exceedMonthlyMax() throws Exception {
        PaymentLimitRequest request = buildRequest(500_000L, 50_000_000L, 300_000L);

        assertThatThrownBy(() -> paymentLimitService.setLimit(USER_NO, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("월 한도는 최대");
    }

    @Test
    @DisplayName("한도 설정 실패 - 1회 한도가 상한선을 초과하면 BadRequestException")
    void setLimit_exceedSingleMax() throws Exception {
        PaymentLimitRequest request = buildRequest(500_000L, 3_000_000L, 10_000_000L);

        assertThatThrownBy(() -> paymentLimitService.setLimit(USER_NO, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("1회 한도는 최대");
    }

    // ── createDefaultLimit ────────────────────────────────────────────────

    @Test
    @DisplayName("기본 한도 생성 - 설정이 없으면 기본값으로 생성한다")
    void createDefaultLimit_success() {
        given(paymentLimitRepository.findByUserNo(USER_NO)).willReturn(Optional.empty());
        given(paymentLimitRepository.save(any(PaymentLimit.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        paymentLimitService.createDefaultLimit(USER_NO);

        verify(paymentLimitRepository).save(any(PaymentLimit.class));
    }

    @Test
    @DisplayName("기본 한도 생성 - 이미 설정이 있으면 생성하지 않는다")
    void createDefaultLimit_alreadyExists() {
        PaymentLimit existing = buildLimit(USER_NO, 500_000L, 3_000_000L, 300_000L);
        given(paymentLimitRepository.findByUserNo(USER_NO)).willReturn(Optional.of(existing));

        paymentLimitService.createDefaultLimit(USER_NO);

        verify(paymentLimitRepository, never()).save(any());
    }
}