package com.ssafy.naeda.domain.user.service;
import com.ssafy.naeda.domain.user.dto.response.UserResponse;
import com.ssafy.naeda.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {
    private final UserRepository userRepository;

    public UserResponse getMyInfo(Long userNo){
        return userRepository.findByUserNo(userNo)
                .map(UserResponse::from)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
    }
}
