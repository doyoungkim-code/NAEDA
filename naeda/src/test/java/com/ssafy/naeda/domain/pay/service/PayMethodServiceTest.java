package com.ssafy.naeda.domain.pay.service;

import com.ssafy.naeda.domain.pay.entity.MethodType;
import com.ssafy.naeda.domain.pay.entity.PayMethod;
import com.ssafy.naeda.domain.pay.repository.PayMethodRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PayMethodServiceTest {

    @Mock
    private PayMethodRepository payMethodRepository;

    @InjectMocks
    private PayMethodService payMethodService;

    @Test
    @DisplayName("대표 결제수단 변경 시 기존 기본값은 해제되고 선택한 수단만 isDefault=true가 된다")
    void setDefault_switchesSingleDefault() throws Exception {
        PayMethod accountMethod = buildMethod(1L, 10L, MethodType.ACCOUNT, true, false);
        PayMethod cardMethod = buildMethod(2L, 10L, MethodType.CREDIT_CARD, false, false);

        given(payMethodRepository.findByUserNoAndIsActiveTrue(10L))
                .willReturn(List.of(accountMethod, cardMethod));

        PayMethod result = payMethodService.setDefault(10L, 2L);

        assertThat(accountMethod.getIsDefault()).isFalse();
        assertThat(cardMethod.getIsDefault()).isTrue();
        assertThat(result.getPaymentMethodId()).isEqualTo(2L);
        verify(payMethodRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("페이스페이 결제수단 변경 시 기존 isFacePay는 해제되고 선택한 수단만 true가 된다")
    void setFacePay_switchesSingleFacePay() throws Exception {
        PayMethod accountMethod = buildMethod(1L, 10L, MethodType.ACCOUNT, true, true);
        PayMethod cardMethod = buildMethod(2L, 10L, MethodType.CREDIT_CARD, false, false);

        given(payMethodRepository.findByUserNoAndIsActiveTrue(10L))
                .willReturn(List.of(accountMethod, cardMethod));

        PayMethod result = payMethodService.setFacePay(10L, 2L);

        assertThat(accountMethod.getIsFacePay()).isFalse();
        assertThat(cardMethod.getIsFacePay()).isTrue();
        assertThat(cardMethod.getIsDefault()).isTrue();
        assertThat(result.getPaymentMethodId()).isEqualTo(2L);
        verify(payMethodRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("페이스페이 사용 해제 시 default는 유지하고 isFacePay만 false가 된다")
    void clearFacePay_keepsDefaultAndDisablesFacePay() throws Exception {
        PayMethod accountMethod = buildMethod(1L, 10L, MethodType.ACCOUNT, true, true);

        given(payMethodRepository.findByUserNoAndIsActiveTrue(10L))
                .willReturn(List.of(accountMethod));
        given(payMethodRepository.save(any(PayMethod.class))).willAnswer(invocation -> invocation.getArgument(0));

        PayMethod result = payMethodService.clearFacePay(10L, 1L);

        assertThat(accountMethod.getIsDefault()).isTrue();
        assertThat(accountMethod.getIsFacePay()).isFalse();
        assertThat(result.getPaymentMethodId()).isEqualTo(1L);
        verify(payMethodRepository).save(accountMethod);
    }

    private PayMethod buildMethod(
            Long paymentMethodId,
            Long userNo,
            MethodType methodType,
            boolean isDefault,
            boolean isFacePay
    ) throws Exception {
        PayMethod method = PayMethod.builder()
                .userNo(userNo)
                .methodType(methodType)
                .isDefault(isDefault)
                .isFacePay(isFacePay)
                .isActive(true)
                .build();
        setField(method, "paymentMethodId", paymentMethodId);
        return method;
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}