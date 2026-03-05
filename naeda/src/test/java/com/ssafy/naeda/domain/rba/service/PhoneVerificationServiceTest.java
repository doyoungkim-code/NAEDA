package com.ssafy.naeda.domain.rba.service;

import com.ssafy.naeda.domain.user.entity.User;
import com.ssafy.naeda.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class PhoneVerificationServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private PhoneVerificationService service;

    // === extractMiddleDigits ===

    @Test
    @DisplayName("하이픈 있는 번호에서 가운데 4자리 추출")
    void extractMiddleDigits_withHyphen() {
        assertThat(service.extractMiddleDigits("010-1234-5678")).isEqualTo("1234");
    }

    @Test
    @DisplayName("하이픈 없는 번호에서 가운데 4자리 추출")
    void extractMiddleDigits_withoutHyphen() {
        assertThat(service.extractMiddleDigits("01012345678")).isEqualTo("1234");
    }

    @Test
    @DisplayName("유효하지 않은 전화번호 → 예외")
    void extractMiddleDigits_invalidPhone() {
        assertThatThrownBy(() -> service.extractMiddleDigits("0101234"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // === verify ===

    @Test
    @DisplayName("정확한 4자리 입력 → true")
    void verify_correct() {
        User user = User.builder()
                .phone("010-1234-5678")
                .userId("test@test.com")
                .password("pw")
                .username("테스트")
                .institutionCode("001")
                .build();

        given(userRepository.findByUserNo(1L)).willReturn(Optional.of(user));

        assertThat(service.verify(1L, "1234")).isTrue();
    }

    @Test
    @DisplayName("틀린 4자리 입력 → false")
    void verify_wrong() {
        User user = User.builder()
                .phone("010-1234-5678")
                .userId("test@test.com")
                .password("pw")
                .username("테스트")
                .institutionCode("001")
                .build();

        given(userRepository.findByUserNo(1L)).willReturn(Optional.of(user));

        assertThat(service.verify(1L, "0000")).isFalse();
    }

    @Test
    @DisplayName("존재하지 않는 유저 → 예외")
    void verify_userNotFound() {
        given(userRepository.findByUserNo(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.verify(999L, "1234"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}