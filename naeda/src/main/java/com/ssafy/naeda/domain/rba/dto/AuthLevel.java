package com.ssafy.naeda.domain.rba.dto;

public enum AuthLevel {
    FACE_ONLY,       // 얼굴만으로 통과
    FACE_PHONE,      // 얼굴 + 전화번호 4자리
    FACE_PIN,        // 얼굴 + PIN 번호
    FACE_SIGNATURE,  // 얼굴 + 전자서명 (5만원 이상)
    BLOCKED;         // 결제 차단

    /**
     * 더 높은(엄격한) 인증 레벨을 반환
     */
    public AuthLevel stricter(AuthLevel other) {
        return this.ordinal() >= other.ordinal() ? this : other;
    }
}