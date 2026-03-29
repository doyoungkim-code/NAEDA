package com.ssafy.naeda.global.fcm;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.ssafy.naeda.domain.notification.entity.Notification;
import com.ssafy.naeda.domain.notification.entity.NotificationType;
import com.ssafy.naeda.domain.notification.entity.ReferenceType;
import com.ssafy.naeda.domain.notification.repository.NotificationRepository;
import com.ssafy.naeda.domain.notificationsetting.repository.NotificationSettingRepository;
import com.ssafy.naeda.domain.user.entity.User;
import com.ssafy.naeda.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class FcmService {

    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationSettingRepository notificationSettingRepository;

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

            // 알림 설정 확인
            boolean enabled = notificationSettingRepository.findByUserNo(user.getUserNo())
                    .map(setting -> switch (type) {
                        case PAYMENT -> setting.getPaymentEnabled();
                        case FDS_ALERT -> setting.getFdsEnabled();
                        case FESTIVAL -> setting.getFestivalEnabled();
                        case POINT -> setting.getPointEnabled();
                        case SYSTEM -> setting.getSystemEnabled();
                    })
                    .orElse(true);

            if (!enabled) {
                log.info("[FCM] 알림 비활성화 상태 (전체 발송 제외): userNo={}, type={}", user.getUserNo(), type);
                continue;
            }

            // FCM 발송
            if (sendFcm(user.getFcmToken(), title, body, null)) {
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

    @Transactional
    public boolean sendToUser(Long userNo, String title, String body,
                              NotificationType type,
                              Long referenceId,
                              ReferenceType referenceType,
                              Map<String, String> data) {
        User user = userRepository.findByUserNo(userNo).orElse(null);
        if (user == null) {
            log.warn("[FCM] 사용자 없음: userNo={}", userNo);
            return false;
        }

        // 알림 설정 확인
        boolean enabled = notificationSettingRepository.findByUserNo(userNo)
                .map(setting -> switch (type) {
                    case PAYMENT -> setting.getPaymentEnabled();
                    case FDS_ALERT -> setting.getFdsEnabled();
                    case FESTIVAL -> setting.getFestivalEnabled();
                    case POINT -> setting.getPointEnabled();
                    case SYSTEM -> setting.getSystemEnabled();
                })
                .orElse(true);

        if (!enabled) {
            log.info("[FCM] 알림 비활성화 상태: userNo={}, type={}", userNo, type);
            return false;
        }

        // 알림 이력 저장
        Notification notification = Notification.builder()
                .userNo(userNo)
                .type(type)
                .title(title)
                .body(body)
                .referenceId(referenceId)
                .referenceType(referenceType)
                .build();
        notificationRepository.save(notification);

        // FCM 발송
        if (user.getFcmToken() == null) {
            log.info("[FCM] 토큰 없음 (알림 이력만 저장): userNo={}", userNo);
            return false;
        }

        // data에 공통 필드 추가
        Map<String, String> payload = new HashMap<>();
        payload.put("type", type.name());
        if (referenceId != null) payload.put("referenceId", referenceId.toString());
        if (referenceType != null) payload.put("referenceType", referenceType.name());
        if (data != null) payload.putAll(data);

        return sendFcm(user.getFcmToken(), title, body, payload);
    }

    private boolean sendFcm(String token, String title, String body, Map<String, String> data) {
        if (FirebaseApp.getApps().isEmpty()) {
            log.warn("[FCM] Firebase 미초기화 — 실제 발송 건너뜀 (token={})", maskToken(token));
            return false;
        }

        try {
            Message.Builder messageBuilder = Message.builder()
                    .setToken(token)
                    .setNotification(
                            com.google.firebase.messaging.Notification.builder()
                                    .setTitle(title)
                                    .setBody(body)
                                    .build()
                    );
            if (data != null && !data.isEmpty()) {
                messageBuilder.putAllData(data);
            }
            Message message = messageBuilder.build();
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
