package com.ssafy.naeda.domain.pay.lock;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class PayDistributedLock {

    private final StringRedisTemplate redisTemplate;

    private static final String LOCK_PREFIX = "pay:lock:";
    private static final Duration LOCK_TTL = Duration.ofSeconds(30);

    private static final String RELEASE_SCRIPT =
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                    "return redis.call('del', KEYS[1])" +
                    "else" +
                    "   return 0 " +
                    "end";


    /**
     * 락 획득 시도
     * @return owner UUID(성공) / null(실패)
     * **/

    public String tryAcquire(String key){
        String lockKey = LOCK_PREFIX + key;
        String owner = UUID.randomUUID().toString();

        Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(lockKey,owner,LOCK_TTL);
        if(Boolean.TRUE.equals(acquired)){
            log.info("[Lock] 획득 성공: key={}, owner={}", lockKey, owner);
            return owner;
        }

        log.warn("[Lock] 획득 실패 (이미 처리 중): key={}", lockKey);
        return null;
    }

    /**
     * 락 해제 (Lua 스크립트로 owner 검증 후 원자적 삭제)
     */
    public boolean release(String key, String owner) {
        String lockKey = LOCK_PREFIX + key;

        DefaultRedisScript<Long> script = new DefaultRedisScript<>(RELEASE_SCRIPT, Long.class);
        Long result = redisTemplate.execute(script, Collections.singletonList(lockKey), owner);

        boolean released = result != null && result == 1L;
        if (released) {
            log.info("[Lock] 해제 성공: key={}, owner={}", lockKey, owner);
        } else {
            log.warn("[Lock] 해제 실패 (owner 불일치 또는 만료): key={}, owner={}", lockKey, owner);
        }
        return released;
    }
}
