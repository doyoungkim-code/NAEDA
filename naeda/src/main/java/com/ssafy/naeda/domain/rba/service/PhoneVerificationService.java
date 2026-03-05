package com.ssafy.naeda.domain.rba.service;

import com.ssafy.naeda.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PhoneVerificationService {

    private final UserRepository userRepository;

    /**
     * 전화번호에서 가운데 4자리 추출
     * 예: "010-1234-5678" → "1234"
     */
    public String extractMiddleDigits(String phone) {
        String digits = phone.replaceAll("[^0-9]", "");
        if(digits.length() != 11) {
            throw new IllegalArgumentException("유효하지 않은 전화번호: " + phone);
        }
        return digits.substring(3, 7);
    }

    /**
     * userNo로 유저를 조회하고, 입력한 4자리와 비교
     */
    public boolean verify(Long userNo, String inputDigits) {
        String phone = userRepository.findByUserNo(userNo)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저: " + userNo))
                .getPhone();

        return extractMiddleDigits(phone).equals(inputDigits);
    }
}
