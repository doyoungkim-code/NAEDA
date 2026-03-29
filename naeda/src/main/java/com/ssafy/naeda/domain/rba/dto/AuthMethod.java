package com.ssafy.naeda.domain.rba.dto;

public enum AuthMethod {
    FACE,       // 얼굴 인식 (기본, 항상 포함)
    PHONE,      // 전화번호 가운데 4자리
    PIN,        // PIN 번호
    SIGNATURE   // 전자서명 (5만원 이상)
}