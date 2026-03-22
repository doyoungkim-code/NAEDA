package com.ssafy.naeda.domain.notice.controller;

import com.ssafy.naeda.domain.notice.dto.request.NoticeCreateRequest;
import com.ssafy.naeda.domain.notice.dto.request.NoticeUpdateRequest;
import com.ssafy.naeda.domain.notice.dto.response.NoticeResponse;
import com.ssafy.naeda.domain.notice.service.NoticeService;
import com.ssafy.naeda.global.fcm.FcmSendResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notices")
@RequiredArgsConstructor
@Tag(name = "Notice", description = "공지사항 API")
public class NoticeController {

    private final NoticeService noticeService;

    @Operation(summary = "공지사항 등록")
    @PostMapping
    public ResponseEntity<NoticeResponse> createNotice(@Valid @RequestBody NoticeCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(noticeService.createNotice(request));
    }

    @Operation(summary = "공지사항 목록 조회")
    @GetMapping
    public ResponseEntity<List<NoticeResponse>> getAllNotices() {
        return ResponseEntity.ok(noticeService.getAllNotices());
    }

    @Operation(summary = "공지사항 단건 조회")
    @GetMapping("/{noticeId}")
    public ResponseEntity<NoticeResponse> getNotice(@PathVariable Long noticeId) {
        return ResponseEntity.ok(noticeService.getNotice(noticeId));
    }

    @Operation(summary = "공지사항 수정")
    @PutMapping("/{noticeId}")
    public ResponseEntity<NoticeResponse> updateNotice(
            @PathVariable Long noticeId,
            @Valid @RequestBody NoticeUpdateRequest request
    ) {
        return ResponseEntity.ok(noticeService.updateNotice(noticeId, request));
    }

    @Operation(summary = "공지사항 삭제")
    @DeleteMapping("/{noticeId}")
    public ResponseEntity<Void> deleteNotice(@PathVariable Long noticeId) {
        noticeService.deleteNotice(noticeId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "공지사항 FCM 알림 발송", description = "해당 공지사항을 전체 사용자에게 FCM 푸시 알림으로 발송합니다.")
    @PostMapping("/{noticeId}/notify")
    public ResponseEntity<Map<String, Object>> notifyNotice(@PathVariable Long noticeId) {
        FcmSendResult result = noticeService.notifyNotice(noticeId);

        return ResponseEntity.ok(Map.of(
                "noticeId", noticeId,
                "totalUsers", result.getTotalUsers(),
                "targetUsers", result.getTargetUsers(),
                "successCount", result.getSuccessCount(),
                "failCount", result.getFailCount()
        ));
    }
}
