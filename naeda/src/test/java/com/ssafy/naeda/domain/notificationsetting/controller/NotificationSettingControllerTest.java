package com.ssafy.naeda.domain.notificationsetting.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.naeda.domain.notificationsetting.dto.request.NotificationSettingRequest;
import com.ssafy.naeda.domain.notificationsetting.dto.response.NotificationSettingResponse;
import com.ssafy.naeda.domain.notificationsetting.service.NotificationSettingService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NotificationSettingController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class NotificationSettingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private NotificationSettingService notificationSettingService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    private NotificationSettingResponse defaultResponse() {
        return NotificationSettingResponse.builder()
                .settingId(1L)
                .paymentEnabled(true)
                .fdsEnabled(true)
                .festivalEnabled(true)
                .pointEnabled(true)
                .systemEnabled(true)
                .build();
    }

    // ── GET /api/notification-settings/{userNo} ───────────────────────────

    @Test
    @DisplayName("알림 설정 조회 - 200")
    void getSetting() throws Exception {
        given(notificationSettingService.getSetting(1L)).willReturn(defaultResponse());

        mockMvc.perform(get("/api/notification-settings/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.settingId").value(1))
                .andExpect(jsonPath("$.paymentEnabled").value(true))
                .andExpect(jsonPath("$.fdsEnabled").value(true));
    }

    @Test
    @DisplayName("알림 설정 조회 - 404")
    void getSetting_notFound() throws Exception {
        given(notificationSettingService.getSetting(999L))
                .willThrow(new NotFoundException("알림 설정을 찾을 수 없습니다."));

        mockMvc.perform(get("/api/notification-settings/999"))
                .andExpect(status().isNotFound());
    }

    // ── POST /api/notification-settings/{userNo} ──────────────────────────

    @Test
    @DisplayName("알림 설정 생성 - 201")
    void createSetting() throws Exception {
        given(notificationSettingService.createSetting(1L)).willReturn(defaultResponse());

        mockMvc.perform(post("/api/notification-settings/1"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.settingId").value(1))
                .andExpect(jsonPath("$.paymentEnabled").value(true));
    }

    // ── PUT /api/notification-settings/{userNo} ───────────────────────────

    @Test
    @DisplayName("알림 설정 수정 - 200")
    void updateSetting() throws Exception {
        NotificationSettingResponse updated = NotificationSettingResponse.builder()
                .settingId(1L)
                .paymentEnabled(false)
                .fdsEnabled(true)
                .festivalEnabled(false)
                .pointEnabled(true)
                .systemEnabled(false)
                .build();

        given(notificationSettingService.updateSetting(eq(1L), any(NotificationSettingRequest.class)))
                .willReturn(updated);

        NotificationSettingRequest request = NotificationSettingRequest.builder()
                .paymentEnabled(false)
                .fdsEnabled(true)
                .festivalEnabled(false)
                .pointEnabled(true)
                .systemEnabled(false)
                .build();

        mockMvc.perform(put("/api/notification-settings/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentEnabled").value(false))
                .andExpect(jsonPath("$.festivalEnabled").value(false))
                .andExpect(jsonPath("$.systemEnabled").value(false));
    }

    @Test
    @DisplayName("알림 설정 수정 - 필수값 누락 400")
    void updateSetting_badRequest() throws Exception {
        String invalidJson = "{\"paymentEnabled\": true}";

        mockMvc.perform(put("/api/notification-settings/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }
}
