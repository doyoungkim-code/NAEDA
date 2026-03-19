package com.ssafy.naeda.domain.pay.service;
import com.ssafy.naeda.domain.pay.entity.PayRequestStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class PayRequestRedisService {
    private final StringRedisTemplate redisTemplate;

    private static final String REQUEST_KEY_PREFIX = "pay:request:";
    private static final String STORE_INDEX_PREFIX = "pay:store:";
    private static final Duration REQUEST_TTL = Duration.ofMinutes(5);

    // ========================
    // 결제 요청 생성 (PENDING)
    // ========================
    /**
     * 결제 요청을 Redis에 생성 (상태: PENDING, TTL: 60초)
     */

    public void createRequest(Long requestId, Long storeId, Long amount){
        String key = REQUEST_KEY_PREFIX + requestId;

        Map<String, String> data = new HashMap<>();
        data.put("status", PayRequestStatus.PENDING.name());
        data.put("storeId", String.valueOf(storeId));
        data.put("amount", String.valueOf(amount));
        data.put("createdAt", String.valueOf(System.currentTimeMillis()));

        redisTemplate.opsForHash().putAll(key,data);
        redisTemplate.expire(key,REQUEST_TTL);

        //매장별 인덱스에 추가
        String storeKey = STORE_INDEX_PREFIX + storeId + ":request";
        redisTemplate.opsForSet().add(storeKey, String.valueOf(requestId));
        redisTemplate.expire(storeKey, REQUEST_TTL);

        log.info("[PayRequest] 생성: requestId={}, storeId={}, status=PENDING", requestId, storeId);
    }

    // ========================
    // 상태 조회
    // ========================

    /**
     * 결제 요청의 현재 상태 조회
     */

    public PayRequestStatus getStatus(Long requestId){
        String key = REQUEST_KEY_PREFIX + requestId;
        String status = (String)redisTemplate.opsForHash().get(key, "status");

        if(status == null){
            return null; //만료되었거나 존재하지 않음.
        }
        return PayRequestStatus.valueOf(status);
    }

    /**
     * 결제 요청 전체 데이터 조회
     */

    public Map<Object,Object> getRequest(Long requestId){
        String key = REQUEST_KEY_PREFIX + requestId;
        Map<Object, Object> data = redisTemplate.opsForHash().entries(key);
        return data.isEmpty() ? null : data;
    }

    /**
     * 매장별 결제 요청 ID 목록 조회
     */

    public Set<String> getStoreRequests(Long storeId){
        String storeKey = STORE_INDEX_PREFIX + storeId + ":request";
        return redisTemplate.opsForSet().members(storeKey);
    }

    // ========================
    // 상태 전이 (State Machine)
    // ========================

    /**
     * 상태 전이 (유효한 전이만 허용)
     * @return true: 전이 성공 / false: 잘못된 전이
     */
    public boolean transition(Long requestId, PayRequestStatus targetStatus){
        String key = REQUEST_KEY_PREFIX + requestId;
        PayRequestStatus currentStatus = getStatus(requestId);

        if(currentStatus == null){
            log.error("[PayRequest] 존재하지 않음: requestId={}", requestId);
            return false;
        }

        if(!currentStatus.canTransitionTo(targetStatus)){
            log.error("[PayRequest] 잘못된 상태 전이: requestId={}, {}->{}",
                    requestId,currentStatus,targetStatus);
            return false;
        }
        redisTemplate.opsForHash().put(key,"status",targetStatus.name());

        // 최종 상태면 TTL 연장 (조회용으로 잠시 유지)
        if (targetStatus == PayRequestStatus.SUCCESS
                || targetStatus == PayRequestStatus.FAILED
                || targetStatus == PayRequestStatus.BLOCKED) {
            redisTemplate.expire(key, Duration.ofMinutes(5));
        }

        log.info("[PayRequest] 상태 전이: requestId={}, {} → {}", requestId, currentStatus, targetStatus);
        return true;
    }

    /**
     * 결과 데이터 추가 (성공 시 transactionId, 실패 시 reason 등)
     */
    public void setResultData(Long requestId, Map<String, String> resultData) {
        String key = REQUEST_KEY_PREFIX + requestId;
        redisTemplate.opsForHash().putAll(key, resultData);
    }

    // ========================
    // 정리
    // ========================

    /**
     * 매장 인덱스에서 요청 제거
     */
    public void removeFromStoreIndex(Long storeId, Long requestId) {
        String storeKey = STORE_INDEX_PREFIX + storeId + ":request";
        redisTemplate.opsForSet().remove(storeKey, String.valueOf(requestId));
    }

}

