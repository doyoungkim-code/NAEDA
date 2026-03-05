package com.ssafy.naeda.domain.rba.service;

import com.ssafy.naeda.domain.rba.dto.AuthMethod;
import com.ssafy.naeda.domain.rba.dto.RbaResult;
import org.springframework.stereotype.Service;

import java.util.EnumSet;
import java.util.Random;
import java.util.Set;

@Service
public class RbaEngine {

    private static final double CONFIDENT_THRESHOLD = 0.80;
    private static final double BORDERLINE_THRESHOLD = 0.70;
    private static final long HIGH_AMOUNT_THRESHOLD = 50_000L;

    private final Random random;

    public RbaEngine() {
        this.random = new Random();
    }

    // 테스트용 생성자
    RbaEngine(Random random) {
        this.random = random;
    }

    public RbaResult evaluate(long amount, double similarity) {
        // 1단계: 유사도 판단
        if (similarity < BORDERLINE_THRESHOLD) {
            return RbaResult.builder()
                    .requiredMethods(Set.of())
                    .blocked(true)
                    .reason("유사도 " + similarity + " — 매칭 실패")
                    .build();
        }

        Set<AuthMethod> methods = EnumSet.of(AuthMethod.FACE);

        // 2단계: 경계 구간이면 랜덤 2차 인증 추가
        String reason = "유사도 " + similarity;
        if (similarity < CONFIDENT_THRESHOLD) {
            AuthMethod secondAuth = random.nextBoolean() ? AuthMethod.PHONE : AuthMethod.PIN;
            methods.add(secondAuth);
            reason += " — 경계 구간, " + secondAuth + " 추가";
        }

        // 3단계: 고액이면 서명 추가
        if (amount >= HIGH_AMOUNT_THRESHOLD) {
            methods.add(AuthMethod.SIGNATURE);
            reason += " / 고액 결제, 전자서명 추가";
        }

        return RbaResult.builder()
                .requiredMethods(methods)
                .blocked(false)
                .reason(reason)
                .build();
    }
}