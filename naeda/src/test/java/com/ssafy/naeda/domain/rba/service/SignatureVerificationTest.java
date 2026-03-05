package com.ssafy.naeda.domain.rba.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SignatureVerificationServiceTest {

    private final SignatureVerificationService service = new SignatureVerificationService();

    @Test
    @DisplayName("소액 + 서명 없음 → 통과")
    void lowAmount_noSignature_pass() {
        assertThat(service.verify(30_000L, false)).isTrue();
    }

    @Test
    @DisplayName("고액 + 서명 완료 → 통과")
    void highAmount_signed_pass() {
        assertThat(service.verify(70_000L, true)).isTrue();
    }

    @Test
    @DisplayName("고액 + 서명 미완료 → 실패")
    void highAmount_notSigned_fail() {
        assertThat(service.verify(70_000L, false)).isFalse();
    }

    @Test
    @DisplayName("정확히 5만원 + 서명 완료 → 통과")
    void exactThreshold_signed_pass() {
        assertThat(service.verify(50_000L, true)).isTrue();
    }

    @Test
    @DisplayName("정확히 5만원 + 서명 미완료 → 실패")
    void exactThreshold_notSigned_fail() {
        assertThat(service.verify(50_000L, false)).isFalse();
    }
}