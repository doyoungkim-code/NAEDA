package com.ssafy.naeda.domain.rba.service;

import com.ssafy.naeda.domain.face.dto.response.FaceMatchStatus;
import com.ssafy.naeda.domain.rba.dto.AuthLevel;
import com.ssafy.naeda.domain.rba.dto.AuthMethod;
import com.ssafy.naeda.domain.rba.dto.RbaResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class RbaEngineTest {

    private RbaEngine engine;

    @BeforeEach
    void setUp() {
        engine = new RbaEngine();
        ReflectionTestUtils.setField(engine, "highAmountThreshold", 50_000L);
    }

    @Test
    @DisplayName("MATCH + 소액이면 FACE_ONLY")
    void match_lowAmount_faceOnly() {
        RbaResult result = engine.evaluate(30_000L, FaceMatchStatus.MATCH, 0.85);

        assertThat(result.isBlocked()).isFalse();
        assertThat(result.getAuthLevel()).isEqualTo(AuthLevel.FACE_ONLY);
        assertThat(result.getRequiredMethods()).containsExactly(AuthMethod.FACE);
    }

    @Test
    @DisplayName("AMBIGUOUS + 소액이면 FACE_PHONE")
    void ambiguous_lowAmount_facePhone() {
        RbaResult result = engine.evaluate(30_000L, FaceMatchStatus.AMBIGUOUS, 0.68);

        assertThat(result.isBlocked()).isFalse();
        assertThat(result.getAuthLevel()).isEqualTo(AuthLevel.FACE_PHONE);
        assertThat(result.getRequiredMethods())
                .containsExactlyInAnyOrder(AuthMethod.FACE, AuthMethod.PHONE);
    }

    @Test
    @DisplayName("MATCH + 고액이면 FACE_SIGNATURE")
    void match_highAmount_faceSignature() {
        RbaResult result = engine.evaluate(70_000L, FaceMatchStatus.MATCH, 0.90);

        assertThat(result.isBlocked()).isFalse();
        assertThat(result.getAuthLevel()).isEqualTo(AuthLevel.FACE_SIGNATURE);
        assertThat(result.getRequiredMethods())
                .containsExactlyInAnyOrder(AuthMethod.FACE, AuthMethod.SIGNATURE);
    }

    @Test
    @DisplayName("AMBIGUOUS + 고액이면 FACE_SIGNATURE(전화+서명)")
    void ambiguous_highAmount_faceSignature() {
        RbaResult result = engine.evaluate(70_000L, FaceMatchStatus.AMBIGUOUS, 0.67);

        assertThat(result.isBlocked()).isFalse();
        assertThat(result.getAuthLevel()).isEqualTo(AuthLevel.FACE_SIGNATURE);
        assertThat(result.getRequiredMethods())
                .containsExactlyInAnyOrder(AuthMethod.FACE, AuthMethod.PHONE, AuthMethod.SIGNATURE);
    }

    @Test
    @DisplayName("NO_MATCH는 금액 무관하게 BLOCKED")
    void noMatch_blocked() {
        RbaResult result = engine.evaluate(30_000L, FaceMatchStatus.NO_MATCH, 0.50);

        assertThat(result.isBlocked()).isTrue();
        assertThat(result.getAuthLevel()).isEqualTo(AuthLevel.BLOCKED);
        assertThat(result.getRequiredMethods()).isEmpty();
    }
}
