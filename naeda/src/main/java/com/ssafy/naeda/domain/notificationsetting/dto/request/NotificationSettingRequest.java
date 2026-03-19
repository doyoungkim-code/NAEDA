package com.ssafy.naeda.domain.notificationsetting.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationSettingRequest {

    @NotNull
    private Boolean paymentEnabled;

    @NotNull
    private Boolean fdsEnabled;

    @NotNull
    private Boolean festivalEnabled;

    @NotNull
    private Boolean pointEnabled;

    @NotNull
    private Boolean systemEnabled;
}