package com.ssafy.naeda.domain.pay.entity;

public enum PayStatus {
    SUCCESS,
    FAILED,
    CANCELLED,
    BLOCKED,
    PAUSED       // 기존 코드에 빠져있던 것. FDS 심사 보류용
}
