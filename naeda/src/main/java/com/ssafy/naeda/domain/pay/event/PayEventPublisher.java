package com.ssafy.naeda.domain.pay.event;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PayEventPublisher {

    private final KafkaTemplate<String, PayEvent> kafkaTemplate;

    // Kafka 없는 로컬 환경에서도 동작하도록 null-safe 처리

    private static final String TOPIC = "payment-events";

    public void publish(PayEvent event){
        kafkaTemplate.send(TOPIC, String.valueOf(event.getTransactionId()), event)
                .whenComplete((result,ex) -> {
                    if(ex != null){
                        log.error("[Kafka] 이벤트 발행 실패: transactionId={}", event.getTransactionId(), ex);
                    }else{
                        log.info("[Kafka] 이벤트 발행 성공: transactionId={}, topic={}", event.getTransactionId(), TOPIC);
                    }
                });
    }
}
