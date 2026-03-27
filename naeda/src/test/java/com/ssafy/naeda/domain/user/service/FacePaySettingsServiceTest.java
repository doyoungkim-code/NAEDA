package com.ssafy.naeda.domain.user.service;

import com.ssafy.naeda.domain.face.service.FaceRegistrationSessionService;
import com.ssafy.naeda.domain.identity.service.ResidentIdVerificationSessionService;
import com.ssafy.naeda.domain.pay.service.PayLimitService;
import com.ssafy.naeda.domain.pay.service.PayMethodService;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FacePaySettingsServiceTest {

    @InjectMocks
    private FacePaySettingsService facePaySettingsService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private FaceRegistrationSessionService faceRegistrationSessionService;

    @Mock
    private ResidentIdVerificationSessionService residentIdVerificationSessionService;

    @Mock
    private PayMethodService payMethodService;

    @Mock
    private PayLimitService payLimitService;

    @Test
    @DisplayName("PIN 2차 인증을 사용하지 않으면 얼굴 등록과 대표 결제수단, 한도를 함께 저장한다")
    void updateSettings_registersFaceWithoutSecondaryAuth() {
        User user = baseUser().userNo(1L).build();
        given(userRepository.findByUserId("user-1")).willReturn(Optional.of(user));
        given(faceRegistrationSessionService.hasActiveSession("user-1")).willReturn(true);
        given(residentIdVerificationSessionService.isConfirmed("user-1")).willReturn(true);
        given(faceRegistrationSessionService.isRegistrationReady("user-1")).willReturn(true);

        FacePaySettingsResponse response = facePaySettingsService.updateSettings(
                "user-1",
                request(false, null, 7L, 300_000L, 3_000_000L, 100_000L)
        );

        assertThat(response.isFaceRegistered()).isTrue();
        assertThat(response.isSecondaryAuthEnabled()).isFalse();
        assertThat(user.getFaceRegistered()).isTrue();
        assertThat(user.getSecondaryAuthEnabled()).isFalse();
        verify(faceRegistrationSessionService).persistPendingEmbeddings("user-1");
        verify(payMethodService).setFacePay(1L, 7L);
        verify(payLimitService).setLimit(eq(1L), any());
        verify(faceRegistrationSessionService).clearSession("user-1");
        verify(residentIdVerificationSessionService).clearSession("user-1");
    }

    @Test
    @DisplayName("현재 PIN이 일치하면 PIN 2차 인증 사용을 저장한다")
    void updateSettings_enablesSecondaryAuthWhenCurrentPinMatches() {
        User user = baseUser()
                .userNo(1L)
                .pinPassword("encoded-pin")
                .build();
        given(userRepository.findByUserId("user-1")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("123456", "encoded-pin")).willReturn(true);
        given(faceRegistrationSessionService.hasActiveSession("user-1")).willReturn(true);
        given(residentIdVerificationSessionService.isConfirmed("user-1")).willReturn(true);
        given(faceRegistrationSessionService.isRegistrationReady("user-1")).willReturn(true);

        FacePaySettingsResponse response = facePaySettingsService.updateSettings(
                "user-1",
                request(true, "123456", 7L, 300_000L, 3_000_000L, 100_000L)
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
                .userNo(1L)
                .pinPassword("encoded-pin")
                .build();
        given(userRepository.findByUserId("user-1")).willReturn(Optional.of(user));

        assertThatThrownBy(() -> facePaySettingsService.updateSettings("user-1", request(true, null, 7L, 300_000L, 3_000_000L, 100_000L)))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("현재 PIN을 입력해주세요.");
    }

    @Test
    @DisplayName("현재 PIN이 다르면 PIN 2차 인증 사용을 저장할 수 없다")
    void updateSettings_rejectsInvalidCurrentPin() {
        User user = baseUser()
                .userNo(1L)
                .pinPassword("encoded-pin")
                .build();
        given(userRepository.findByUserId("user-1")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("654321", "encoded-pin")).willReturn(false);

        assertThatThrownBy(() -> facePaySettingsService.updateSettings("user-1", request(true, "654321", 7L, 300_000L, 3_000_000L, 100_000L)))
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessage("현재 PIN이 일치하지 않습니다.");
    }

    @Test
    @DisplayName("등록 세션이 있을 때 대표 결제수단이나 한도가 없으면 저장할 수 없다")
    void updateSettings_requiresPaymentMethodAndLimitWhenRegistrationPending() {
        User user = baseUser().userNo(1L).build();
        given(userRepository.findByUserId("user-1")).willReturn(Optional.of(user));
        given(faceRegistrationSessionService.hasActiveSession("user-1")).willReturn(true);

        assertThatThrownBy(() -> facePaySettingsService.updateSettings("user-1", request(false, null, null, null, null, null)))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("대표 결제수단을 선택해주세요.");
    }

    @Test
    @DisplayName("얼굴 등록 세션이 있어도 신분증 확인이 끝나지 않으면 저장할 수 없다")
    void updateSettings_requiresResidentIdConfirmation() {
        User user = baseUser().userNo(1L).build();
        given(userRepository.findByUserId("user-1")).willReturn(Optional.of(user));
        given(faceRegistrationSessionService.hasActiveSession("user-1")).willReturn(true);
        given(residentIdVerificationSessionService.isConfirmed("user-1")).willReturn(false);

        assertThatThrownBy(() -> facePaySettingsService.updateSettings(
                "user-1",
                request(false, null, 7L, 300_000L, 3_000_000L, 100_000L)
        ))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("신분증 확인을 먼저 완료해주세요.");
    }

    private static UpdateFacePaySettingsRequest request(
            boolean enableSecondaryAuth,
            String currentPin,
            Long paymentMethodId,
            Long dailyLimit,
            Long monthlyLimit,
            Long singleTransactionLimit
    ) {
        return UpdateFacePaySettingsRequest.builder()
                .enableSecondaryAuth(enableSecondaryAuth)
                .currentPin(currentPin)
                .paymentMethodId(paymentMethodId)
                .dailyLimit(dailyLimit)
                .monthlyLimit(monthlyLimit)
                .singleTransactionLimit(singleTransactionLimit)
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
