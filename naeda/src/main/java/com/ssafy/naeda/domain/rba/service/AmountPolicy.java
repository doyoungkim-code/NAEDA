package com.ssafy.naeda.domain.rba.service;

import com.ssafy.naeda.domain.rba.dto.AuthLevel;
import org.springframework.stereotype.Component;

@Component
public class AmountPolicy implements RbaPolicy{

    private static final long HIGH_AMOUNT_THRESHOLD = 50_000L;

    @Override
    public AuthLevel evaluate(long amount, double similarity) {
        if (amount >= HIGH_AMOUNT_THRESHOLD) {
            return AuthLevel.FACE_SIGNATURE;
        }

        return AuthLevel.FACE_ONLY;
    }
}
