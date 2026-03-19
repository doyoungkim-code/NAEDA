package com.ssafy.naeda.domain.notificationsetting.controller;

import com.ssafy.naeda.domain.notificationsetting.dto.request.NotificationSettingRequest;
import com.ssafy.naeda.domain.notificationsetting.dto.response.NotificationSettingResponse;
import com.ssafy.naeda.domain.notificationsetting.service.NotificationSettingService;
import com.ssafy.naeda.global.exception.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notification-settings")
@RequiredArgsConstructor
@Validated
@Tag(name = "알림 설정", description = "알림 수신 설정 조회/생성/수정 API")
public class NotificationSettingController {

    private final NotificationSettingService notificationSettingService;

    @GetMapping("/{userNo}")
    @Operation(summary = "알림 설정 조회", description = "사용자의 알림 수신 설정을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "설정을 찾을 수 없음", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<NotificationSettingResponse> getSetting(@PathVariable @Positive Long userNo) {
        return ResponseEntity.ok(notificationSettingService.getSetting(userNo));
    }

    @PostMapping("/{userNo}")
    @Operation(summary = "알림 설정 생성", description = "사용자의 알림 수신 설정을 생성합니다. (전체 기본값 TRUE)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "생성 성공"),
            @ApiResponse(responseCode = "400", description = "이미 설정이 존재함", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<NotificationSettingResponse> createSetting(@PathVariable @Positive Long userNo) {
        return ResponseEntity.status(HttpStatus.CREATED).body(notificationSettingService.createSetting(userNo));
    }

    @PutMapping("/{userNo}")
    @Operation(summary = "알림 설정 수정", description = "사용자의 알림 수신 설정을 수정합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수정 성공"),
            @ApiResponse(responseCode = "404", description = "설정을 찾을 수 없음", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<NotificationSettingResponse> updateSetting(
            @PathVariable @Positive Long userNo,
            @Valid @RequestBody NotificationSettingRequest request) {
        return ResponseEntity.ok(notificationSettingService.updateSetting(userNo, request));
    }
}
