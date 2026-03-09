package com.ssafy.naeda.domain.card.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.naeda.domain.card.dto.response.CardRegisterResponse;
import com.ssafy.naeda.domain.card.service.CardService;
import com.ssafy.naeda.global.exception.DuplicateException;
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

@WebMvcTest(CardController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class CardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CardService cardService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    // ── 성공 ──

    @Test
    @DisplayName("카드 등록 성공 - 201 CREATED와 등록 결과를 반환한다")
    void registerCard_returns201() throws Exception {
        given(cardService.registerCard(eq(1L), eq("test-user-key"), any())).willReturn(
                CardRegisterResponse.builder()
                        .cardId(100L)
                        .cardNo("1003622654847049")
                        .cvc("713")
                        .cardUniqueNo("1003-unique-abc")
                        .cardIssuerCode("1003")
                        .cardIssuerName("롯데카드")
                        .cardName("디지로카 SEOUL")
                        .cardExpiryDate("20290409")
                        .cardType("CREDIT")
                        .withdrawalAccountNo("0320000000001234")
                        .withdrawalDate("15")
                        .paymentMethodId(50L)
                        .build()
        );

        String body = objectMapper.writeValueAsString(Map.of(
                "cardUniqueNo", "1003-unique-abc",
                "withdrawalAccountNo", "0320000000001234",
                "withdrawalDate", "15"
        ));

        mockMvc.perform(post("/api/cards")
                        .param("userNo", "1")
                        .param("userKey", "test-user-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cardId").value(100))
                .andExpect(jsonPath("$.cardNo").value("1003622654847049"))
                .andExpect(jsonPath("$.cvc").value("713"))
                .andExpect(jsonPath("$.cardType").value("CREDIT"))
                .andExpect(jsonPath("$.cardIssuerName").value("롯데카드"))
                .andExpect(jsonPath("$.withdrawalAccountNo").value("0320000000001234"))
                .andExpect(jsonPath("$.paymentMethodId").value(50));
    }

    // ── 실패: 중복 카드 ──

    @Test
    @DisplayName("카드 등록 실패 - 중복 카드이면 409를 반환한다")
    void registerCard_duplicate_returns409() throws Exception {
        given(cardService.registerCard(eq(1L), eq("test-user-key"), any()))
                .willThrow(new DuplicateException("이미 등록된 카드입니다: 1003622654847049"));

        String body = objectMapper.writeValueAsString(Map.of(
                "cardUniqueNo", "1003-unique-abc",
                "withdrawalAccountNo", "0320000000001234",
                "withdrawalDate", "15"
        ));

        mockMvc.perform(post("/api/cards")
                        .param("userNo", "1")
                        .param("userKey", "test-user-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE"))
                .andExpect(jsonPath("$.message").value("이미 등록된 카드입니다: 1003622654847049"));
    }

    // ── 실패: 연결 계좌 없음 ──

    @Test
    @DisplayName("카드 등록 실패 - 연결 계좌 없으면 404를 반환한다")
    void registerCard_accountNotFound_returns404() throws Exception {
        given(cardService.registerCard(eq(1L), eq("test-user-key"), any()))
                .willThrow(new NotFoundException("연결 계좌를 찾을 수 없습니다: 0320000000001234"));

        String body = objectMapper.writeValueAsString(Map.of(
                "cardUniqueNo", "1003-unique-abc",
                "withdrawalAccountNo", "0320000000001234",
                "withdrawalDate", "15"
        ));

        mockMvc.perform(post("/api/cards")
                        .param("userNo", "1")
                        .param("userKey", "test-user-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("연결 계좌를 찾을 수 없습니다: 0320000000001234"));
    }

    // ── 실패: 파라미터 누락 ──

    @Test
    @DisplayName("카드 등록 실패 - userNo 누락 시 400을 반환한다")
    void registerCard_missingUserNo_returns400() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "cardUniqueNo", "1003-unique-abc",
                "withdrawalAccountNo", "0320000000001234",
                "withdrawalDate", "15"
        ));

        mockMvc.perform(post("/api/cards")
                        .param("userKey", "test-user-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("카드 등록 실패 - userKey 누락 시 400을 반환한다")
    void registerCard_missingUserKey_returns400() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "cardUniqueNo", "1003-unique-abc",
                "withdrawalAccountNo", "0320000000001234",
                "withdrawalDate", "15"
        ));

        mockMvc.perform(post("/api/cards")
                        .param("userNo", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("카드 등록 실패 - cardUniqueNo 누락 시 400을 반환한다")
    void registerCard_missingCardUniqueNo_returns400() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "withdrawalAccountNo", "0320000000001234",
                "withdrawalDate", "15"
        ));

        mockMvc.perform(post("/api/cards")
                        .param("userNo", "1")
                        .param("userKey", "test-user-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("카드 등록 실패 - withdrawalAccountNo 누락 시 400을 반환한다")
    void registerCard_missingWithdrawalAccountNo_returns400() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "cardUniqueNo", "1003-unique-abc",
                "withdrawalDate", "15"
        ));

        mockMvc.perform(post("/api/cards")
                        .param("userNo", "1")
                        .param("userKey", "test-user-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("카드 등록 실패 - withdrawalDate 누락 시 400을 반환한다")
    void registerCard_missingWithdrawalDate_returns400() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "cardUniqueNo", "1003-unique-abc",
                "withdrawalAccountNo", "0320000000001234"
        ));

        mockMvc.perform(post("/api/cards")
                        .param("userNo", "1")
                        .param("userKey", "test-user-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }
}
