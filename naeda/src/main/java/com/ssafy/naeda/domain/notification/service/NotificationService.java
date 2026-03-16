package com.ssafy.naeda.domain.notification.service;

import com.ssafy.naeda.domain.notification.dto.response.NotificationResponse;
import com.ssafy.naeda.domain.notification.entity.Notification;
import com.ssafy.naeda.domain.notification.repository.NotificationRepository;
import com.ssafy.naeda.global.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public List<NotificationResponse> getUserNotifications(Long userNo) {
        return notificationRepository.findByUserNoOrderBySentDesc(userNo)
                .stream()
                .map(NotificationResponse::from)
                .toList();
    }

    @Transactional
    public void markAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotFoundException("존재하지 않는 알림입니다."));
        notification.markAsRead();
    }

    public long getUnreadCount(Long userNo) {
        return notificationRepository.countByUserNoAndIsReadFalse(userNo);
    }
}
