package com.ssafy.naeda.domain.user.service;

import com.ssafy.naeda.domain.user.dto.response.UserResponse;
import com.ssafy.naeda.domain.user.entity.User;
import com.ssafy.naeda.domain.user.repository.UserRepository;
import com.ssafy.naeda.global.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {
    private final UserRepository userRepository;

    public UserResponse getMyInfo(Long userNo) {
        return userRepository.findByUserNo(userNo)
                .map(UserResponse::from)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));
    }

    @Transactional
    public void updateFcmToken(Long userNo, String fcmToken) {
        User user = userRepository.findByUserNo(userNo)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));

        // 같은 FCM 토큰을 가진 다른 유저의 토큰을 null로 밀어서 중복 알림 방지
        // 하나의 기기(토큰)는 하나의 유저만 알림을 받아야 함
        if (fcmToken != null && !fcmToken.isBlank()) {
            userRepository.findByFcmToken(fcmToken).ifPresent(otherUser -> {
                if (!otherUser.getUserNo().equals(userNo)) {
                    otherUser.clearFcmToken();
                }
            });
        }

        user.updateFcmToken(fcmToken);
    }
}
