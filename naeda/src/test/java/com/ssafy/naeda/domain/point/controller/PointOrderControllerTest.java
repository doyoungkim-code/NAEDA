package com.ssafy.naeda.domain.point.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.naeda.domain.point.dto.request.PointOrderCreateRequest;
import com.ssafy.naeda.domain.point.dto.response.PointOrderResponse;
import com.ssafy.naeda.domain.point.service.PointOrderService;
import com.ssafy.naeda.global.exception.InsufficientBalanceException;
import com.ssafy.naeda.global.exception.NotFoundException;
import com.ssafy.naeda.global.security.JwtAuthenticationFilter;
import com.ssafy.naeda.global.security.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PointOrderController.class)
@AutoConfigureMockMvc(addFilters = false)
class PointOrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PointOrderService pointOrderService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    private PointOrderResponse buildResponse() {
        return PointOrderResponse.builder()
                .orderId(1L)
                .userNo(1L)
                .productId(1L)
                .productName("아메리카노 쿠폰")
                .pointPrice(3000L)
                .roadAddress("구미시 인동중앙로 100")
                .numberAddress("인동동 123-4")
                .orderAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("구매 성공 - 201 Created")
    void purchaseProduct_success() throws Exception {
        PointOrderCreateRequest request = PointOrderCreateRequest.builder()
                .productId(1L)
                .roadAddress("구미시 인동중앙로 100")
                .numberAddress("인동동 123-4")
                .build();

        given(pointOrderService.purchaseProduct(eq(1L), any(PointOrderCreateRequest.class)))
                .willReturn(buildResponse());

        mockMvc.perform(post("/api/orders")
                        .param("userNo", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").value(1))
                .andExpect(jsonPath("$.productName").value("아메리카노 쿠폰"))
                .andExpect(jsonPath("$.pointPrice").value(3000))
                .andExpect(jsonPath("$.roadAddress").value("구미시 인동중앙로 100"));
    }

    @Test
    @DisplayName("구매 실패 - productId 누락 400")
    void purchaseProduct_missingProductId() throws Exception {
        PointOrderCreateRequest request = PointOrderCreateRequest.builder()
                .roadAddress("구미시 인동중앙로 100")
                .build();

        mockMvc.perform(post("/api/orders")
                        .param("userNo", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("구매 실패 - userNo 누락 400")
    void purchaseProduct_missingUserNo() throws Exception {
        PointOrderCreateRequest request = PointOrderCreateRequest.builder()
                .productId(1L)
                .build();

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("구매 실패 - 상품 없음 404")
    void purchaseProduct_productNotFound() throws Exception {
        PointOrderCreateRequest request = PointOrderCreateRequest.builder()
                .productId(999L)
                .build();

        given(pointOrderService.purchaseProduct(eq(1L), any(PointOrderCreateRequest.class)))
                .willThrow(new NotFoundException("포인트 상품을 찾을 수 없습니다. id = 999"));

        mockMvc.perform(post("/api/orders")
                        .param("userNo", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("구매 실패 - 잔액 부족 400")
    void purchaseProduct_insufficientBalance() throws Exception {
        PointOrderCreateRequest request = PointOrderCreateRequest.builder()
                .productId(1L)
                .build();

        given(pointOrderService.purchaseProduct(eq(1L), any(PointOrderCreateRequest.class)))
                .willThrow(new InsufficientBalanceException("포인트 잔액이 부족합니다."));

        mockMvc.perform(post("/api/orders")
                        .param("userNo", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("구매 실패 - 판매 불가 상품 500")
    void purchaseProduct_notAvailable() throws Exception {
        PointOrderCreateRequest request = PointOrderCreateRequest.builder()
                .productId(1L)
                .build();

        given(pointOrderService.purchaseProduct(eq(1L), any(PointOrderCreateRequest.class)))
                .willThrow(new IllegalStateException("현재 구매할 수 없는 상품입니다."));

        mockMvc.perform(post("/api/orders")
                        .param("userNo", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("구매 성공 - 주소 없이 주문")
    void purchaseProduct_withoutAddress() throws Exception {
        PointOrderCreateRequest request = PointOrderCreateRequest.builder()
                .productId(1L)
                .build();

        PointOrderResponse response = PointOrderResponse.builder()
                .orderId(2L)
                .userNo(1L)
                .productId(1L)
                .productName("아메리카노 쿠폰")
                .pointPrice(3000L)
                .orderAt(LocalDateTime.now())
                .build();

        given(pointOrderService.purchaseProduct(eq(1L), any(PointOrderCreateRequest.class)))
                .willReturn(response);

        mockMvc.perform(post("/api/orders")
                        .param("userNo", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.roadAddress").doesNotExist())
                .andExpect(jsonPath("$.numberAddress").doesNotExist());
    }
}
