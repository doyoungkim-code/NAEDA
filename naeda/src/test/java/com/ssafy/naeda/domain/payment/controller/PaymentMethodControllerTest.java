package com.ssafy.naeda.domain.payment.controller;

import com.ssafy.naeda.domain.payment.entity.MethodType;
import com.ssafy.naeda.domain.payment.entity.PaymentMethod;
import com.ssafy.naeda.domain.payment.service.PaymentMethodService;
import com.ssafy.naeda.global.exception.BadRequestException;
import com.ssafy.naeda.global.exception.GlobalExceptionHandler;
import com.ssafy.naeda.global.exception.NotFoundException;
import com.ssafy.naeda.global.security.JwtAuthenticationFilter;
import com.ssafy.naeda.global.security.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentMethodController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class PaymentMethodControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaymentMethodService paymentMethodService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    // ── GET /api/payment-methods ─────────────────────────────────────────

    @Test
    @DisplayName("결제 수단 목록 조회 - 200 OK와 목록을 반환한다")
    void getPaymentMethods_returns200() throws Exception {
        PaymentMethod creditMethod = PaymentMethod.builder()
                .userNo(1L)
                .methodType(MethodType.CREDIT_CARD)
                .creditCardId(100L)
                .build();
        PaymentMethod debitMethod = PaymentMethod.builder()
                .userNo(1L)
                .methodType(MethodType.DEBIT_CARD)
                .debitCardId(200L)
                .build();

        given(paymentMethodService.getPaymentMethods(1L)).willReturn(List.of(creditMethod, debitMethod));

        mockMvc.perform(get("/api/payment-methods").param("userNo", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].methodType").value("CREDIT_CARD"))
                .andExpect(jsonPath("$[1].methodType").value("DEBIT_CARD"));
    }

    @Test
    @DisplayName("결제 수단 목록 조회 - userNo 누락 시 400을 반환한다")
    void getPaymentMethods_missingUserNo_returns400() throws Exception {
        mockMvc.perform(get("/api/payment-methods"))
                .andExpect(status().isBadRequest());
    }

    // ── PATCH /api/payment-methods/{id}/face-pay ─────────────────────────

    @Test
    @DisplayName("페이스페이 수단 지정 - 204 No Content를 반환한다")
    void setFacePayMethod_returns204() throws Exception {
        doNothing().when(paymentMethodService).setFacePayMethod(1L, 10L);

        mockMvc.perform(patch("/api/payment-methods/10/face-pay").param("userNo", "1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("페이스페이 수단 지정 - 존재하지 않는 paymentMethodId면 404를 반환한다")
    void setFacePayMethod_notFound_returns404() throws Exception {
        willThrow(new NotFoundException("존재하지 않는 결제 수단입니다."))
                .given(paymentMethodService).setFacePayMethod(1L, 999L);

        mockMvc.perform(patch("/api/payment-methods/999/face-pay").param("userNo", "1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("존재하지 않는 결제 수단입니다."));
    }

    @Test
    @DisplayName("페이스페이 수단 지정 - 타인의 결제 수단이면 400을 반환한다")
    void setFacePayMethod_wrongUser_returns400() throws Exception {
        willThrow(new BadRequestException("본인의 결제 수단만 설정할 수 있습니다."))
                .given(paymentMethodService).setFacePayMethod(1L, 10L);

        mockMvc.perform(patch("/api/payment-methods/10/face-pay").param("userNo", "1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("본인의 결제 수단만 설정할 수 있습니다."));
    }

    @Test
    @DisplayName("페이스페이 수단 지정 - userNo 누락 시 400을 반환한다")
    void setFacePayMethod_missingUserNo_returns400() throws Exception {
        mockMvc.perform(patch("/api/payment-methods/10/face-pay"))
                .andExpect(status().isBadRequest());
    }
}
