package com.ssafy.naeda.domain.rba.service;

import com.ssafy.naeda.domain.rba.dto.AuthLevel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AmountPolicyTest {

    private final AmountPolicy policy = new AmountPolicy();

    @Test
    @DisplayName("5만원 미만 → FACE_ONLY")
    void underThreshold_returnseFaceOnly() {
        AuthLevel result = policy.evaluate(49_999L, 0.3);
        assertThat(result).isEqualTo(AuthLevel.FACE_ONLY);
    }

    @Test
    @DisplayName("정확히 5만원 → FACE_SIGNATURE")
    void exactThreshold_returnsFaceSignature() {
        AuthLevel result = policy.evaluate(50_000L, 0.3);
        assertThat(result).isEqualTo(AuthLevel.FACE_SIGNATURE);
    }

    @Test
    @DisplayName("5만원 초과 → FACE_SIGNATURE")
    void overThreshold_returnsFaceSignature() {
        AuthLevel result = policy.evaluate(100_000L, 0.3);
        assertThat(result).isEqualTo(AuthLevel.FACE_SIGNATURE);
    }

    @Test
    @DisplayName("0원 → FACE_ONLY")
    void zeroAmount_returnsFaceOnly() {
        AuthLevel result = policy.evaluate(0L, 0.3);
        assertThat(result).isEqualTo(AuthLevel.FACE_ONLY);
    }
}