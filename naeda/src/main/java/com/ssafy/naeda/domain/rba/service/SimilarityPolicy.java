package com.ssafy.naeda.domain.rba.service;

import com.ssafy.naeda.domain.rba.dto.AuthLevel;
import org.springframework.stereotype.Component;

import java.util.Random;

@Component
public class SimilarityPolicy implements RbaPolicy{

    private static final double CONFIDENT_THRESHOLD = 0.80;
    private static final double BORDERLINE_THRESHOLD = 0.70;

    private final Random random;

    public SimilarityPolicy() {
        this.random = new Random();
    }

    SimilarityPolicy(Random random) {
        this.random = random;
    }

    @Override
    public AuthLevel evaluate(long amount, double similarity) {
        if (similarity >= CONFIDENT_THRESHOLD) {
            return AuthLevel.FACE_ONLY;
        }

        if (similarity >= BORDERLINE_THRESHOLD) {
            return random.nextBoolean() ? AuthLevel.FACE_PHONE : AuthLevel.FACE_PIN;
        }
        return AuthLevel.BLOCKED;
    }
}
