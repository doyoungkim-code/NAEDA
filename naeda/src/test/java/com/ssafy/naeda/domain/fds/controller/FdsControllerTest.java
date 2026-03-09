package com.ssafy.naeda.domain.fds.controller;

import com.ssafy.naeda.domain.fds.entity.FdsAction;
import com.ssafy.naeda.domain.fds.entity.FdsLog;
import com.ssafy.naeda.domain.fds.service.FdsRuleService;
import com.ssafy.naeda.global.exception.NotFoundException;
import com.ssafy.naeda.global.security.JwtAuthenticationFilter;
import com.ssafy.naeda.global.security.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FdsController.class)
@AutoConfigureMockMvc(addFilters = false)
class FdsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FdsRuleService fdsRuleService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    private FdsLog sampleLog(Long fdsId, Long paymentId, Long userNo, int score, FdsAction action) {
        return FdsLog.builder()
                .paymentId(paymentId)
                .userNo(userNo)
                .anomalyScore(score)
                .triggeredRules(List.of("LATE_NIGHT"))
                .actionTaken(action)
                .build();
    }

    @Test
    @DisplayName("GET /api/fds/logs - 사용자별 FDS 로그 조회")
    void getLogsByUserNo() throws Exception {
        when(fdsRuleService.getLogsByUserNo(1L))
                .thenReturn(List.of(
                        sampleLog(1L, 10L, 1L, 33, FdsAction.ALERT),
                        sampleLog(2L, 20L, 1L, 70, FdsAction.PAUSE)
                ));

        mockMvc.perform(get("/api/fds/logs").param("userNo", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].anomalyScore").value(33))
                .andExpect(jsonPath("$[0].actionTaken").value("ALERT"))
                .andExpect(jsonPath("$[1].anomalyScore").value(70));
    }

    @Test
    @DisplayName("GET /api/fds/logs - userNo 음수면 400")
    void getLogsByUserNo_invalidParam() throws Exception {
        mockMvc.perform(get("/api/fds/logs").param("userNo", "-1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/fds/logs/payment/{paymentId} - 결제별 FDS 로그 조회")
    void getLogByPaymentId() throws Exception {
        when(fdsRuleService.getLogByPaymentId(10L))
                .thenReturn(sampleLog(1L, 10L, 1L, 45, FdsAction.ALERT));

        mockMvc.perform(get("/api/fds/logs/payment/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value(10))
                .andExpect(jsonPath("$.anomalyScore").value(45))
                .andExpect(jsonPath("$.actionTaken").value("ALERT"));
    }

    @Test
    @DisplayName("GET /api/fds/logs/payment/{paymentId} - 없으면 404")
    void getLogByPaymentId_notFound() throws Exception {
        when(fdsRuleService.getLogByPaymentId(999L))
                .thenThrow(new NotFoundException("해당 결제의 FDS 로그가 없습니다: 999"));

        mockMvc.perform(get("/api/fds/logs/payment/999"))
                .andExpect(status().isNotFound());
    }
}
