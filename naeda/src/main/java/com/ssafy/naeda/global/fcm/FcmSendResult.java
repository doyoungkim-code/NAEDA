package com.ssafy.naeda.global.fcm;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class FcmSendResult {
    private int totalUsers;
    private int targetUsers;
    private int successCount;
    private int failCount;
}
