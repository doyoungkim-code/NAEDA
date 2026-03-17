package com.ssafy.naeda.global.fcm;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.ssafy.naeda.domain.notification.entity.Notification;
import com.ssafy.naeda.domain.notification.entity.NotificationType;
import com.ssafy.naeda.domain.notification.entity.ReferenceType;
import com.ssafy.naeda.domain.notification.repository.NotificationRepository;
import com.ssafy.naeda.domain.user.entity.User;
import com.ssafy.naeda.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FcmService {

    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;

    @Transactional
    public FcmSendResult sendToAllUsers(String title, String body,
                                         NotificationType type,
                                         Long referenceId,
                                         ReferenceType referenceType) {
        List<User> users = userRepository.findByFcmTokenIsNotNull();
        int successCount = 0;
        int failCount = 0;

        for (User user : users) {
            // 알림 이력 저장
            Notification notification = Notification.builder()
                    .userNo(user.getUserNo())
                    .type(type)
                    .title(title)
                    .body(body)
                    .referenceId(referenceId)
                    .referenceType(referenceType)
                    .build();
            notificationRepository.save(notification);

            // FCM 발송
            if (sendFcm(user.getFcmToken(), title, body)) {
                successCount++;
            } else {
                failCount++;
            }
        }

        // FCM 토큰이 없는 사용자에게도 알림 이력은 저장
        List<User> allUsers = userRepository.findAll();
        for (User user : allUsers) {
            if (user.getFcmToken() != null) continue;
            Notification notification = Notification.builder()
                    .userNo(user.getUserNo())
                    .type(type)
                    .title(title)
                    .body(body)
                    .referenceId(referenceId)
                    .referenceType(referenceType)
                    .build();
            notificationRepository.save(notification);
        }

        log.info("[FCM] 발송 완료 — 전체: {}, 성공: {}, 실패: {}",
                users.size(), successCount, failCount);

        return new FcmSendResult(allUsers.size(), users.size(), successCount, failCount);
    }

    private boolean sendFcm(String token, String title, String body) {
        if (FirebaseApp.getApps().isEmpty()) {
            log.warn("[FCM] Firebase 미초기화 — 실제 발송 건너뜀 (token={})", maskToken(token));
            return false;
        }

        try {
            Message message = Message.builder()
                    .setToken(token)
                    .setNotification(
                            com.google.firebase.messaging.Notification.builder()
                                    .setTitle(title)
                                    .setBody(body)
                                    .build()
                    )
                    .build();
            String messageId = FirebaseMessaging.getInstance().send(message);
            log.debug("[FCM] 발송 성공 — messageId={}", messageId);
            return true;
        } catch (FirebaseMessagingException e) {
            log.error("[FCM] 발송 실패 — token={}, error={}", maskToken(token), e.getMessage());
            return false;
        }
    }

    private String maskToken(String token) {
        if (token == null || token.length() < 10) return "***";
        return token.substring(0, 6) + "..." + token.substring(token.length() - 4);
    }
}
