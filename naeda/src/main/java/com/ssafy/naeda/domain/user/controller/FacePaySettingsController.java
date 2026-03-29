package com.ssafy.naeda.domain.user.controller;

import com.ssafy.naeda.domain.user.dto.request.UpdateFacePaySettingsRequest;
import com.ssafy.naeda.domain.user.dto.response.FacePaySettingsResponse;
import com.ssafy.naeda.domain.user.service.FacePaySettingsService;
import com.ssafy.naeda.global.exception.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/me/face-pay-settings")
@RequiredArgsConstructor
@Tag(name = "FacePay Settings", description = "페이스페이 등록 상태 및 2차 인증 설정 API")
public class FacePaySettingsController {

    private final FacePaySettingsService facePaySettingsService;

    @GetMapping
    @Operation(summary = "페이스페이 설정 조회", description = "로그인 사용자의 페이스페이 등록 여부와 PIN 2차 인증 사용 여부를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "사용자 없음", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<FacePaySettingsResponse> getSettings(Principal principal) {
        return ResponseEntity.ok(facePaySettingsService.getSettings(principal.getName()));
    }

    @PutMapping
    @Operation(summary = "페이스페이 설정 저장", description = "페이스페이 등록 완료 상태와 PIN 2차 인증 사용 여부를 저장합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "저장 성공"),
            @ApiResponse(responseCode = "400", description = "입력값 오류", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "현재 PIN 불일치", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "사용자 없음", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<FacePaySettingsResponse> updateSettings(
            @Valid @RequestBody UpdateFacePaySettingsRequest request,
            Principal principal
    ) {
        return ResponseEntity.ok(facePaySettingsService.updateSettings(principal.getName(), request));
    }
}
