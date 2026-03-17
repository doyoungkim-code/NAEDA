package com.ssafy.naeda.domain.notification.controller;

import com.ssafy.naeda.domain.notification.dto.response.NotificationResponse;
import com.ssafy.naeda.domain.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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

    @Operation(summary = "안 읽은 알림 수 조회")
    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(@RequestParam Long userNo) {
        long count = notificationService.getUnreadCount(userNo);
        return ResponseEntity.ok(Map.of("unreadCount", count));
    }
}
