package com.ssafy.naeda.domain.notificationsetting.service;

import com.ssafy.naeda.domain.notificationsetting.dto.request.NotificationSettingRequest;
import com.ssafy.naeda.domain.notificationsetting.dto.response.NotificationSettingResponse;
import com.ssafy.naeda.domain.notificationsetting.entity.NotificationSetting;
import com.ssafy.naeda.domain.notificationsetting.repository.NotificationSettingRepository;
import com.ssafy.naeda.global.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationSettingService {

    private final NotificationSettingRepository notificationSettingRepository;

    public NotificationSettingResponse getSetting(Long userNo) {
        NotificationSetting setting = findByUserNo(userNo);
        return NotificationSettingResponse.from(setting);
    }

    @Transactional
    public NotificationSettingResponse createSetting(Long userNo) {
        notificationSettingRepository.findByUserNo(userNo).ifPresent(s -> {
            throw new com.ssafy.naeda.global.exception.DuplicateException(
                    "알림 설정이 이미 존재합니다. userNo: " + userNo);
        });

        NotificationSetting setting = notificationSettingRepository.save(
                NotificationSetting.builder()
                        .userNo(userNo)
                        .build()
        );

        return NotificationSettingResponse.from(setting);
    }

    @Transactional
    public NotificationSettingResponse updateSetting(Long userNo, NotificationSettingRequest request) {
        NotificationSetting setting = findByUserNo(userNo);

        setting.update(
                request.getPaymentEnabled(),
                request.getFdsEnabled(),
                request.getFestivalEnabled(),
                request.getPointEnabled(),
                request.getSystemEnabled()
        );

        return NotificationSettingResponse.from(setting);
    }

    private NotificationSetting findByUserNo(Long userNo) {
        return notificationSettingRepository.findByUserNo(userNo)
                .orElseThrow(() -> new NotFoundException("알림 설정을 찾을 수 없습니다. userNo: " + userNo));
    }
}
