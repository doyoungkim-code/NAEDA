package com.ssafy.naeda.domain.pay.lock;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class PayRateLimiter {
    private final StringRedisTemplate redisTemplate;

    private static final String RATE_KEY_PREFIX = "pay:rate:";
    private static final int MAX_REQUESTS = 10;
    private static final Duration WINDOW_SIZE = Duration.ofMinutes(1);

    /**
     * 슬라이딩 윈도우 방식 Rate Limit 체크
     * @return true: 허용 / false: 차단 (분당 10건 초과)
     */

    public boolean isAllowed(Long userNo){
        String key = RATE_KEY_PREFIX + userNo;
        long now = Instant.now().toEpochMilli();
        long windowStart = now - WINDOW_SIZE.toMillis();

        ZSetOperations<String, String> zSet = redisTemplate.opsForZSet();

        // 1) 윈도우 밖 오래된 요청 제거
        zSet.removeRangeByScore(key,0,windowStart);

        // 2) 현재 윈도우 내 요청 수 확인
        Long count = zSet.zCard(key);

        if(count != null && count >= MAX_REQUESTS){
            log.warn("[RateLimit] 초과: userNo={}, count={}/{}", userNo, count, MAX_REQUESTS);
            return false;
        }

        // 3) 현재 요청 추가
        zSet.add(key, UUID.randomUUID().toString(),now);

        // 4) 키 만료 설정
        redisTemplate.expire(key,WINDOW_SIZE.plusSeconds(10));

        log.debug("[RateLimit] 허용: userNo={}, count={}/{}", userNo, (count != null ? count + 1 : 1), MAX_REQUESTS);
        return true;
    }
}
