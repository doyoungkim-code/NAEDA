package com.ssafy.naeda.domain.notice.service;

import com.ssafy.naeda.domain.notice.dto.request.NoticeCreateRequest;
import com.ssafy.naeda.domain.notice.dto.request.NoticeUpdateRequest;
import com.ssafy.naeda.domain.notice.dto.response.NoticeResponse;
import com.ssafy.naeda.domain.notice.entity.Notice;
import com.ssafy.naeda.domain.notice.repository.NoticeRepository;
import com.ssafy.naeda.domain.notification.entity.NotificationType;
import com.ssafy.naeda.domain.notification.entity.ReferenceType;
import com.ssafy.naeda.global.exception.BadRequestException;
import com.ssafy.naeda.global.exception.NotFoundException;
import com.ssafy.naeda.global.fcm.FcmSendResult;
import com.ssafy.naeda.global.fcm.FcmService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class NoticeServiceTest {

    @InjectMocks
    private NoticeService noticeService;

    @Mock
    private NoticeRepository noticeRepository;

    @Mock
    private FcmService fcmService;

    private Notice createNotice(Long id, String title, String content, boolean fcmNotified) {
        Notice notice = Notice.builder()
                .title(title)
                .content(content)
                .build();
        ReflectionTestUtils.setField(notice, "noticeId", id);
        if (fcmNotified) {
            notice.markFcmNotified();
        }
        return notice;
    }

    @Test
    @DisplayName("공지사항 등록 - 정상적으로 저장하고 응답을 반환한다")
    void createNotice_success() {
        NoticeCreateRequest request = new NoticeCreateRequest();
        ReflectionTestUtils.setField(request, "title", "서비스 점검 안내");
        ReflectionTestUtils.setField(request, "content", "점검 내용입니다.");

        Notice savedNotice = createNotice(1L, "서비스 점검 안내", "점검 내용입니다.", false);
        given(noticeRepository.save(any(Notice.class))).willReturn(savedNotice);

        NoticeResponse result = noticeService.createNotice(request);

        assertThat(result.getNoticeId()).isEqualTo(1L);
        assertThat(result.getTitle()).isEqualTo("서비스 점검 안내");
        assertThat(result.getContent()).isEqualTo("점검 내용입니다.");
        assertThat(result.getFcmNotified()).isFalse();
    }

    @Test
    @DisplayName("공지사항 목록 조회 - 최신순으로 반환한다")
    void getAllNotices_returnsDescOrder() {
        Notice notice1 = createNotice(1L, "첫 번째 공지", "내용1", false);
        Notice notice2 = createNotice(2L, "두 번째 공지", "내용2", true);
        given(noticeRepository.findAllByOrderByCreatedDesc()).willReturn(List.of(notice2, notice1));

        List<NoticeResponse> result = noticeService.getAllNotices();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getNoticeId()).isEqualTo(2L);
        assertThat(result.get(1).getNoticeId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("공지사항 단건 조회 - 정상 조회")
    void getNotice_success() {
        Notice notice = createNotice(1L, "서비스 점검 안내", "점검 내용", false);
        given(noticeRepository.findById(1L)).willReturn(Optional.of(notice));

        NoticeResponse result = noticeService.getNotice(1L);

        assertThat(result.getNoticeId()).isEqualTo(1L);
        assertThat(result.getTitle()).isEqualTo("서비스 점검 안내");
    }

    @Test
    @DisplayName("공지사항 단건 조회 - 존재하지 않으면 예외를 던진다")
    void getNotice_notFound() {
        given(noticeRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> noticeService.getNotice(999L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("존재하지 않는 공지사항입니다.");
    }

    @Test
    @DisplayName("공지사항 수정 - 정상적으로 수정하고 응답을 반환한다")
    void updateNotice_success() {
        Notice notice = createNotice(1L, "원래 제목", "원래 내용", false);
        given(noticeRepository.findById(1L)).willReturn(Optional.of(notice));

        NoticeUpdateRequest request = new NoticeUpdateRequest();
        ReflectionTestUtils.setField(request, "title", "수정된 제목");
        ReflectionTestUtils.setField(request, "content", "수정된 내용");

        NoticeResponse result = noticeService.updateNotice(1L, request);

        assertThat(result.getTitle()).isEqualTo("수정된 제목");
        assertThat(result.getContent()).isEqualTo("수정된 내용");
    }

    @Test
    @DisplayName("공지사항 수정 - 존재하지 않으면 예외를 던진다")
    void updateNotice_notFound() {
        given(noticeRepository.findById(999L)).willReturn(Optional.empty());

        NoticeUpdateRequest request = new NoticeUpdateRequest();
        ReflectionTestUtils.setField(request, "title", "제목");
        ReflectionTestUtils.setField(request, "content", "내용");

        assertThatThrownBy(() -> noticeService.updateNotice(999L, request))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("공지사항 삭제 - 정상 삭제")
    void deleteNotice_success() {
        Notice notice = createNotice(1L, "삭제할 공지", "내용", false);
        given(noticeRepository.findById(1L)).willReturn(Optional.of(notice));

        noticeService.deleteNotice(1L);

        then(noticeRepository).should().delete(notice);
    }

    @Test
    @DisplayName("공지사항 삭제 - 존재하지 않으면 예외를 던진다")
    void deleteNotice_notFound() {
        given(noticeRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> noticeService.deleteNotice(999L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("FCM 알림 발송 - 정상 발송하고 결과를 반환한다")
    void notifyNotice_success() {
        Notice notice = createNotice(1L, "점검 안내", "점검 내용입니다.", false);
        given(noticeRepository.findById(1L)).willReturn(Optional.of(notice));
        given(fcmService.sendToAllUsers(
                eq("점검 안내"), eq("점검 내용입니다."),
                eq(NotificationType.SYSTEM), eq(1L), eq(ReferenceType.NOTICE)
        )).willReturn(new FcmSendResult(100, 80, 78, 2));

        FcmSendResult result = noticeService.notifyNotice(1L);

        assertThat(result.getTotalUsers()).isEqualTo(100);
        assertThat(result.getSuccessCount()).isEqualTo(78);
        assertThat(notice.getFcmNotified()).isTrue();
    }

    @Test
    @DisplayName("FCM 알림 발송 - 이미 발송된 공지사항이면 예외를 던진다")
    void notifyNotice_alreadyNotified() {
        Notice notice = createNotice(1L, "점검 안내", "점검 내용입니다.", true);
        given(noticeRepository.findById(1L)).willReturn(Optional.of(notice));

        assertThatThrownBy(() -> noticeService.notifyNotice(1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("이미 FCM 알림이 발송된 공지사항입니다.");
    }

    @Test
    @DisplayName("FCM 알림 발송 - 존재하지 않는 공지사항이면 예외를 던진다")
    void notifyNotice_notFound() {
        given(noticeRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> noticeService.notifyNotice(999L))
                .isInstanceOf(NotFoundException.class);
    }
}
