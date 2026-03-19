package com.ssafy.naeda.domain.notificationsetting.dto.response;

import com.ssafy.naeda.domain.notificationsetting.entity.NotificationSetting;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class NotificationSettingResponse {

    private Long settingId;
    private Boolean paymentEnabled;
    private Boolean fdsEnabled;
    private Boolean festivalEnabled;
    private Boolean pointEnabled;
    private Boolean systemEnabled;

    public static NotificationSettingResponse from(NotificationSetting setting) {
        return NotificationSettingResponse.builder()
                .settingId(setting.getSettingId())
                .paymentEnabled(setting.getPaymentEnabled())
                .fdsEnabled(setting.getFdsEnabled())
                .festivalEnabled(setting.getFestivalEnabled())
                .pointEnabled(setting.getPointEnabled())
                .systemEnabled(setting.getSystemEnabled())
                .build();
    }
}
