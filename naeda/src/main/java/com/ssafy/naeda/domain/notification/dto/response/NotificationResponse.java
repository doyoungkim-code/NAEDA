package com.ssafy.naeda.domain.notification.dto.response;

import com.ssafy.naeda.domain.notification.entity.Notification;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class NotificationResponse {

    private Long notificationId;
    private Long userNo;
    private String type;
    private String title;
    private String body;
    private Long referenceId;
    private String referenceType;
    private Boolean isRead;
    private LocalDateTime sent;

    public static NotificationResponse from(Notification notification) {
        return NotificationResponse.builder()
                .notificationId(notification.getNotificationId())
                .userNo(notification.getUserNo())
                .type(notification.getType().name())
                .title(notification.getTitle())
                .body(notification.getBody())
                .referenceId(notification.getReferenceId())
                .referenceType(notification.getReferenceType() != null ? notification.getReferenceType().name() : null)
                .isRead(notification.getIsRead())
                .sent(notification.getSent())
                .build();
    }
}
