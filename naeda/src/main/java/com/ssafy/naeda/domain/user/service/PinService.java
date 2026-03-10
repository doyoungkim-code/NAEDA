package com.ssafy.naeda.domain.user.service;

import com.ssafy.naeda.domain.user.dto.request.UpdatePinRequest;
import com.ssafy.naeda.domain.user.dto.response.PinUpdateResponse;
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

    @Transactional
    public PinUpdateResponse updatePin(String userId, UpdatePinRequest request) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));

        String currentHashedPin = user.getPinPassword();
        boolean hasExistingPin = currentHashedPin != null && !currentHashedPin.isBlank();

        if (hasExistingPin) {
            if (request.getCurrentPin() == null || request.getCurrentPin().isBlank()) {
                throw new BadRequestException("현재 PIN을 입력해주세요.");
            }
            if (!passwordEncoder.matches(request.getCurrentPin(), currentHashedPin)) {
                throw new AuthenticationFailedException("현재 PIN이 일치하지 않습니다.");
            }
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
}
