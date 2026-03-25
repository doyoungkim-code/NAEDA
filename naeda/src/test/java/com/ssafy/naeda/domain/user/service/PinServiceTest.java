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
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class PinServiceTest {

    @InjectMocks
    private PinService pinService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("로그인 비밀번호가 맞으면 검증을 통과한다")
    void verifyAccountPassword_succeeds() {
        User user = baseUser()
                .password("encoded-password")
                .pinPassword("encoded-old-pin")
                .build();
        given(userRepository.findByUserId("user-1")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("password1234!", "encoded-password")).willReturn(true);

        PasswordVerifyResponse response = pinService.verifyAccountPassword("user-1", passwordRequest("password1234!"));

        assertThat(response.isVerified()).isTrue();
        assertThat(response.getMessage()).isEqualTo("비밀번호 확인이 완료되었습니다.");
    }

    @Test
    @DisplayName("로그인 비밀번호가 다르면 검증에 실패한다")
    void verifyAccountPassword_rejectsInvalidPassword() {
        User user = baseUser()
                .password("encoded-password")
                .pinPassword("encoded-old-pin")
                .build();
        given(userRepository.findByUserId("user-1")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("wrong-password", "encoded-password")).willReturn(false);

        assertThatThrownBy(() -> pinService.verifyAccountPassword("user-1", passwordRequest("wrong-password")))
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessage("비밀번호가 일치하지 않습니다.");
    }

    @Test
    @DisplayName("현재 PIN이 맞으면 검증을 통과한다")
    void verifyCurrentPin_succeeds() {
        User user = baseUser()
                .pinPassword("encoded-old-pin")
                .build();
        given(userRepository.findByUserId("user-1")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("123456", "encoded-old-pin")).willReturn(true);

        PinVerifyResponse response = pinService.verifyCurrentPin("user-1", verifyRequest("123456"));

        assertThat(response.isVerified()).isTrue();
        assertThat(response.getMessage()).isEqualTo("현재 PIN 확인이 완료되었습니다.");
    }

    @Test
    @DisplayName("현재 PIN이 다르면 검증에 실패한다")
    void verifyCurrentPin_rejectsInvalidPin() {
        User user = baseUser()
                .pinPassword("encoded-old-pin")
                .build();
        given(userRepository.findByUserId("user-1")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("123456", "encoded-old-pin")).willReturn(false);

        assertThatThrownBy(() -> pinService.verifyCurrentPin("user-1", verifyRequest("123456")))
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessage("현재 PIN이 일치하지 않습니다.");
    }

    @Test
    @DisplayName("기존 PIN이 없으면 새 PIN을 설정한다")
    void updatePin_setsInitialPin() {
        User user = baseUser().build();
        given(userRepository.findByUserId("user-1")).willReturn(Optional.of(user));
        given(passwordEncoder.encode("654321")).willReturn("encoded-new-pin");

        PinUpdateResponse response = pinService.updatePin("user-1", request(null, "654321"));

        assertThat(user.getPinPassword()).isEqualTo("encoded-new-pin");
        assertThat(response.isPinSet()).isTrue();
        assertThat(response.getMessage()).isEqualTo("PIN 설정이 완료되었습니다.");
    }

    @Test
    @DisplayName("기존 PIN이 있으면 현재 PIN 검증 후 변경한다")
    void updatePin_changesExistingPin() {
        User user = baseUser()
                .pinPassword("encoded-old-pin")
                .build();
        given(userRepository.findByUserId("user-1")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("123456", "encoded-old-pin")).willReturn(true);
        given(passwordEncoder.encode("654321")).willReturn("encoded-new-pin");

        PinUpdateResponse response = pinService.updatePin("user-1", request("123456", "654321"));

        assertThat(user.getPinPassword()).isEqualTo("encoded-new-pin");
        assertThat(response.getMessage()).isEqualTo("PIN 변경이 완료되었습니다.");
    }

    @Test
    @DisplayName("기존 PIN이 있는데 현재 PIN이 없으면 변경할 수 없다")
    void updatePin_requiresCurrentPinWhenExistingPinPresent() {
        User user = baseUser()
                .pinPassword("encoded-old-pin")
                .build();
        given(userRepository.findByUserId("user-1")).willReturn(Optional.of(user));

        assertThatThrownBy(() -> pinService.updatePin("user-1", request(null, "654321")))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("현재 PIN을 입력해주세요.");
    }

    @Test
    @DisplayName("현재 PIN이 다르면 변경할 수 없다")
    void updatePin_rejectsInvalidCurrentPin() {
        User user = baseUser()
                .pinPassword("encoded-old-pin")
                .build();
        given(userRepository.findByUserId("user-1")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("123456", "encoded-old-pin")).willReturn(false);

        assertThatThrownBy(() -> pinService.updatePin("user-1", request("123456", "654321")))
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessage("현재 PIN이 일치하지 않습니다.");
    }

    @Test
    @DisplayName("비밀번호가 맞으면 기존 PIN과 무관하게 새 PIN으로 재설정한다")
    void resetPinWithPassword_resetsPin() {
        User user = baseUser()
                .password("encoded-password")
                .pinPassword("encoded-old-pin")
                .build();
        given(userRepository.findByUserId("user-1")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("password1234!", "encoded-password")).willReturn(true);
        given(passwordEncoder.matches("654321", "encoded-old-pin")).willReturn(false);
        given(passwordEncoder.encode("654321")).willReturn("encoded-new-pin");

        PinUpdateResponse response = pinService.resetPinWithPassword("user-1", resetRequest("password1234!", "654321"));

        assertThat(user.getPinPassword()).isEqualTo("encoded-new-pin");
        assertThat(response.getMessage()).isEqualTo("PIN 재설정이 완료되었습니다.");
    }

    @Test
    @DisplayName("비밀번호가 다르면 PIN을 재설정할 수 없다")
    void resetPinWithPassword_rejectsInvalidPassword() {
        User user = baseUser()
                .password("encoded-password")
                .pinPassword("encoded-old-pin")
                .build();
        given(userRepository.findByUserId("user-1")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("wrong-password", "encoded-password")).willReturn(false);

        assertThatThrownBy(() -> pinService.resetPinWithPassword("user-1", resetRequest("wrong-password", "654321")))
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessage("비밀번호가 일치하지 않습니다.");
    }

    private static UpdatePinRequest request(String currentPin, String newPin) {
        return UpdatePinRequest.builder()
                .currentPin(currentPin)
                .newPin(newPin)
                .build();
    }

    private static ResetPinWithPasswordRequest resetRequest(String password, String newPin) {
        return ResetPinWithPasswordRequest.builder()
                .password(password)
                .newPin(newPin)
                .build();
    }

    private static VerifyPasswordRequest passwordRequest(String password) {
        return VerifyPasswordRequest.builder()
                .password(password)
                .build();
    }

    private static VerifyPinRequest verifyRequest(String pin) {
        return VerifyPinRequest.builder()
                .pin(pin)
                .build();
    }

    private static User.UserBuilder baseUser() {
        return User.builder()
                .userId("user-1")
                .password("encoded-password")
                .username("홍길동")
                .residentNo("9001011")
                .phone("01012345678")
                .institutionCode("001");
    }
}
