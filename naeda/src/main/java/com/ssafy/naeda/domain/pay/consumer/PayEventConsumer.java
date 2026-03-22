package com.ssafy.naeda.domain.pay.consumer;
import com.ssafy.naeda.domain.notification.entity.NotificationType;
import com.ssafy.naeda.domain.notification.entity.ReferenceType;
import com.ssafy.naeda.domain.pay.event.PayEvent;
import com.ssafy.naeda.domain.point.dto.request.PointEarnRequest;
import com.ssafy.naeda.domain.point.service.PointService;
import com.ssafy.naeda.global.fcm.FcmService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
        name = "spring.kafka.bootstrap-servers",
        matchIfMissing = false
)
public class PayEventConsumer {
    private final PointService pointService;
    private final FcmService fcmService;


    @KafkaListener(
            topics = "payment-events",
            groupId = "pay-point-group",
            containerFactory = "payEventListenerFactory"
    )
    public void handlePointAccumulation(PayEvent event) {
        if (!"SUCCESS".equals(event.getStatus())) return;

        log.info("[Consumer] 포인트 적립 시작: transactionId={}, points={}",
                event.getTransactionId(), event.getEarnedPoints());

        try {
            PointEarnRequest earnRequest = PointEarnRequest.builder()
                    .amount(event.getEarnedPoints())
                    .paymentId(event.getTransactionId())
                    .build();

            pointService.earnPoints(event.getUserNo(), earnRequest);
            log.info("[Consumer] 포인트 적립 완료: userNo={}, points={}",
                    event.getUserNo(), event.getEarnedPoints());
        } catch (Exception e) {
            log.error("[Consumer] 포인트 적립 실패: transactionId={}", event.getTransactionId(), e);
        }
    }

    @KafkaListener(
            topics = "payment-events",
            groupId = "pay-fds-group",
            containerFactory = "payEventListenerFactory"
    )
    public void handleFdsLogging(PayEvent event) {
        log.info("[Consumer] FDS 로그 저장: transactionId={}, fdsScore={}, fdsAction={}",
                event.getTransactionId(), event.getFdsScore(), event.getFdsAction());

        try {
            // TODO: FDS 로그 DB 저장
            log.info("[Consumer] FDS 로그 저장 완료");
        } catch (Exception e) {
            log.error("[Consumer] FDS 로그 저장 실패: transactionId={}", event.getTransactionId(), e);
        }
    }

    @KafkaListener(
            topics = "payment-events",
            groupId = "pay-notification-group",
            containerFactory = "payEventListenerFactory"
    )
    public void handleNotification(PayEvent event) {
        if (!"SUCCESS".equals(event.getStatus())) return;

        log.info("[Consumer] 결제 알림 발송: transactionId={}, userNo={}",
                event.getTransactionId(), event.getUserNo());

        try {
            String formattedAmount = NumberFormat.getNumberInstance(Locale.KOREA)
                    .format(event.getAmount());

            String title = "결제 완료";
            String body = formattedAmount + "원 결제가 완료되었습니다.";

            Map<String, String> data = new HashMap<>();
            data.put("paymentId", String.valueOf(event.getTransactionId()));
            data.put("amount", String.valueOf(event.getAmount()));
            data.put("storeId", String.valueOf(event.getStoreId()));
            if (event.getEarnedPoints() != null) {
                data.put("earnedPoints", String.valueOf(event.getEarnedPoints()));
            }

            fcmService.sendToUser(
                    event.getUserNo(),
                    title,
                    body,
                    NotificationType.PAYMENT,
                    event.getTransactionId(),
                    ReferenceType.PAYMENT,
                    data
            );

            log.info("[Consumer] 결제 알림 발송 완료: userNo={}, amount={}",
                    event.getUserNo(), event.getAmount());
        } catch (Exception e) {
            log.error("[Consumer] 결제 알림 발송 실패: transactionId={}", event.getTransactionId(), e);
        }
    }


}
