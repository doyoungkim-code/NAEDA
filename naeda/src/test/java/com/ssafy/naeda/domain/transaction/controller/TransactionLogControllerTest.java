package com.ssafy.naeda.domain.transaction.controller;

import com.ssafy.naeda.domain.transaction.dto.response.TransactionLogResponse;
import com.ssafy.naeda.domain.transaction.entity.TransactionType;
import com.ssafy.naeda.domain.transaction.service.TransactionLogService;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransactionLogController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class TransactionLogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TransactionLogService transactionLogService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    private TransactionLogResponse stubResponse(Long logId, TransactionType type, Long amount) {
        return TransactionLogResponse.builder()
                .logId(logId)
                .accountId(10L)
                .transactionType(type)
                .amount(amount)
                .balanceAfter(100_000L)
                .counterpart("홍길동")
                .memo("테스트")
                .category("이체")
                .aiCategory("생활")
                .ssafyTransactionId("TXN" + logId)
                .transacted(LocalDateTime.of(2026, 3, 6, 12, 0, 0))
                .build();
    }

    // ── GET /api/transactions ────────────────────────────────────────────

    @Test
    @DisplayName("전체 거래내역 조회 - 200 OK와 거래 목록을 반환한다")
    void getTransactions_returns200() throws Exception {
        given(transactionLogService.getTransactions(1L, 10L))
                .willReturn(List.of(
                        stubResponse(1L, TransactionType.DEPOSIT, 50_000L),
                        stubResponse(2L, TransactionType.WITHDRAW, 10_000L)
                ));

        mockMvc.perform(get("/api/transactions")
                        .param("userNo", "1")
                        .param("accountId", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].logId").value(1))
                .andExpect(jsonPath("$[0].transactionType").value("DEPOSIT"))
                .andExpect(jsonPath("$[0].amount").value(50_000))
                .andExpect(jsonPath("$[0].aiCategory").value("생활"))
                .andExpect(jsonPath("$[1].transactionType").value("WITHDRAW"));
    }

    @Test
    @DisplayName("전체 거래내역 조회 - 소유권 검증 실패 시 404를 반환한다")
    void getTransactions_notFound() throws Exception {
        given(transactionLogService.getTransactions(1L, 99L))
                .willThrow(new NotFoundException("계좌를 찾을 수 없거나 접근할 수 없습니다."));

        mockMvc.perform(get("/api/transactions")
                        .param("userNo", "1")
                        .param("accountId", "99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("계좌를 찾을 수 없거나 접근할 수 없습니다."));
    }

    @Test
    @DisplayName("전체 거래내역 조회 - 필수 파라미터 누락 시 400을 반환한다")
    void getTransactions_missingParam_returns400() throws Exception {
        mockMvc.perform(get("/api/transactions")
                        .param("userNo", "1"))
                .andExpect(status().isBadRequest());
    }

    // ── GET /api/transactions/period ─────────────────────────────────────

    @Test
    @DisplayName("기간 필터 조회 - 200 OK와 거래 목록을 반환한다")
    void getTransactionsByPeriod_returns200() throws Exception {
        LocalDateTime from = LocalDateTime.of(2026, 1, 1, 0, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 3, 1, 0, 0, 0);

        given(transactionLogService.getTransactionsByPeriod(1L, 10L, from, to))
                .willReturn(List.of(
                        stubResponse(1L, TransactionType.DEPOSIT, 30_000L)
                ));

        mockMvc.perform(get("/api/transactions/period")
                        .param("userNo", "1")
                        .param("accountId", "10")
                        .param("from", "2026-01-01T00:00:00")
                        .param("to", "2026-03-01T00:00:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].amount").value(30_000));
    }

    @Test
    @DisplayName("기간 필터 조회 - from/to 누락 시 400을 반환한다")
    void getTransactionsByPeriod_missingParam_returns400() throws Exception {
        mockMvc.perform(get("/api/transactions/period")
                        .param("userNo", "1")
                        .param("accountId", "10"))
                .andExpect(status().isBadRequest());
    }

    // ── GET /api/transactions/type ───────────────────────────────────────

    @Test
    @DisplayName("거래유형 필터 조회 - 200 OK와 거래 목록을 반환한다")
    void getTransactionsByType_returns200() throws Exception {
        given(transactionLogService.getTransactionsByType(1L, 10L, TransactionType.DEPOSIT))
                .willReturn(List.of(
                        stubResponse(1L, TransactionType.DEPOSIT, 50_000L),
                        stubResponse(3L, TransactionType.DEPOSIT, 20_000L)
                ));

        mockMvc.perform(get("/api/transactions/type")
                        .param("userNo", "1")
                        .param("accountId", "10")
                        .param("transactionType", "DEPOSIT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].transactionType").value("DEPOSIT"))
                .andExpect(jsonPath("$[1].transactionType").value("DEPOSIT"));
    }

    @Test
    @DisplayName("거래유형 필터 조회 - transactionType 누락 시 400을 반환한다")
    void getTransactionsByType_missingParam_returns400() throws Exception {
        mockMvc.perform(get("/api/transactions/type")
                        .param("userNo", "1")
                        .param("accountId", "10"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("거래유형 필터 조회 - 잘못된 transactionType이면 400을 반환한다")
    void getTransactionsByType_invalidType_returns400() throws Exception {
        mockMvc.perform(get("/api/transactions/type")
                        .param("userNo", "1")
                        .param("accountId", "10")
                        .param("transactionType", "INVALID"))
                .andExpect(status().isBadRequest());
    }
}
