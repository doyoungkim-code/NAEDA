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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NoticeService {

    private final NoticeRepository noticeRepository;
    private final FcmService fcmService;

    @Transactional
    public NoticeResponse createNotice(NoticeCreateRequest request) {
        Notice notice = Notice.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .build();

        return NoticeResponse.from(noticeRepository.save(notice));
    }

    public List<NoticeResponse> getAllNotices() {
        return noticeRepository.findAllByOrderByCreatedDesc()
                .stream()
                .map(NoticeResponse::from)
                .toList();
    }

    public NoticeResponse getNotice(Long noticeId) {
        return NoticeResponse.from(findNoticeOrThrow(noticeId));
    }

    @Transactional
    public NoticeResponse updateNotice(Long noticeId, NoticeUpdateRequest request) {
        Notice notice = findNoticeOrThrow(noticeId);
        notice.update(request.getTitle(), request.getContent());
        return NoticeResponse.from(notice);
    }

    @Transactional
    public void deleteNotice(Long noticeId) {
        Notice notice = findNoticeOrThrow(noticeId);
        noticeRepository.delete(notice);
    }

    @Transactional
    public FcmSendResult notifyNotice(Long noticeId) {
        Notice notice = findNoticeOrThrow(noticeId);

        if (Boolean.TRUE.equals(notice.getFcmNotified())) {
            throw new BadRequestException("이미 FCM 알림이 발송된 공지사항입니다.");
        }

        notice.markFcmNotified();

        return fcmService.sendToAllUsers(
                notice.getTitle(),
                notice.getContent(),
                NotificationType.SYSTEM,
                notice.getNoticeId(),
                ReferenceType.NOTICE
        );
    }

    private Notice findNoticeOrThrow(Long noticeId) {
        return noticeRepository.findById(noticeId)
                .orElseThrow(() -> new NotFoundException("존재하지 않는 공지사항입니다."));
    }
}
