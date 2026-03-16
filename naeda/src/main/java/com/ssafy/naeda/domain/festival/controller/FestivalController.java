package com.ssafy.naeda.domain.festival.controller;

import com.ssafy.naeda.domain.festival.dto.request.FestivalCreateRequest;
import com.ssafy.naeda.domain.festival.dto.request.FestivalUpdateRequest;
import com.ssafy.naeda.domain.festival.dto.response.FestivalResponse;
import com.ssafy.naeda.domain.festival.entity.Festival;
import com.ssafy.naeda.domain.festival.service.FestivalService;
import com.ssafy.naeda.domain.notification.entity.NotificationType;
import com.ssafy.naeda.domain.notification.entity.ReferenceType;
import com.ssafy.naeda.global.fcm.FcmSendResult;
import com.ssafy.naeda.global.fcm.FcmService;
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
@RequestMapping("/api/festivals")
@RequiredArgsConstructor
@Tag(name = "Festival", description = "축제/이벤트 API")
public class FestivalController {

    private final FestivalService festivalService;
    private final FcmService fcmService;

    @Operation(summary = "축제 등록")
    @PostMapping
    public ResponseEntity<FestivalResponse> createFestival(@Valid @RequestBody FestivalCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(festivalService.createFestival(request));
    }

    @Operation(summary = "축제 목록 조회")
    @GetMapping
    public ResponseEntity<List<FestivalResponse>> getAllFestivals() {
        return ResponseEntity.ok(festivalService.getAllFestivals());
    }

    @Operation(summary = "축제 단건 조회")
    @GetMapping("/{festivalId}")
    public ResponseEntity<FestivalResponse> getFestival(@PathVariable Long festivalId) {
        return ResponseEntity.ok(festivalService.getFestival(festivalId));
    }

    @Operation(summary = "축제 수정")
    @PutMapping("/{festivalId}")
    public ResponseEntity<FestivalResponse> updateFestival(
            @PathVariable Long festivalId,
            @Valid @RequestBody FestivalUpdateRequest request
    ) {
        return ResponseEntity.ok(festivalService.updateFestival(festivalId, request));
    }

    @Operation(summary = "축제 삭제")
    @DeleteMapping("/{festivalId}")
    public ResponseEntity<Void> deleteFestival(@PathVariable Long festivalId) {
        festivalService.deleteFestival(festivalId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "축제 FCM 알림 발송", description = "특정 축제에 대해 전체 사용자에게 FCM 푸시 알림을 발송합니다.")
    @PostMapping("/{festivalId}/notify")
    public ResponseEntity<Map<String, Object>> notifyFestival(@PathVariable Long festivalId) {
        Festival festival = festivalService.markAsNotified(festivalId);

        String title = "구미 축제 안내";
        String body = String.format("[%s] %s ~ %s",
                festival.getTitle(),
                festival.getStartDate(),
                festival.getEndDate());

        FcmSendResult result = fcmService.sendToAllUsers(
                title, body,
                NotificationType.FESTIVAL,
                festival.getFestivalId(),
                ReferenceType.FESTIVAL
        );

        return ResponseEntity.ok(Map.of(
                "festivalId", festival.getFestivalId(),
                "totalUsers", result.getTotalUsers(),
                "targetUsers", result.getTargetUsers(),
                "successCount", result.getSuccessCount(),
                "failCount", result.getFailCount()
        ));
    }
}
