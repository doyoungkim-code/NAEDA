package com.ssafy.naeda.domain.notificationsetting.repository;

import com.ssafy.naeda.domain.notificationsetting.entity.NotificationSetting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NotificationSettingRepository extends JpaRepository<NotificationSetting, Long> {

    Optional<NotificationSetting> findByUserNo(Long userNo);

    void deleteByUserNo(Long userNo);
}