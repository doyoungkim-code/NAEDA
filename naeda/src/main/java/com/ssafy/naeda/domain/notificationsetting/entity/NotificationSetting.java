package com.ssafy.naeda.domain.notificationsetting.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "notification_setting")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@Builder
public class NotificationSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "setting_id")
    private Long settingId;

    @Column(name = "user_no", nullable = false, unique = true)
    private Long userNo;

    @Column(name = "payment_enabled", nullable = false)
    @Builder.Default
    private Boolean paymentEnabled = true;

    @Column(name = "fds_enabled", nullable = false)
    @Builder.Default
    private Boolean fdsEnabled = true;

    @Column(name = "festival_enabled", nullable = false)
    @Builder.Default
    private Boolean festivalEnabled = true;

    @Column(name = "point_enabled", nullable = false)
    @Builder.Default
    private Boolean pointEnabled = true;

    @Column(name = "system_enabled", nullable = false)
    @Builder.Default
    private Boolean systemEnabled = true;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime created;

    public void update(Boolean paymentEnabled, Boolean fdsEnabled,
                       Boolean festivalEnabled, Boolean pointEnabled,
                       Boolean systemEnabled) {
        this.paymentEnabled = paymentEnabled;
        this.fdsEnabled = fdsEnabled;
        this.festivalEnabled = festivalEnabled;
        this.pointEnabled = pointEnabled;
        this.systemEnabled = systemEnabled;
    }
}