package com.ssafy.naeda.domain.rba.service;

import org.springframework.stereotype.Service;

@Service
public class SignatureVerificationService {

    /**
     * 고액 결제(5만원 이상)에서 전자서명 완료 여부 검증
     * 프론트에서 서명 완료 후 signatureCompleted = true로 전달
     */
    public boolean verify(long amount, boolean signatureCompleted) {
        if (amount < 50_000L) {
            return true;
        }

        return signatureCompleted;
    }
}
