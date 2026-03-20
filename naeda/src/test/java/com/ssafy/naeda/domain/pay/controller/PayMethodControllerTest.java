package com.ssafy.naeda.domain.pay.controller;

import com.ssafy.naeda.domain.pay.entity.MethodType;
import com.ssafy.naeda.domain.pay.entity.PayMethod;
import com.ssafy.naeda.domain.pay.service.PayMethodService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.lang.reflect.Field;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class PayMethodControllerTest {

    @Mock
    private PayMethodService payMethodService;

    @InjectMocks
    private PayMethodController payMethodController;

    @Test
    @DisplayName("결제수단 목록 조회는 서비스를 그대로 위임한다")
    void getPaymentMethods_delegatesToService() throws Exception {
        List<PayMethod> methods = List.of(buildMethod(1L, 10L));
        given(payMethodService.getActiveMethods(10L)).willReturn(methods);

        ResponseEntity<List<PayMethod>> response = payMethodController.getPaymentMethods(10L);

        assertThat(response.getBody()).isEqualTo(methods);
        then(payMethodService).should().getActiveMethods(10L);
    }

    @Test
    @DisplayName("대표 결제수단 설정은 서비스를 호출해 결과를 반환한다")
    void setDefault_delegatesToService() throws Exception {
        PayMethod method = buildMethod(2L, 10L);
        given(payMethodService.setDefault(10L, 2L)).willReturn(method);

        ResponseEntity<PayMethod> response = payMethodController.setDefault(2L, 10L);

        assertThat(response.getBody()).isEqualTo(method);
        then(payMethodService).should().setDefault(10L, 2L);
    }

    @Test
    @DisplayName("페이스페이 사용 토글은 enabled 값을 포함해 서비스를 호출한다")
    void setFacePay_delegatesToService() throws Exception {
        PayMethod method = buildMethod(3L, 10L);
        given(payMethodService.updateFacePay(10L, 3L, false)).willReturn(method);

        ResponseEntity<PayMethod> response = payMethodController.setFacePay(3L, 10L, false);

        assertThat(response.getBody()).isEqualTo(method);
        then(payMethodService).should().updateFacePay(10L, 3L, false);
    }

    private PayMethod buildMethod(Long paymentMethodId, Long userNo) throws Exception {
        PayMethod method = PayMethod.builder()
                .userNo(userNo)
                .methodType(MethodType.ACCOUNT)
                .isActive(true)
                .build();
        Field field = method.getClass().getDeclaredField("paymentMethodId");
        field.setAccessible(true);
        field.set(method, paymentMethodId);
        return method;
    }
}