package com.ssafy.naeda.domain.user.service;

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

        user.updateFacePaySettings(true, enableSecondaryAuth);

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
}
