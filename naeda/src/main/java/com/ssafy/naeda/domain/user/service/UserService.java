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
        user.updateFcmToken(fcmToken);
    }
}
