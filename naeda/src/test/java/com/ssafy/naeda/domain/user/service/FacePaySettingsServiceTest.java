package com.ssafy.naeda.domain.user.service;

import com.ssafy.naeda.domain.user.dto.request.UpdateFacePaySettingsRequest;
import com.ssafy.naeda.domain.user.dto.response.FacePaySettingsResponse;
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
class FacePaySettingsServiceTest {

    @InjectMocks
    private FacePaySettingsService facePaySettingsService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("PIN 2차 인증을 사용하지 않으면 얼굴 등록만 완료 상태로 저장한다")
    void updateSettings_registersFaceWithoutSecondaryAuth() {
        User user = baseUser().build();
        given(userRepository.findByUserId("user-1")).willReturn(Optional.of(user));

        FacePaySettingsResponse response = facePaySettingsService.updateSettings(
                "user-1",
                request(false, null)
        );

        assertThat(response.isFaceRegistered()).isTrue();
        assertThat(response.isSecondaryAuthEnabled()).isFalse();
        assertThat(user.getFaceRegistered()).isTrue();
        assertThat(user.getSecondaryAuthEnabled()).isFalse();
    }

    @Test
    @DisplayName("현재 PIN이 일치하면 PIN 2차 인증 사용을 저장한다")
    void updateSettings_enablesSecondaryAuthWhenCurrentPinMatches() {
        User user = baseUser()
                .pinPassword("encoded-pin")
                .build();
        given(userRepository.findByUserId("user-1")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("123456", "encoded-pin")).willReturn(true);

        FacePaySettingsResponse response = facePaySettingsService.updateSettings(
                "user-1",
                request(true, "123456")
        );

        assertThat(response.isFaceRegistered()).isTrue();
        assertThat(response.isSecondaryAuthEnabled()).isTrue();
        assertThat(user.getFaceRegistered()).isTrue();
        assertThat(user.getSecondaryAuthEnabled()).isTrue();
    }

    @Test
    @DisplayName("PIN 2차 인증을 켜려면 현재 PIN 입력이 필요하다")
    void updateSettings_requiresCurrentPinWhenSecondaryAuthEnabled() {
        User user = baseUser()
                .pinPassword("encoded-pin")
                .build();
        given(userRepository.findByUserId("user-1")).willReturn(Optional.of(user));

        assertThatThrownBy(() -> facePaySettingsService.updateSettings("user-1", request(true, null)))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("현재 PIN을 입력해주세요.");
    }

    @Test
    @DisplayName("현재 PIN이 다르면 PIN 2차 인증 사용을 저장할 수 없다")
    void updateSettings_rejectsInvalidCurrentPin() {
        User user = baseUser()
                .pinPassword("encoded-pin")
                .build();
        given(userRepository.findByUserId("user-1")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("654321", "encoded-pin")).willReturn(false);

        assertThatThrownBy(() -> facePaySettingsService.updateSettings("user-1", request(true, "654321")))
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessage("현재 PIN이 일치하지 않습니다.");
    }

    private static UpdateFacePaySettingsRequest request(boolean enableSecondaryAuth, String currentPin) {
        return UpdateFacePaySettingsRequest.builder()
                .enableSecondaryAuth(enableSecondaryAuth)
                .currentPin(currentPin)
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
