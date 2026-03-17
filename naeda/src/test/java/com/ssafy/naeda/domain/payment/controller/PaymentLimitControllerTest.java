package com.ssafy.naeda.domain.payment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.naeda.domain.payment.dto.response.PaymentLimitResponse;
import com.ssafy.naeda.domain.payment.service.PaymentLimitService;
import com.ssafy.naeda.global.exception.BadRequestException;
import com.ssafy.naeda.global.exception.GlobalExceptionHandler;
import com.ssafy.naeda.global.security.JwtAuthenticationFilter;
import com.ssafy.naeda.global.security.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentLimitController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class PaymentLimitControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PaymentLimitService paymentLimitService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    // ── GET /api/payment/limit ────────────────────────────────────────────

    @Test
    @DisplayName("한도 조회 - 200 OK와 한도 정보를 반환한다")
    void getLimit_returns200() throws Exception {
        PaymentLimitResponse response = PaymentLimitResponse.builder()
                .userNo(1L)
                .dailyLimit(500_000L)
                .monthlyLimit(3_000_000L)
                .singleTransactionLimit(300_000L)
                .build();

        given(paymentLimitService.getLimit(1L)).willReturn(response);

        mockMvc.perform(get("/api/payment/limit").param("userNo", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userNo").value(1))
                .andExpect(jsonPath("$.dailyLimit").value(500_000))
                .andExpect(jsonPath("$.monthlyLimit").value(3_000_000))
                .andExpect(jsonPath("$.singleTransactionLimit").value(300_000));
    }

    @Test
    @DisplayName("한도 조회 - userNo 누락 시 400을 반환한다")
    void getLimit_missingUserNo_returns400() throws Exception {
        mockMvc.perform(get("/api/payment/limit"))
                .andExpect(status().isBadRequest());
    }

    // ── PUT /api/payment/limit ────────────────────────────────────────────

    @Test
    @DisplayName("한도 설정 - 200 OK와 설정된 한도를 반환한다")
    void setLimit_returns200() throws Exception {
        PaymentLimitResponse response = PaymentLimitResponse.builder()
                .userNo(1L)
                .dailyLimit(1_000_000L)
                .monthlyLimit(5_000_000L)
                .singleTransactionLimit(500_000L)
                .build();

        given(paymentLimitService.setLimit(eq(1L), any())).willReturn(response);

        Map<String, Object> body = Map.of(
                "dailyLimit", 1_000_000,
                "monthlyLimit", 5_000_000,
                "singleTransactionLimit", 500_000
        );

        mockMvc.perform(put("/api/payment/limit")
                        .param("userNo", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dailyLimit").value(1_000_000))
                .andExpect(jsonPath("$.monthlyLimit").value(5_000_000))
                .andExpect(jsonPath("$.singleTransactionLimit").value(500_000));
    }

    @Test
    @DisplayName("한도 설정 - 상한선 초과 시 400을 반환한다")
    void setLimit_exceedMax_returns400() throws Exception {
        given(paymentLimitService.setLimit(eq(1L), any()))
                .willThrow(new BadRequestException("1일 한도는 최대 5000000원까지 설정 가능합니다."));

        Map<String, Object> body = Map.of(
                "dailyLimit", 10_000_000,
                "monthlyLimit", 3_000_000,
                "singleTransactionLimit", 300_000
        );

        mockMvc.perform(put("/api/payment/limit")
                        .param("userNo", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    @DisplayName("한도 설정 - 필수 값 누락 시 400을 반환한다")
    void setLimit_missingField_returns400() throws Exception {
        Map<String, Object> body = Map.of(
                "dailyLimit", 500_000
                // monthlyLimit, singleTransactionLimit 누락
        );

        mockMvc.perform(put("/api/payment/limit")
                        .param("userNo", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("한도 설정 - userNo 누락 시 400을 반환한다")
    void setLimit_missingUserNo_returns400() throws Exception {
        Map<String, Object> body = Map.of(
                "dailyLimit", 500_000,
                "monthlyLimit", 3_000_000,
                "singleTransactionLimit", 300_000
        );

        mockMvc.perform(put("/api/payment/limit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }
}