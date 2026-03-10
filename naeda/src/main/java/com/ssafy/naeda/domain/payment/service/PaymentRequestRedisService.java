package com.ssafy.naeda.domain.payment.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.naeda.domain.payment.dto.PaymentRequestData;
import com.ssafy.naeda.domain.payment.entity.PaymentRequestStatus;
import com.ssafy.naeda.global.exception.BadRequestException;
import com.ssafy.naeda.global.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentRequestRedisService {

    private static final String REQUEST_KEY_PREFIX = "payment:request:";
    private static final String STORE_INDEX_PREFIX = "payment:store:";
    private static final String LOCK_KEY_PREFIX = "payment:lock:";

    private static final Map<PaymentRequestStatus, Set<PaymentRequestStatus>> VALID_TRANSITIONS = Map.of(
            PaymentRequestStatus.PENDING, Set.of(PaymentRequestStatus.PROCESSING),
            PaymentRequestStatus.PROCESSING, Set.of(PaymentRequestStatus.SUCCESS, PaymentRequestStatus.FAILED, PaymentRequestStatus.BLOCKED),
            PaymentRequestStatus.SUCCESS, Set.of(),
            PaymentRequestStatus.FAILED, Set.of(),
            PaymentRequestStatus.BLOCKED, Set.of()
    );

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${payment.request.ttl-seconds:30}")
    private long ttlSeconds;

    @Value("${payment.request.process-ttl-seconds:300}")
    private long processTtlSeconds;

    /**
     * 결제 요청 생성 — PENDING 상태로 Redis에 저장.
     */
    public PaymentRequestData createRequest(Long storeId, Long amount) {
        String requestId = UUID.randomUUID().toString();
        long now = System.currentTimeMillis();

        PaymentRequestData data = PaymentRequestData.builder()
                .requestId(requestId)
                .storeId(storeId)
                .amount(amount)
                .status(PaymentRequestStatus.PENDING)
                .createdAt(now)
                .updatedAt(now)
                .build();

        redisTemplate.opsForValue().set(requestKey(requestId), toJson(data), ttlSeconds, TimeUnit.SECONDS);
        String indexKey = storeIndexKey(storeId);
        redisTemplate.opsForSet().add(indexKey, requestId);
        redisTemplate.expire(indexKey, ttlSeconds * 2, TimeUnit.SECONDS);

        log.info("결제 요청 생성: requestId={}, storeId={}, amount={}", requestId, storeId, amount);
        return data;
    }

    /**
     * 결제 요청 단건 조회. 만료/미존재 시 null 반환.
     */
    public PaymentRequestData getRequest(String requestId) {
        String json = redisTemplate.opsForValue().get(requestKey(requestId));
        if (json == null) {
            return null;
        }
        return fromJson(json);
    }

    /**
     * 매장별 결제 요청 목록 조회. 만료된 건은 lazy cleanup.
     */
    public List<PaymentRequestData> getRequestsByStore(Long storeId) {
        Set<String> requestIds = redisTemplate.opsForSet().members(storeIndexKey(storeId));
        if (requestIds == null || requestIds.isEmpty()) {
            return List.of();
        }

        List<PaymentRequestData> result = new ArrayList<>();
        for (String requestId : requestIds) {
            PaymentRequestData data = getRequest(requestId);
            if (data == null) {
                redisTemplate.opsForSet().remove(storeIndexKey(storeId), requestId);
            } else {
                result.add(data);
            }
        }
        return result;
    }

    /**
     * 결제 요청 상태 변경. 상태 전이 규칙을 검증하고 TTL을 갱신한다.
     */
    public PaymentRequestData updateStatus(String requestId, PaymentRequestStatus newStatus) {
        PaymentRequestData data = getRequest(requestId);
        if (data == null) {
            throw new NotFoundException("결제 요청이 만료되었거나 존재하지 않습니다.");
        }

        validateTransition(data.getStatus(), newStatus);

        PaymentRequestData updated = PaymentRequestData.builder()
                .requestId(data.getRequestId())
                .storeId(data.getStoreId())
                .amount(data.getAmount())
                .status(newStatus)
                .userNo(data.getUserNo())
                .nextAction(data.getNextAction())
                .paymentId(data.getPaymentId())
                .failureReason(data.getFailureReason())
                .createdAt(data.getCreatedAt())
                .updatedAt(System.currentTimeMillis())
                .build();

        long ttl = (newStatus == PaymentRequestStatus.PENDING) ? ttlSeconds : processTtlSeconds;
        redisTemplate.opsForValue().set(requestKey(requestId), toJson(updated), ttl, TimeUnit.SECONDS);

        log.info("결제 요청 상태 변경: requestId={}, {} → {}", requestId, data.getStatus(), newStatus);
        return updated;
    }

    /**
     * 결제 처리 결과 반영 (userNo, nextAction, paymentId, failureReason).
     */
    public PaymentRequestData updateResult(String requestId, Long userNo, String nextAction,
                                           Long paymentId, String failureReason) {
        PaymentRequestData data = getRequest(requestId);
        if (data == null) {
            throw new NotFoundException("결제 요청이 만료되었거나 존재하지 않습니다.");
        }

        PaymentRequestData updated = PaymentRequestData.builder()
                .requestId(data.getRequestId())
                .storeId(data.getStoreId())
                .amount(data.getAmount())
                .status(data.getStatus())
                .userNo(userNo != null ? userNo : data.getUserNo())
                .nextAction(nextAction != null ? nextAction : data.getNextAction())
                .paymentId(paymentId != null ? paymentId : data.getPaymentId())
                .failureReason(failureReason != null ? failureReason : data.getFailureReason())
                .createdAt(data.getCreatedAt())
                .updatedAt(System.currentTimeMillis())
                .build();

        redisTemplate.opsForValue().set(requestKey(requestId), toJson(updated), processTtlSeconds, TimeUnit.SECONDS);

        log.info("결제 요청 결과 반영: requestId={}, userNo={}, nextAction={}", requestId, userNo, nextAction);
        return updated;
    }

    /**
     * 결제 요청 삭제 — Redis 키 + 매장 인덱스에서 제거.
     */
    public void deleteRequest(String requestId) {
        PaymentRequestData data = getRequest(requestId);
        redisTemplate.delete(requestKey(requestId));
        if (data != null) {
            redisTemplate.opsForSet().remove(storeIndexKey(data.getStoreId()), requestId);
        }
        log.info("결제 요청 삭제: requestId={}", requestId);
    }

    // ── 분산 락 ──────────────────────────────────────────────────────────

    /**
     * 결제 처리 락 획득. SETNX로 원자적 획득 — 이미 락이 있으면 false 반환.
     */
    public boolean tryAcquireProcessingLock(String requestId) {
        Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(LOCK_KEY_PREFIX + requestId, "1", processTtlSeconds, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(acquired);
    }

    /**
     * 결제 처리 락 해제.
     */
    public void releaseProcessingLock(String requestId) {
        redisTemplate.delete(LOCK_KEY_PREFIX + requestId);
    }

    // ── 내부 헬퍼 ──────────────────────────────────────────────────────────

    private void validateTransition(PaymentRequestStatus current, PaymentRequestStatus next) {
        Set<PaymentRequestStatus> allowed = VALID_TRANSITIONS.getOrDefault(current, Set.of());
        if (!allowed.contains(next)) {
            throw new BadRequestException(
                    String.format("잘못된 상태 전환입니다: %s → %s", current, next));
        }
    }

    private String toJson(PaymentRequestData data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("결제 요청 직렬화 실패", e);
        }
    }

    private PaymentRequestData fromJson(String json) {
        try {
            return objectMapper.readValue(json, PaymentRequestData.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("결제 요청 역직렬화 실패", e);
        }
    }

    private String requestKey(String requestId) {
        return REQUEST_KEY_PREFIX + requestId;
    }

    private String storeIndexKey(Long storeId) {
        return STORE_INDEX_PREFIX + storeId + ":requests";
    }
}
