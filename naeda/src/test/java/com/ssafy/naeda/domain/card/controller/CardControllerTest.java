package com.ssafy.naeda.domain.card.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.naeda.domain.card.dto.response.CardRegisterResponse;
import com.ssafy.naeda.domain.card.dto.response.CardResponse;
import com.ssafy.naeda.domain.card.dto.response.CardTransactionResponse;
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

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
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
        given(cardService.registerCard(eq(1L), any())).willReturn(
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
                "withdrawalDate", "15",
                "cardTypeCode", "1"
        ));

        mockMvc.perform(post("/api/cards")
                        .param("userNo", "1")
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
        given(cardService.registerCard(eq(1L), any()))
                .willThrow(new DuplicateException("이미 등록된 카드입니다: 1003622654847049"));

        String body = objectMapper.writeValueAsString(Map.of(
                "cardUniqueNo", "1003-unique-abc",
                "withdrawalAccountNo", "0320000000001234",
                "withdrawalDate", "15",
                "cardTypeCode", "1"
        ));

        mockMvc.perform(post("/api/cards")
                        .param("userNo", "1")
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
        given(cardService.registerCard(eq(1L), any()))
                .willThrow(new NotFoundException("연결 계좌를 찾을 수 없습니다: 0320000000001234"));

        String body = objectMapper.writeValueAsString(Map.of(
                "cardUniqueNo", "1003-unique-abc",
                "withdrawalAccountNo", "0320000000001234",
                "withdrawalDate", "15",
                "cardTypeCode", "1"
        ));

        mockMvc.perform(post("/api/cards")
                        .param("userNo", "1")
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
                "withdrawalDate", "15",
                "cardTypeCode", "1"
        ));

        mockMvc.perform(post("/api/cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("카드 등록 실패 - cardUniqueNo 누락 시 400을 반환한다")
    void registerCard_missingCardUniqueNo_returns400() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "withdrawalAccountNo", "0320000000001234",
                "withdrawalDate", "15",
                "cardTypeCode", "1"
        ));

        mockMvc.perform(post("/api/cards")
                        .param("userNo", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("카드 등록 실패 - withdrawalAccountNo 누락 시 400을 반환한다")
    void registerCard_missingWithdrawalAccountNo_returns400() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "cardUniqueNo", "1003-unique-abc",
                "withdrawalDate", "15",
                "cardTypeCode", "1"
        ));

        mockMvc.perform(post("/api/cards")
                        .param("userNo", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("카드 등록 실패 - withdrawalDate 누락 시 400을 반환한다")
    void registerCard_missingWithdrawalDate_returns400() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "cardUniqueNo", "1003-unique-abc",
                "withdrawalAccountNo", "0320000000001234",
                "cardTypeCode", "1"
        ));

        mockMvc.perform(post("/api/cards")
                        .param("userNo", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("카드 등록 실패 - cardTypeCode 누락 시 400을 반환한다")
    void registerCard_missingCardTypeCode_returns400() throws Exception {
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

    // ── GET /api/cards ──

    @Test
    @DisplayName("카드 목록 조회 성공 - 200 OK와 카드 리스트를 반환한다")
    void getCards_returns200() throws Exception {
        given(cardService.getMyCards(1L)).willReturn(List.of(
                CardResponse.builder()
                        .cardId(100L).cardNo("1003000000001111").cvc("123")
                        .cardIssuerName("롯데카드").cardName("디지로카 SEOUL")
                        .cardType("CREDIT").isActive(true)
                        .creditLimit(1_000_000L).billingDate(15)
                        .build(),
                CardResponse.builder()
                        .cardId(200L).cardNo("1005000000002222").cvc("456")
                        .cardIssuerName("신한카드").cardName("신한 체크카드")
                        .cardType("DEBIT").isActive(true)
                        .build()
        ));

        mockMvc.perform(get("/api/cards")
                        .param("userNo", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].cardType").value("CREDIT"))
                .andExpect(jsonPath("$[0].cardNo").value("1003000000001111"))
                .andExpect(jsonPath("$[1].cardType").value("DEBIT"))
                .andExpect(jsonPath("$[1].cardNo").value("1005000000002222"));
    }

    @Test
    @DisplayName("카드 목록 조회 - 카드 없으면 빈 리스트 반환")
    void getCards_empty() throws Exception {
        given(cardService.getMyCards(1L)).willReturn(List.of());

        mockMvc.perform(get("/api/cards")
                        .param("userNo", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("카드 목록 조회 실패 - userNo 누락 시 400을 반환한다")
    void getCards_missingUserNo_returns400() throws Exception {
        mockMvc.perform(get("/api/cards"))
                .andExpect(status().isBadRequest());
    }

    // ── DELETE /api/cards/{cardId} ──

    @Test
    @DisplayName("카드 삭제 성공 - 204 No Content를 반환한다")
    void deleteCard_returns204() throws Exception {
        doNothing().when(cardService).deleteCard(1L, 100L, "CREDIT");

        mockMvc.perform(delete("/api/cards/100")
                        .param("userNo", "1")
                        .param("cardType", "CREDIT"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("카드 삭제 실패 - 카드 없으면 404를 반환한다")
    void deleteCard_notFound_returns404() throws Exception {
        doThrow(new NotFoundException("카드를 찾을 수 없습니다: 999"))
                .when(cardService).deleteCard(1L, 999L, "CREDIT");

        mockMvc.perform(delete("/api/cards/999")
                        .param("userNo", "1")
                        .param("cardType", "CREDIT"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    @DisplayName("카드 삭제 실패 - userNo 누락 시 400을 반환한다")
    void deleteCard_missingUserNo_returns400() throws Exception {
        mockMvc.perform(delete("/api/cards/100")
                        .param("cardType", "CREDIT"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("카드 삭제 실패 - cardType 누락 시 400을 반환한다")
    void deleteCard_missingCardType_returns400() throws Exception {
        mockMvc.perform(delete("/api/cards/100")
                        .param("userNo", "1"))
                .andExpect(status().isBadRequest());
    }

    // ── GET /api/cards/{cardId}/transactions ──

    @Test
    @DisplayName("카드 결제 내역 조회 성공 - 200 OK와 거래 내역 리스트를 반환한다")
    void getCardTransactions_returns200() throws Exception {
        given(cardService.getCardTransactions(eq(1L), eq("test-user-key"), eq(100L), any()))
                .willReturn(List.of(
                        CardTransactionResponse.builder()
                                .logId(1L).transactionUniqueNo("TX-001")
                                .categoryName("식비").merchantName("스타벅스")
                                .transactionDate("2024-04-10").transactionTime("14:30:00")
                                .amount(5000L).cardStatus("승인")
                                .build(),
                        CardTransactionResponse.builder()
                                .logId(2L).transactionUniqueNo("TX-002")
                                .categoryName("교통").merchantName("카카오택시")
                                .transactionDate("2024-04-11").transactionTime("09:15:00")
                                .amount(12000L).cardStatus("승인")
                                .build()
                ));

        mockMvc.perform(get("/api/cards/100/transactions")
                        .param("userNo", "1")
                        .param("userKey", "test-user-key")
                        .param("startDate", "20240401")
                        .param("endDate", "20240430"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].logId").value(1))
                .andExpect(jsonPath("$[0].categoryName").value("식비"))
                .andExpect(jsonPath("$[0].merchantName").value("스타벅스"))
                .andExpect(jsonPath("$[0].amount").value(5000))
                .andExpect(jsonPath("$[1].logId").value(2))
                .andExpect(jsonPath("$[1].merchantName").value("카카오택시"));
    }

    @Test
    @DisplayName("카드 결제 내역 조회 - 거래 없으면 빈 리스트 반환")
    void getCardTransactions_empty() throws Exception {
        given(cardService.getCardTransactions(eq(1L), eq("test-user-key"), eq(100L), any()))
                .willReturn(List.of());

        mockMvc.perform(get("/api/cards/100/transactions")
                        .param("userNo", "1")
                        .param("userKey", "test-user-key")
                        .param("startDate", "20240401")
                        .param("endDate", "20240430"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("카드 결제 내역 조회 실패 - 카드 없으면 404를 반환한다")
    void getCardTransactions_notFound_returns404() throws Exception {
        given(cardService.getCardTransactions(eq(1L), eq("test-user-key"), eq(999L), any()))
                .willThrow(new NotFoundException("카드를 찾을 수 없습니다: 999"));

        mockMvc.perform(get("/api/cards/999/transactions")
                        .param("userNo", "1")
                        .param("userKey", "test-user-key")
                        .param("startDate", "20240401")
                        .param("endDate", "20240430"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    @DisplayName("카드 결제 내역 조회 실패 - userNo 누락 시 400을 반환한다")
    void getCardTransactions_missingUserNo_returns400() throws Exception {
        mockMvc.perform(get("/api/cards/100/transactions")
                        .param("userKey", "test-user-key")
                        .param("startDate", "20240401")
                        .param("endDate", "20240430"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("카드 결제 내역 조회 실패 - userKey 누락 시 400을 반환한다")
    void getCardTransactions_missingUserKey_returns400() throws Exception {
        mockMvc.perform(get("/api/cards/100/transactions")
                        .param("userNo", "1")
                        .param("startDate", "20240401")
                        .param("endDate", "20240430"))
                .andExpect(status().isBadRequest());
    }
}
