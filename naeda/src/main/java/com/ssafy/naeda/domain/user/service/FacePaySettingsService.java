package com.ssafy.naeda.domain.user.service;

import com.ssafy.naeda.domain.face.exception.FaceException;
import com.ssafy.naeda.domain.face.service.FaceRegistrationSessionService;
import com.ssafy.naeda.domain.pay.dto.request.PayLimitRequest;
import com.ssafy.naeda.domain.pay.service.PayLimitService;
import com.ssafy.naeda.domain.pay.service.PayMethodService;
import com.ssafy.naeda.domain.user.dto.request.UpdateFacePaySettingsRequest;
import com.ssafy.naeda.domain.user.dto.response.FacePaySettingsResponse;
import com.ssafy.naeda.domain.user.entity.User;
import com.ssafy.naeda.domain.user.repository.UserRepository;
import com.ssafy.naeda.global.exception.AuthenticationFailedException;
import com.ssafy.naeda.global.exception.BadRequestException;
import com.ssafy.naeda.global.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FacePaySettingsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final FaceRegistrationSessionService faceRegistrationSessionService;
    private final PayMethodService payMethodService;
    private final PayLimitService payLimitService;

    public FacePaySettingsResponse getSettings(String userId) {
        User user = findUser(userId);
        return FacePaySettingsResponse.from(user, null);
    }

    @Transactional
    public FacePaySettingsResponse updateSettings(String userId, UpdateFacePaySettingsRequest request) {
        User user = findUser(userId);
        boolean enableSecondaryAuth = Boolean.TRUE.equals(request.getEnableSecondaryAuth());

        if (enableSecondaryAuth) {
            validateCurrentPin(user, request.getCurrentPin());
        }

        boolean hasPendingRegistration = faceRegistrationSessionService.hasActiveSession(userId);
        if (hasPendingRegistration) {
            validateRegistrationRequest(request);
            if (!faceRegistrationSessionService.isRegistrationReady(userId)) {
                throw new FaceException(com.ssafy.naeda.domain.face.exception.FaceErrorCode.REGISTRATION_INCOMPLETE);
            }
            faceRegistrationSessionService.persistPendingEmbeddings(userId);
            payMethodService.setFacePay(user.getUserNo(), request.getPaymentMethodId());
            payLimitService.setLimit(user.getUserNo(), PayLimitRequest.builder()
                    .dailyLimit(request.getDailyLimit())
                    .monthlyLimit(request.getMonthlyLimit())
                    .singleTransactionLimit(request.getSingleTransactionLimit())
                    .build());
        } else if (!Boolean.TRUE.equals(user.getFaceRegistered())) {
            throw new BadRequestException("얼굴 등록이 완료되지 않았습니다.");
        }

        user.updateFacePaySettings(true, enableSecondaryAuth);

        if (hasPendingRegistration) {
            faceRegistrationSessionService.clearSession(userId);
        }

        String message = enableSecondaryAuth
                ? "페이스페이 등록과 PIN 2차 인증 설정이 완료되었습니다."
                : "페이스페이 등록이 완료되었습니다.";
        return FacePaySettingsResponse.from(user, message);
    }

    private User findUser(String userId) {
        return userRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));
    }

    private void validateCurrentPin(User user, String currentPin) {
        String currentHashedPin = user.getPinPassword();
        if (currentHashedPin == null || currentHashedPin.isBlank()) {
            throw new BadRequestException("현재 PIN이 설정되어 있지 않습니다.");
        }
        if (currentPin == null || currentPin.isBlank()) {
            throw new BadRequestException("현재 PIN을 입력해주세요.");
        }
        if (!passwordEncoder.matches(currentPin, currentHashedPin)) {
            throw new AuthenticationFailedException("현재 PIN이 일치하지 않습니다.");
        }
    }

    private void validateRegistrationRequest(UpdateFacePaySettingsRequest request) {
        if (request.getPaymentMethodId() == null) {
            throw new BadRequestException("대표 결제수단을 선택해주세요.");
        }
        if (request.getDailyLimit() == null || request.getMonthlyLimit() == null || request.getSingleTransactionLimit() == null) {
            throw new BadRequestException("결제 한도 정보를 모두 입력해주세요.");
        }
    }
}
