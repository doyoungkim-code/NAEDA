package com.ssafy.naeda.domain.notice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.naeda.domain.notice.dto.request.NoticeCreateRequest;
import com.ssafy.naeda.domain.notice.dto.request.NoticeUpdateRequest;
import com.ssafy.naeda.domain.notice.dto.response.NoticeResponse;
import com.ssafy.naeda.domain.notice.service.NoticeService;
import com.ssafy.naeda.global.exception.BadRequestException;
import com.ssafy.naeda.global.exception.GlobalExceptionHandler;
import com.ssafy.naeda.global.exception.NotFoundException;
import com.ssafy.naeda.global.fcm.FcmSendResult;
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

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NoticeController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class NoticeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private NoticeService noticeService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @Test
    @DisplayName("공지사항 등록 - 201 Created와 등록된 공지사항을 반환한다")
    void createNotice_returns201() throws Exception {
        NoticeCreateRequest request = new NoticeCreateRequest();
        setField(request, "title", "서비스 점검 안내");
        setField(request, "content", "2026년 3월 25일 새벽 2시~4시 서비스 점검이 예정되어 있습니다.");

        given(noticeService.createNotice(any(NoticeCreateRequest.class))).willReturn(
                NoticeResponse.builder()
                        .noticeId(1L)
                        .title("서비스 점검 안내")
                        .content("2026년 3월 25일 새벽 2시~4시 서비스 점검이 예정되어 있습니다.")
                        .fcmNotified(false)
                        .created(LocalDateTime.of(2026, 3, 23, 10, 0))
                        .build()
        );

        mockMvc.perform(post("/api/notices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.noticeId").value(1))
                .andExpect(jsonPath("$.title").value("서비스 점검 안내"))
                .andExpect(jsonPath("$.fcmNotified").value(false));
    }

    @Test
    @DisplayName("공지사항 등록 - 제목 누락 시 400을 반환한다")
    void createNotice_missingTitle_returns400() throws Exception {
        NoticeCreateRequest request = new NoticeCreateRequest();
        setField(request, "content", "내용만 있는 요청");

        mockMvc.perform(post("/api/notices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("공지사항 목록 조회 - 200과 최신순 목록을 반환한다")
    void getAllNotices_returns200() throws Exception {
        given(noticeService.getAllNotices()).willReturn(List.of(
                NoticeResponse.builder()
                        .noticeId(2L).title("두 번째 공지").content("내용2")
                        .fcmNotified(true).created(LocalDateTime.of(2026, 3, 23, 12, 0))
                        .build(),
                NoticeResponse.builder()
                        .noticeId(1L).title("첫 번째 공지").content("내용1")
                        .fcmNotified(false).created(LocalDateTime.of(2026, 3, 23, 10, 0))
                        .build()
        ));

        mockMvc.perform(get("/api/notices"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].noticeId").value(2))
                .andExpect(jsonPath("$[1].noticeId").value(1));
    }

    @Test
    @DisplayName("공지사항 단건 조회 - 200과 공지사항 정보를 반환한다")
    void getNotice_returns200() throws Exception {
        given(noticeService.getNotice(1L)).willReturn(
                NoticeResponse.builder()
                        .noticeId(1L).title("서비스 점검 안내")
                        .content("점검 내용입니다.")
                        .fcmNotified(false)
                        .created(LocalDateTime.of(2026, 3, 23, 10, 0))
                        .build()
        );

        mockMvc.perform(get("/api/notices/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.noticeId").value(1))
                .andExpect(jsonPath("$.title").value("서비스 점검 안내"));
    }

    @Test
    @DisplayName("공지사항 단건 조회 - 존재하지 않는 ID면 404를 반환한다")
    void getNotice_notFound_returns404() throws Exception {
        given(noticeService.getNotice(999L))
                .willThrow(new NotFoundException("존재하지 않는 공지사항입니다."));

        mockMvc.perform(get("/api/notices/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("존재하지 않는 공지사항입니다."));
    }

    @Test
    @DisplayName("공지사항 수정 - 200과 수정된 공지사항을 반환한다")
    void updateNotice_returns200() throws Exception {
        NoticeUpdateRequest request = new NoticeUpdateRequest();
        setField(request, "title", "수정된 제목");
        setField(request, "content", "수정된 내용");

        given(noticeService.updateNotice(eq(1L), any(NoticeUpdateRequest.class))).willReturn(
                NoticeResponse.builder()
                        .noticeId(1L).title("수정된 제목").content("수정된 내용")
                        .fcmNotified(false)
                        .created(LocalDateTime.of(2026, 3, 23, 10, 0))
                        .modified(LocalDateTime.of(2026, 3, 23, 14, 0))
                        .build()
        );

        mockMvc.perform(put("/api/notices/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.noticeId").value(1))
                .andExpect(jsonPath("$.title").value("수정된 제목"))
                .andExpect(jsonPath("$.content").value("수정된 내용"));
    }

    @Test
    @DisplayName("공지사항 삭제 - 204 No Content를 반환한다")
    void deleteNotice_returns204() throws Exception {
        willDoNothing().given(noticeService).deleteNotice(1L);

        mockMvc.perform(delete("/api/notices/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("공지사항 삭제 - 존재하지 않는 ID면 404를 반환한다")
    void deleteNotice_notFound_returns404() throws Exception {
        willThrow(new NotFoundException("존재하지 않는 공지사항입니다."))
                .given(noticeService).deleteNotice(999L);

        mockMvc.perform(delete("/api/notices/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    @DisplayName("FCM 알림 발송 - 200과 발송 결과를 반환한다")
    void notifyNotice_returns200() throws Exception {
        given(noticeService.notifyNotice(1L))
                .willReturn(new FcmSendResult(100, 80, 78, 2));

        mockMvc.perform(post("/api/notices/1/notify"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.noticeId").value(1))
                .andExpect(jsonPath("$.totalUsers").value(100))
                .andExpect(jsonPath("$.targetUsers").value(80))
                .andExpect(jsonPath("$.successCount").value(78))
                .andExpect(jsonPath("$.failCount").value(2));
    }

    @Test
    @DisplayName("FCM 알림 발송 - 이미 발송된 공지사항이면 400을 반환한다")
    void notifyNotice_alreadyNotified_returns400() throws Exception {
        given(noticeService.notifyNotice(1L))
                .willThrow(new BadRequestException("이미 FCM 알림이 발송된 공지사항입니다."));

        mockMvc.perform(post("/api/notices/1/notify"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("이미 FCM 알림이 발송된 공지사항입니다."));
    }
}
