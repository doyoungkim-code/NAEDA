package com.ssafy.naeda.domain.face.exception;

import lombok.Getter;

@Getter
public class FaceException extends RuntimeException {

    private final FaceErrorCode errorCode;

    public FaceException(FaceErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public int getStatus() {
        return errorCode.getStatus();
    }

    public String getCode() {
        return errorCode.getCode();
    }
}
