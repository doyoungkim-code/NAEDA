package com.ssafy.naeda.domain.notice.dto.response;

import com.ssafy.naeda.domain.notice.entity.Notice;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class NoticeResponse {

    private Long noticeId;
    private String title;
    private String content;
    private Boolean fcmNotified;
    private LocalDateTime created;
    private LocalDateTime modified;

    public static NoticeResponse from(Notice notice) {
        return NoticeResponse.builder()
                .noticeId(notice.getNoticeId())
                .title(notice.getTitle())
                .content(notice.getContent())
                .fcmNotified(notice.getFcmNotified())
                .created(notice.getCreated())
                .modified(notice.getModified())
                .build();
    }
}
