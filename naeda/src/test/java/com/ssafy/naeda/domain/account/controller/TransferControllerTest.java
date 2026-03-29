package com.ssafy.naeda.domain.account.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.naeda.domain.account.dto.response.TransferResponse;
import com.ssafy.naeda.domain.account.service.TransferService;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransferController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class TransferControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TransferService transferService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    // ── POST /api/transfers ──────────────────────────────────────────────

    @Test
    @DisplayName("이체 성공 - 200 OK와 이체 결과를 반환한다")
    void transfer_returns200() throws Exception {
        given(transferService.transfer(eq(1L), any())).willReturn(
                TransferResponse.builder()
                        .withdrawalAccountNo("9990000000001234")
                        .depositAccountNo("0010000000005678")
                        .amount(50_000L)
                        .transactionDate("20260306")
                        .withdrawalTransactionNo("61")
                        .depositTransactionNo("62")
                        .build()
        );

        String body = objectMapper.writeValueAsString(Map.of(
                "withdrawalAccountNo", "9990000000001234",
                "depositAccountNo", "0010000000005678",
                "amount", 50000,
                "memo", "용돈"
        ));

        mockMvc.perform(post("/api/transfers")
                        .param("userNo", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.withdrawalAccountNo").value("9990000000001234"))
                .andExpect(jsonPath("$.depositAccountNo").value("0010000000005678"))
                .andExpect(jsonPath("$.amount").value(50_000))
                .andExpect(jsonPath("$.transactionDate").value("20260306"))
                .andExpect(jsonPath("$.withdrawalTransactionNo").value("61"))
                .andExpect(jsonPath("$.depositTransactionNo").value("62"));
    }

    @Test
    @DisplayName("이체 실패 - 소유권 검증 실패 시 404를 반환한다")
    void transfer_notFound() throws Exception {
        given(transferService.transfer(eq(1L), any()))
                .willThrow(new NotFoundException("계좌를 찾을 수 없거나 접근 권한이 없습니다."));

        String body = objectMapper.writeValueAsString(Map.of(
                "withdrawalAccountNo", "9990000000001234",
                "depositAccountNo", "0010000000005678",
                "amount", 50000
        ));

        mockMvc.perform(post("/api/transfers")
                        .param("userNo", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("계좌를 찾을 수 없거나 접근 권한이 없습니다."));
    }

    @Test
    @DisplayName("이체 실패 - userNo 파라미터 누락 시 400을 반환한다")
    void transfer_missingUserNo_returns400() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "withdrawalAccountNo", "9990000000001234",
                "depositAccountNo", "0010000000005678",
                "amount", 50000
        ));

        mockMvc.perform(post("/api/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("이체 실패 - 출금 계좌번호 누락 시 400을 반환한다")
    void transfer_missingWithdrawalAccountNo_returns400() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "depositAccountNo", "0010000000005678",
                "amount", 50000
        ));

        mockMvc.perform(post("/api/transfers")
                        .param("userNo", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("이체 실패 - 입금 계좌번호 누락 시 400을 반환한다")
    void transfer_missingDepositAccountNo_returns400() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "withdrawalAccountNo", "9990000000001234",
                "amount", 50000
        ));

        mockMvc.perform(post("/api/transfers")
                        .param("userNo", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("이체 실패 - 이체 금액 누락 시 400을 반환한다")
    void transfer_missingAmount_returns400() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "withdrawalAccountNo", "9990000000001234",
                "depositAccountNo", "0010000000005678"
        ));

        mockMvc.perform(post("/api/transfers")
                        .param("userNo", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("이체 실패 - 이체 금액이 0이면 400을 반환한다")
    void transfer_zeroAmount_returns400() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "withdrawalAccountNo", "9990000000001234",
                "depositAccountNo", "0010000000005678",
                "amount", 0
        ));

        mockMvc.perform(post("/api/transfers")
                        .param("userNo", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }
}
