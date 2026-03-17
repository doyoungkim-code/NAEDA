package com.ssafy.naeda.domain.pay.consumer;
import com.ssafy.naeda.domain.pay.event.PayEvent;
import com.ssafy.naeda.domain.point.dto.request.PointEarnRequest;
import com.ssafy.naeda.domain.point.service.PointService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor

public class PayEventConsumer {
    // TODO: PointService, NotificationService 주입

    private final PointService pointService;


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

        log.info("[Consumer] 알림 발송: transactionId={}, userNo={}",
                event.getTransactionId(), event.getUserNo());

        try {
            // TODO: notificationService.sendPaymentComplete(event.getUserNo(), event.getAmount())
            log.info("[Consumer] 알림 발송 완료");
        } catch (Exception e) {
            log.error("[Consumer] 알림 발송 실패: transactionId={}", event.getTransactionId(), e);
        }
    }


}
