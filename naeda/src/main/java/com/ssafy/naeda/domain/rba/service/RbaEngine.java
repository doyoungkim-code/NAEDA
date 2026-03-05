package com.ssafy.naeda.domain.rba.service;

import com.ssafy.naeda.domain.face.dto.response.FaceMatchStatus;
import com.ssafy.naeda.domain.rba.dto.AuthLevel;
import com.ssafy.naeda.domain.rba.dto.AuthMethod;
import com.ssafy.naeda.domain.rba.dto.RbaResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.EnumSet;
import java.util.Set;

@Service
public class RbaEngine {

    @Value("${rba.high-amount:50000}")
    private long highAmountThreshold;

    public RbaResult evaluate(long amount, FaceMatchStatus faceStatus, double similarity) {
        if (faceStatus == FaceMatchStatus.NO_MATCH) {
            return RbaResult.builder()
                    .authLevel(AuthLevel.BLOCKED)
                    .requiredMethods(Set.of())
                    .blocked(true)
                    .reason("얼굴 매칭 실패: similarity=" + similarity)
                    .build();
        }

        Set<AuthMethod> methods = EnumSet.of(AuthMethod.FACE);
        boolean highAmount = amount >= highAmountThreshold;

        if (faceStatus == FaceMatchStatus.AMBIGUOUS) {
            methods.add(AuthMethod.PHONE);
            AuthLevel level = AuthLevel.FACE_PHONE;
            String reason = "애매한 매칭 구간: PHONE 2차 인증 필요";
            if (highAmount) {
                methods.add(AuthMethod.SIGNATURE);
                level = AuthLevel.FACE_SIGNATURE;
                reason += " + 고액 결제 서명";
            }
            return RbaResult.builder()
                    .authLevel(level)
                    .requiredMethods(methods)
                    .blocked(false)
                    .reason(reason + " (similarity=" + similarity + ", amount=" + amount + ")")
                    .build();
        }

        // MATCH
        AuthLevel level = AuthLevel.FACE_ONLY;
        String reason = "매칭 성공: 얼굴만으로 인증";
        if (highAmount) {
            methods.add(AuthMethod.SIGNATURE);
            level = AuthLevel.FACE_SIGNATURE;
            reason = "고액 결제: 얼굴 + 전자서명";
        }

        return RbaResult.builder()
                .authLevel(level)
                .requiredMethods(methods)
                .blocked(false)
                .reason(reason + " (similarity=" + similarity + ", amount=" + amount + ")")
                .build();
    }
}
