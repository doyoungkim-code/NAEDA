package com.ssafy.naeda.domain.notification.controller;

import com.ssafy.naeda.domain.notification.dto.response.NotificationResponse;
import com.ssafy.naeda.domain.notification.entity.NotificationType;
import com.ssafy.naeda.domain.notification.service.NotificationService;
import com.ssafy.naeda.global.fcm.FcmSendResult;
import com.ssafy.naeda.global.fcm.FcmService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "Notification", description = "알림 API")
public class NotificationController {

    private final NotificationService notificationService;
    private final FcmService fcmService;

    @Operation(summary = "공지사항 알림 발송", description = "전체 사용자에게 공지 알림을 발송합니다.")
    @PostMapping("/announce")
    public ResponseEntity<Map<String, Object>> announce(@Valid @RequestBody AnnounceRequest request) {
        FcmSendResult result = fcmService.sendToAllUsers(
                request.getTitle(),
                request.getBody(),
                NotificationType.SYSTEM,
                null,
                null
        );
        return ResponseEntity.ok(Map.of(
                "totalUsers", result.getTotalUsers(),
                "targetUsers", result.getTargetUsers(),
                "successCount", result.getSuccessCount(),
                "failCount", result.getFailCount()
        ));
    }

    @Operation(summary = "내 알림 목록 조회")
    @GetMapping
    public ResponseEntity<List<NotificationResponse>> getUserNotifications(@RequestParam Long userNo) {
        return ResponseEntity.ok(notificationService.getUserNotifications(userNo));
    }

    @Operation(summary = "알림 읽음 처리")
    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long notificationId) {
        notificationService.markAsRead(notificationId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "내 알림 전체 읽음 처리")
    @PatchMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(@RequestParam Long userNo) {
        notificationService.markAllAsRead(userNo);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "안 읽은 알림 수 조회")
    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(@RequestParam Long userNo) {
        long count = notificationService.getUnreadCount(userNo);
        return ResponseEntity.ok(Map.of("unreadCount", count));
    }

    @Getter
    @NoArgsConstructor
    static class AnnounceRequest {
        @NotBlank(message = "제목은 필수입니다.")
        private String title;
        @NotBlank(message = "내용은 필수입니다.")
        private String body;
    }
}
