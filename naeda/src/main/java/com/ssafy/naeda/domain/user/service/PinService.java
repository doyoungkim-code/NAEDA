package com.ssafy.naeda.domain.user.service;

import com.ssafy.naeda.domain.user.dto.request.ResetPinWithPasswordRequest;
import com.ssafy.naeda.domain.user.dto.request.UpdatePinRequest;
import com.ssafy.naeda.domain.user.dto.request.VerifyPasswordRequest;
import com.ssafy.naeda.domain.user.dto.request.VerifyPinRequest;
import com.ssafy.naeda.domain.user.dto.response.PasswordVerifyResponse;
import com.ssafy.naeda.domain.user.dto.response.PinUpdateResponse;
import com.ssafy.naeda.domain.user.dto.response.PinVerifyResponse;
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
public class PinService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public PasswordVerifyResponse verifyAccountPassword(String userId, VerifyPasswordRequest request) {
        User user = getUser(userId);
        validateAccountPassword(user, request.getPassword());

        return PasswordVerifyResponse.builder()
                .verified(true)
                .message("비밀번호 확인이 완료되었습니다.")
                .build();
    }

    public PinVerifyResponse verifyCurrentPin(String userId, VerifyPinRequest request) {
        User user = getUser(userId);
        validateCurrentPin(user, request.getPin());

        return PinVerifyResponse.builder()
                .verified(true)
                .message("현재 PIN 확인이 완료되었습니다.")
                .build();
    }

    @Transactional
    public PinUpdateResponse updatePin(String userId, UpdatePinRequest request) {
        User user = getUser(userId);

        String currentHashedPin = user.getPinPassword();
        boolean hasExistingPin = currentHashedPin != null && !currentHashedPin.isBlank();

        if (hasExistingPin) {
            validateCurrentPin(user, request.getCurrentPin());
            if (request.getCurrentPin().equals(request.getNewPin())) {
                throw new BadRequestException("새 PIN은 현재 PIN과 달라야 합니다.");
            }
        }

        user.updatePinPassword(passwordEncoder.encode(request.getNewPin()));

        return PinUpdateResponse.builder()
                .pinSet(true)
                .message(hasExistingPin ? "PIN 변경이 완료되었습니다." : "PIN 설정이 완료되었습니다.")
                .build();
    }

    @Transactional
    public PinUpdateResponse resetPinWithPassword(String userId, ResetPinWithPasswordRequest request) {
        User user = getUser(userId);
        validateAccountPassword(user, request.getPassword());

        String currentHashedPin = user.getPinPassword();
        boolean hasExistingPin = currentHashedPin != null && !currentHashedPin.isBlank();
        if (hasExistingPin && passwordEncoder.matches(request.getNewPin(), currentHashedPin)) {
            throw new BadRequestException("새 PIN은 현재 PIN과 달라야 합니다.");
        }

        user.updatePinPassword(passwordEncoder.encode(request.getNewPin()));

        return PinUpdateResponse.builder()
                .pinSet(true)
                .message("PIN 재설정이 완료되었습니다.")
                .build();
    }

    private User getUser(String userId) {
        return userRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));
    }

    private void validateCurrentPin(User user, String currentPin) {
        String currentHashedPin = user.getPinPassword();
        boolean hasExistingPin = currentHashedPin != null && !currentHashedPin.isBlank();

        if (!hasExistingPin) {
            throw new BadRequestException("등록된 PIN이 없습니다.");
        }
        if (currentPin == null || currentPin.isBlank()) {
            throw new BadRequestException("현재 PIN을 입력해주세요.");
        }
        if (!passwordEncoder.matches(currentPin, currentHashedPin)) {
            throw new AuthenticationFailedException("현재 PIN이 일치하지 않습니다.");
        }
    }

    private void validateAccountPassword(User user, String password) {
        if (password == null || password.isBlank()) {
            throw new BadRequestException("비밀번호를 입력해주세요.");
        }
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new AuthenticationFailedException("비밀번호가 일치하지 않습니다.");
        }
    }
}
