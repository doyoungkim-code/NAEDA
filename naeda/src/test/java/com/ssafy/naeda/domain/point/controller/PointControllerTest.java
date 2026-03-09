package com.ssafy.naeda.domain.point.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.naeda.domain.point.dto.request.PointEarnRequest;
import com.ssafy.naeda.domain.point.dto.request.PointUseRequest;
import com.ssafy.naeda.domain.point.repository.PointHistoryRepository;
import com.ssafy.naeda.domain.point.repository.PointWalletRepository;
import com.ssafy.naeda.domain.point.service.PointService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Transactional
class PointControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PointService pointService;

    @Autowired
    private PointWalletRepository pointWalletRepository;

    @Autowired
    private PointHistoryRepository pointHistoryRepository;

    private static final Long USER_NO = 1L;

    @BeforeEach
    void setUp() {
        pointHistoryRepository.deleteAll();
        pointWalletRepository.deleteAll();
    }

    @Test
    @DisplayName("POST /api/points/wallet/{userNo} - 지갑 생성 201")
    void createWallet() throws Exception {
        mockMvc.perform(post("/api/points/wallet/{userNo}", USER_NO))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userNo").value(USER_NO))
                .andExpect(jsonPath("$.balance").value(0));
    }

    @Test
    @DisplayName("GET /api/points/wallet/{userNo} - 잔액 조회 200")
    void getWallet() throws Exception {
        pointService.createWallet(USER_NO);

        mockMvc.perform(get("/api/points/wallet/{userNo}", USER_NO))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userNo").value(USER_NO))
                .andExpect(jsonPath("$.balance").value(0));
    }

    @Test
    @DisplayName("POST /api/points/wallet/{userNo}/earn - 포인트 적립 200")
    void earnPoints() throws Exception {
        pointService.createWallet(USER_NO);

        PointEarnRequest request = PointEarnRequest.builder()
                .amount(1000L)
                .description("결제 적립")
                .paymentId(100L)
                .build();

        mockMvc.perform(post("/api/points/wallet/{userNo}/earn", USER_NO)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(1000))
                .andExpect(jsonPath("$.totalEarned").value(1000));
    }

    @Test
    @DisplayName("POST /api/points/wallet/{userNo}/use - 포인트 사용 200")
    void usePoints() throws Exception {
        pointService.createWallet(USER_NO);
        pointService.earnPoints(USER_NO, PointEarnRequest.builder()
                .amount(1000L).description("충전").build());

        PointUseRequest request = PointUseRequest.builder()
                .amount(300L)
                .description("쿠폰 사용")
                .build();

        mockMvc.perform(post("/api/points/wallet/{userNo}/use", USER_NO)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(700))
                .andExpect(jsonPath("$.totalUsed").value(300));
    }

    @Test
    @DisplayName("GET /api/points/wallet/{userNo}/histories - 이력 조회 200")
    void getHistories() throws Exception {
        pointService.createWallet(USER_NO);
        pointService.earnPoints(USER_NO, PointEarnRequest.builder()
                .amount(1000L).description("적립").build());
        pointService.usePoints(USER_NO, PointUseRequest.builder()
                .amount(300L).description("사용").build());

        mockMvc.perform(get("/api/points/wallet/{userNo}/histories", USER_NO))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].type").value("USE_COUPON"))
                .andExpect(jsonPath("$[1].type").value("EARN"));
    }

    @Test
    @DisplayName("POST /api/points/wallet/{userNo}/earn - amount null이면 400")
    void earnPoints_validation() throws Exception {
        pointService.createWallet(USER_NO);

        String invalidJson = "{\"description\":\"테스트\"}";

        mockMvc.perform(post("/api/points/wallet/{userNo}/earn", USER_NO)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }
}
