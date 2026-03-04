package com.ssafy.naeda.domain.rba.service;

import com.ssafy.naeda.domain.rba.dto.AuthLevel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

class SimilarityPolicyTest {

    @Test
    @DisplayName("유사도 0.80 이상 → FACE_ONLY")
    void confident_returnsFaceOnly() {
        SimilarityPolicy policy = new SimilarityPolicy();
        assertThat(policy.evaluate(10_000L, 0.85)).isEqualTo(AuthLevel.FACE_ONLY);
    }

    @Test
    @DisplayName("유사도 정확히 0.80 → FACE_ONLY")
    void exactConfidentThreshold_returnsFaceOnly() {
        SimilarityPolicy policy = new SimilarityPolicy();
        assertThat(policy.evaluate(10_000L, 0.80)).isEqualTo(AuthLevel.FACE_ONLY);
    }

    @Test
    @DisplayName("경계 구간 + Random true → FACE_PHONE")
    void borderline_randomTrue_returnsFacePhone() {
        Random fixedRandom = new Random() {
            @Override
            public boolean nextBoolean() { return true; }
        };
        SimilarityPolicy policy = new SimilarityPolicy(fixedRandom);
        assertThat(policy.evaluate(10_000L, 0.75)).isEqualTo(AuthLevel.FACE_PHONE);
    }

    @Test
    @DisplayName("경계 구간 + Random false → FACE_PIN")
    void borderline_randomFalse_returnsFacePin() {
        Random fixedRandom = new Random() {
            @Override
            public boolean nextBoolean() { return false; }
        };
        SimilarityPolicy policy = new SimilarityPolicy(fixedRandom);
        assertThat(policy.evaluate(10_000L, 0.75)).isEqualTo(AuthLevel.FACE_PIN);
    }

    @Test
    @DisplayName("유사도 정확히 0.70 → FACE_PHONE 또는 FACE_PIN")
    void exactBorderlineThreshold_returnsBorderlineAuth() {
        SimilarityPolicy policy = new SimilarityPolicy();
        AuthLevel result = policy.evaluate(10_000L, 0.70);
        assertThat(result).isIn(AuthLevel.FACE_PHONE, AuthLevel.FACE_PIN);
    }

    @Test
    @DisplayName("유사도 0.70 미만 → BLOCKED")
    void failed_returnsBlocked() {
        SimilarityPolicy policy = new SimilarityPolicy();
        assertThat(policy.evaluate(10_000L, 0.65)).isEqualTo(AuthLevel.BLOCKED);
    }

    @Test
    @DisplayName("유사도 1.0 → FACE_ONLY (완벽 일치)")
    void perfectMatch_returnsFaceOnly() {
        SimilarityPolicy policy = new SimilarityPolicy();
        assertThat(policy.evaluate(10_000L, 1.0)).isEqualTo(AuthLevel.FACE_ONLY);
    }

    @Test
    @DisplayName("유사도 0.0 → BLOCKED (완전 불일치)")
    void zeroSimilarity_returnsBlocked() {
        SimilarityPolicy policy = new SimilarityPolicy();
        assertThat(policy.evaluate(10_000L, 0.0)).isEqualTo(AuthLevel.BLOCKED);
    }
}