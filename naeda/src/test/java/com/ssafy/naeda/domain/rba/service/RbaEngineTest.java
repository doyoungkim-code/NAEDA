package com.ssafy.naeda.domain.rba.service;

import com.ssafy.naeda.domain.rba.dto.AuthMethod;
import com.ssafy.naeda.domain.rba.dto.RbaResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

class RbaEngineTest {

    @Test
    @DisplayName("소액 + 높은 유사도 → FACE만")
    void lowAmount_confident() {
        RbaEngine engine = new RbaEngine();
        RbaResult result = engine.evaluate(30_000L, 0.85);

        assertThat(result.isBlocked()).isFalse();
        assertThat(result.getRequiredMethods()).containsExactly(AuthMethod.FACE);
    }

    @Test
    @DisplayName("소액 + 경계 유사도 + Random true → FACE + PHONE")
    void lowAmount_borderline_phone() {
        Random fixed = new Random() {
            @Override
            public boolean nextBoolean() { return true; }
        };
        RbaEngine engine = new RbaEngine(fixed);
        RbaResult result = engine.evaluate(30_000L, 0.75);

        assertThat(result.isBlocked()).isFalse();
        assertThat(result.getRequiredMethods())
                .containsExactlyInAnyOrder(AuthMethod.FACE, AuthMethod.PHONE);
    }

    @Test
    @DisplayName("소액 + 경계 유사도 + Random false → FACE + PIN")
    void lowAmount_borderline_pin() {
        Random fixed = new Random() {
            @Override
            public boolean nextBoolean() { return false; }
        };
        RbaEngine engine = new RbaEngine(fixed);
        RbaResult result = engine.evaluate(30_000L, 0.75);

        assertThat(result.isBlocked()).isFalse();
        assertThat(result.getRequiredMethods())
                .containsExactlyInAnyOrder(AuthMethod.FACE, AuthMethod.PIN);
    }

    @Test
    @DisplayName("고액 + 높은 유사도 → FACE + SIGNATURE")
    void highAmount_confident() {
        RbaEngine engine = new RbaEngine();
        RbaResult result = engine.evaluate(70_000L, 0.90);

        assertThat(result.isBlocked()).isFalse();
        assertThat(result.getRequiredMethods())
                .containsExactlyInAnyOrder(AuthMethod.FACE, AuthMethod.SIGNATURE);
    }

    @Test
    @DisplayName("고액 + 경계 유사도 + Random true → FACE + PHONE + SIGNATURE")
    void highAmount_borderline_phone() {
        Random fixed = new Random() {
            @Override
            public boolean nextBoolean() { return true; }
        };
        RbaEngine engine = new RbaEngine(fixed);
        RbaResult result = engine.evaluate(70_000L, 0.75);

        assertThat(result.isBlocked()).isFalse();
        assertThat(result.getRequiredMethods())
                .containsExactlyInAnyOrder(AuthMethod.FACE, AuthMethod.PHONE, AuthMethod.SIGNATURE);
    }

    @Test
    @DisplayName("고액 + 경계 유사도 + Random false → FACE + PIN + SIGNATURE")
    void highAmount_borderline_pin() {
        Random fixed = new Random() {
            @Override
            public boolean nextBoolean() { return false; }
        };
        RbaEngine engine = new RbaEngine(fixed);
        RbaResult result = engine.evaluate(70_000L, 0.75);

        assertThat(result.isBlocked()).isFalse();
        assertThat(result.getRequiredMethods())
                .containsExactlyInAnyOrder(AuthMethod.FACE, AuthMethod.PIN, AuthMethod.SIGNATURE);
    }

    @Test
    @DisplayName("유사도 0.70 미만 → 차단")
    void belowBorderline_blocked() {
        RbaEngine engine = new RbaEngine();
        RbaResult result = engine.evaluate(30_000L, 0.65);

        assertThat(result.isBlocked()).isTrue();
        assertThat(result.getRequiredMethods()).isEmpty();
    }

    @Test
    @DisplayName("고액이어도 유사도 실패 → 차단 (금액 무관)")
    void highAmount_failed_blocked() {
        RbaEngine engine = new RbaEngine();
        RbaResult result = engine.evaluate(100_000L, 0.50);

        assertThat(result.isBlocked()).isTrue();
        assertThat(result.getRequiredMethods()).isEmpty();
    }
}