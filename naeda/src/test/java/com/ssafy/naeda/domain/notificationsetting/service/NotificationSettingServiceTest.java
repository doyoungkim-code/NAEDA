package com.ssafy.naeda.domain.notificationsetting.service;

import com.ssafy.naeda.domain.notificationsetting.dto.request.NotificationSettingRequest;
import com.ssafy.naeda.domain.notificationsetting.dto.response.NotificationSettingResponse;
import com.ssafy.naeda.domain.notificationsetting.entity.NotificationSetting;
import com.ssafy.naeda.domain.notificationsetting.repository.NotificationSettingRepository;
import com.ssafy.naeda.global.exception.DuplicateException;
import com.ssafy.naeda.global.exception.NotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class NotificationSettingServiceTest {

    @InjectMocks
    private NotificationSettingService notificationSettingService;

    @Mock
    private NotificationSettingRepository notificationSettingRepository;

    private static final Long USER_NO = 1L;

    private NotificationSetting defaultSetting() {
        return NotificationSetting.builder()
                .settingId(1L)
                .userNo(USER_NO)
                .build();
    }

    // ── getSetting ────────────────────────────────────────────────────────

    @Test
    @DisplayName("알림 설정 조회")
    void getSetting() {
        given(notificationSettingRepository.findByUserNo(USER_NO))
                .willReturn(Optional.of(defaultSetting()));

        NotificationSettingResponse result = notificationSettingService.getSetting(USER_NO);

        assertThat(result.getSettingId()).isEqualTo(1L);
        assertThat(result.getPaymentEnabled()).isTrue();
        assertThat(result.getFdsEnabled()).isTrue();
        assertThat(result.getFestivalEnabled()).isTrue();
        assertThat(result.getPointEnabled()).isTrue();
        assertThat(result.getSystemEnabled()).isTrue();
    }

    @Test
    @DisplayName("알림 설정 조회 - 존재하지 않는 경우")
    void getSetting_notFound() {
        given(notificationSettingRepository.findByUserNo(USER_NO))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> notificationSettingService.getSetting(USER_NO))
                .isInstanceOf(NotFoundException.class);
    }

    // ── createSetting ─────────────────────────────────────────────────────

    @Test
    @DisplayName("알림 설정 생성 - 전체 기본값 TRUE")
    void createSetting() {
        given(notificationSettingRepository.findByUserNo(USER_NO))
                .willReturn(Optional.empty());
        given(notificationSettingRepository.save(any(NotificationSetting.class)))
                .willReturn(defaultSetting());

        NotificationSettingResponse result = notificationSettingService.createSetting(USER_NO);

        assertThat(result.getSettingId()).isEqualTo(1L);
        assertThat(result.getPaymentEnabled()).isTrue();
    }

    @Test
    @DisplayName("알림 설정 생성 - 이미 존재하면 예외")
    void createSetting_duplicate() {
        given(notificationSettingRepository.findByUserNo(USER_NO))
                .willReturn(Optional.of(defaultSetting()));

        assertThatThrownBy(() -> notificationSettingService.createSetting(USER_NO))
                .isInstanceOf(DuplicateException.class);
    }

    // ── updateSetting ─────────────────────────────────────────────────────

    @Test
    @DisplayName("알림 설정 수정")
    void updateSetting() {
        NotificationSetting setting = defaultSetting();
        given(notificationSettingRepository.findByUserNo(USER_NO))
                .willReturn(Optional.of(setting));

        NotificationSettingRequest request = NotificationSettingRequest.builder()
                .paymentEnabled(false)
                .fdsEnabled(true)
                .festivalEnabled(false)
                .pointEnabled(true)
                .systemEnabled(false)
                .build();

        NotificationSettingResponse result = notificationSettingService.updateSetting(USER_NO, request);

        assertThat(result.getPaymentEnabled()).isFalse();
        assertThat(result.getFdsEnabled()).isTrue();
        assertThat(result.getFestivalEnabled()).isFalse();
        assertThat(result.getPointEnabled()).isTrue();
        assertThat(result.getSystemEnabled()).isFalse();
    }

    @Test
    @DisplayName("알림 설정 수정 - 존재하지 않는 경우")
    void updateSetting_notFound() {
        given(notificationSettingRepository.findByUserNo(USER_NO))
                .willReturn(Optional.empty());

        NotificationSettingRequest request = NotificationSettingRequest.builder()
                .paymentEnabled(true)
                .fdsEnabled(true)
                .festivalEnabled(true)
                .pointEnabled(true)
                .systemEnabled(true)
                .build();

        assertThatThrownBy(() -> notificationSettingService.updateSetting(USER_NO, request))
                .isInstanceOf(NotFoundException.class);
    }
}
