package com.ssafy.naeda.domain.account.controller;

import com.ssafy.naeda.domain.account.dto.response.AccountResponse;
import com.ssafy.naeda.domain.account.service.AccountService;
import com.ssafy.naeda.global.exception.GlobalExceptionHandler;
import com.ssafy.naeda.global.exception.NotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AccountController.class)
@Import(GlobalExceptionHandler.class)
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AccountService accountService;

    // ── GET /api/accounts ────────────────────────────────────────────────

    @Test
    @DisplayName("계좌 목록 조회 - 200 OK와 계좌 목록을 반환한다")
    void getAccounts_returns200() throws Exception {
        given(accountService.getAccounts(1L)).willReturn(List.of(
                AccountResponse.builder()
                        .accountId(1L)
                        .bankCode("999").bankName("싸피은행")
                        .accountNo("9990000000001234")
                        .accountName("내 계좌")
                        .accountBalance(5_000_000L)
                        .build(),
                AccountResponse.builder()
                        .accountId(2L)
                        .bankCode("001").bankName("한국은행")
                        .accountNo("0010000000005678")
                        .accountName("한국은행 입출금")
                        .accountBalance(1_000_000L)
                        .build()
        ));

        mockMvc.perform(get("/api/accounts").param("userNo", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].accountNo").value("9990000000001234"))
                .andExpect(jsonPath("$[0].accountBalance").value(5_000_000))
                .andExpect(jsonPath("$[1].accountNo").value("0010000000005678"));
    }

    @Test
    @DisplayName("계좌 목록 조회 - 존재하지 않는 userNo면 404를 반환한다")
    void getAccounts_userNotFound_returns404() throws Exception {
        given(accountService.getAccounts(999L))
                .willThrow(new NotFoundException("존재하지 않는 사용자입니다."));

        mockMvc.perform(get("/api/accounts").param("userNo", "999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("존재하지 않는 사용자입니다."));
    }

    @Test
    @DisplayName("계좌 목록 조회 - userNo 파라미터 누락 시 400을 반환한다")
    void getAccounts_missingUserNo_returns400() throws Exception {
        mockMvc.perform(get("/api/accounts"))
                .andExpect(status().isBadRequest());
    }

    // ── GET /api/accounts/{accountNo} ────────────────────────────────────

    @Test
    @DisplayName("계좌 단건 조회 - 200 OK와 계좌 정보를 반환한다")
    void getAccount_returns200() throws Exception {
        given(accountService.getAccount(1L, "9990000000001234")).willReturn(
                AccountResponse.builder()
                        .accountId(1L)
                        .bankCode("999").bankName("싸피은행")
                        .accountNo("9990000000001234")
                        .accountName("내 계좌")
                        .accountBalance(3_000_000L)
                        .build()
        );

        mockMvc.perform(get("/api/accounts/9990000000001234").param("userNo", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountNo").value("9990000000001234"))
                .andExpect(jsonPath("$.accountBalance").value(3_000_000))
                .andExpect(jsonPath("$.accountName").value("내 계좌"));
    }

    @Test
    @DisplayName("계좌 단건 조회 - 존재하지 않는 계좌면 404를 반환한다")
    void getAccount_notFound_returns404() throws Exception {
        given(accountService.getAccount(1L, "0000000000000000"))
                .willThrow(new NotFoundException("계좌 정보를 찾을 수 없습니다."));

        mockMvc.perform(get("/api/accounts/0000000000000000").param("userNo", "1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("계좌 정보를 찾을 수 없습니다."));
    }

    @Test
    @DisplayName("계좌 단건 조회 - userNo 파라미터 누락 시 400을 반환한다")
    void getAccount_missingUserNo_returns400() throws Exception {
        mockMvc.perform(get("/api/accounts/9990000000001234"))
                .andExpect(status().isBadRequest());
    }
}