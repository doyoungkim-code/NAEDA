package com.ssafy.naeda.domain.payment.service;

import com.ssafy.naeda.domain.payment.entity.MethodType;
import com.ssafy.naeda.domain.payment.entity.PaymentMethod;
import com.ssafy.naeda.domain.payment.repository.PaymentMethodRepository;
import com.ssafy.naeda.global.exception.BadRequestException;
import com.ssafy.naeda.global.exception.NotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PaymentMethodServiceTest {

    @Mock
    private PaymentMethodRepository paymentMethodRepository;

    @InjectMocks
    private PaymentMethodService paymentMethodService;

    private static final Long USER_NO = 1L;
    private static final Long PM_ID = 10L;

    // ── 헬퍼 ──

    private PaymentMethod buildMethod(Long pmId, Long userNo, MethodType type, boolean isFacePay) throws Exception {
        PaymentMethod pm = PaymentMethod.builder()
                .userNo(userNo)
                .methodType(type)
                .creditCardId(type == MethodType.CREDIT_CARD ? 100L : null)
                .debitCardId(type == MethodType.DEBIT_CARD ? 200L : null)
                .build();
        setField(pm, "paymentMethodId", pmId);
        if (isFacePay) pm.setAsFacePay();
        return pm;
    }

    private void setField(Object target, String name, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(target, value);
    }

    // ── getPaymentMethods ────────────────────────────────────────────────

    @Test
    @DisplayName("결제 수단 목록 조회 - 사용자의 결제 수단 목록을 반환한다")
    void getPaymentMethods_success() throws Exception {
        PaymentMethod creditPm = buildMethod(10L, USER_NO, MethodType.CREDIT_CARD, false);
        PaymentMethod debitPm  = buildMethod(20L, USER_NO, MethodType.DEBIT_CARD, true);

        given(paymentMethodRepository.findByUserNo(USER_NO)).willReturn(List.of(creditPm, debitPm));

        List<PaymentMethod> result = paymentMethodService.getPaymentMethods(USER_NO);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getMethodType()).isEqualTo(MethodType.CREDIT_CARD);
        assertThat(result.get(1).getMethodType()).isEqualTo(MethodType.DEBIT_CARD);
        assertThat(result.get(1).getIsFacePay()).isTrue();
    }

    @Test
    @DisplayName("결제 수단 목록 조회 - 등록된 수단이 없으면 빈 목록을 반환한다")
    void getPaymentMethods_empty() {
        given(paymentMethodRepository.findByUserNo(USER_NO)).willReturn(List.of());

        List<PaymentMethod> result = paymentMethodService.getPaymentMethods(USER_NO);

        assertThat(result).isEmpty();
    }

    // ── setFacePayMethod ─────────────────────────────────────────────────

    @Test
    @DisplayName("페이스페이 수단 지정 - 기존 수단을 해제하고 새 수단에 isFacePay=true를 설정한다")
    void setFacePayMethod_success() throws Exception {
        PaymentMethod existing = buildMethod(5L, USER_NO, MethodType.DEBIT_CARD, true);
        PaymentMethod target   = buildMethod(PM_ID, USER_NO, MethodType.CREDIT_CARD, false);

        given(paymentMethodRepository.findById(PM_ID)).willReturn(Optional.of(target));
        given(paymentMethodRepository.findByUserNoAndIsFacePayTrue(USER_NO)).willReturn(Optional.of(existing));

        paymentMethodService.setFacePayMethod(USER_NO, PM_ID);

        assertThat(existing.getIsFacePay()).isFalse();
        assertThat(target.getIsFacePay()).isTrue();
        assertThat(target.getIsDefault()).isTrue();
    }

    @Test
    @DisplayName("페이스페이 수단 지정 - 기존 페이스페이 수단이 없어도 정상 지정된다")
    void setFacePayMethod_noExistingFacePay() throws Exception {
        PaymentMethod target = buildMethod(PM_ID, USER_NO, MethodType.CREDIT_CARD, false);

        given(paymentMethodRepository.findById(PM_ID)).willReturn(Optional.of(target));
        given(paymentMethodRepository.findByUserNoAndIsFacePayTrue(USER_NO)).willReturn(Optional.empty());

        paymentMethodService.setFacePayMethod(USER_NO, PM_ID);

        assertThat(target.getIsFacePay()).isTrue();
    }

    @Test
    @DisplayName("페이스페이 수단 지정 실패 - 존재하지 않는 paymentMethodId면 NotFoundException")
    void setFacePayMethod_notFound() {
        given(paymentMethodRepository.findById(PM_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> paymentMethodService.setFacePayMethod(USER_NO, PM_ID))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("존재하지 않는 결제 수단입니다.");
    }

    @Test
    @DisplayName("페이스페이 수단 지정 실패 - 타인의 결제 수단이면 BadRequestException")
    void setFacePayMethod_wrongUser() throws Exception {
        PaymentMethod otherUserMethod = buildMethod(PM_ID, 999L, MethodType.CREDIT_CARD, false);

        given(paymentMethodRepository.findById(PM_ID)).willReturn(Optional.of(otherUserMethod));

        assertThatThrownBy(() -> paymentMethodService.setFacePayMethod(USER_NO, PM_ID))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("본인의 결제 수단만 설정할 수 있습니다.");
    }
}
