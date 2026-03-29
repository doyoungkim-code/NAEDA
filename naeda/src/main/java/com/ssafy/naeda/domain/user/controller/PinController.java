package com.ssafy.naeda.domain.user.controller;

import com.ssafy.naeda.domain.user.dto.request.ResetPinWithPasswordRequest;
import com.ssafy.naeda.domain.user.dto.request.UpdatePinRequest;
import com.ssafy.naeda.domain.user.dto.request.VerifyPasswordRequest;
import com.ssafy.naeda.domain.user.dto.request.VerifyPinRequest;
import com.ssafy.naeda.domain.user.dto.response.PasswordVerifyResponse;
import com.ssafy.naeda.domain.user.dto.response.PinUpdateResponse;
import com.ssafy.naeda.domain.user.dto.response.PinVerifyResponse;
import com.ssafy.naeda.domain.user.service.PinService;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/me")
@RequiredArgsConstructor
@Tag(name = "PIN", description = "페이스페이 PIN 설정/변경 API")
public class PinController {

    private final PinService pinService;

    @PostMapping("/pin/password-verify")
    @Operation(summary = "비밀번호 검증", description = "로그인 사용자의 비밀번호가 일치하는지 검증합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "비밀번호 검증 성공"),
            @ApiResponse(responseCode = "400", description = "입력값 오류", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "비밀번호 불일치", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "사용자 없음", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<PasswordVerifyResponse> verifyPassword(
            @Valid @RequestBody VerifyPasswordRequest request,
            Principal principal
    ) {
        return ResponseEntity.ok(pinService.verifyAccountPassword(principal.getName(), request));
    }

    @PostMapping("/pin/verify")
    @Operation(summary = "현재 PIN 검증", description = "로그인 사용자의 현재 PIN이 일치하는지 검증합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "현재 PIN 검증 성공"),
            @ApiResponse(responseCode = "400", description = "입력값 오류", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "현재 PIN 불일치", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "사용자 없음", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<PinVerifyResponse> verifyCurrentPin(
            @Valid @RequestBody VerifyPinRequest request,
            Principal principal
    ) {
        return ResponseEntity.ok(pinService.verifyCurrentPin(principal.getName(), request));
    }

    @PutMapping("/pin")
    @Operation(summary = "페이스페이 PIN 설정/변경", description = "로그인 사용자의 6자리 PIN을 설정하거나 변경합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "PIN 설정 또는 변경 성공"),
            @ApiResponse(responseCode = "400", description = "입력값 오류", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "현재 PIN 불일치", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "사용자 없음", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<PinUpdateResponse> updatePin(
            @Valid @RequestBody UpdatePinRequest request,
            Principal principal
    ) {
        return ResponseEntity.ok(pinService.updatePin(principal.getName(), request));
    }

    @PutMapping("/pin/reset-with-password")
    @Operation(summary = "비밀번호 기반 PIN 재설정", description = "로그인 사용자의 비밀번호를 검증한 뒤 새 PIN으로 재설정합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "PIN 재설정 성공"),
            @ApiResponse(responseCode = "400", description = "입력값 오류", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "비밀번호 불일치", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "사용자 없음", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<PinUpdateResponse> resetPinWithPassword(
            @Valid @RequestBody ResetPinWithPasswordRequest request,
            Principal principal
    ) {
        return ResponseEntity.ok(pinService.resetPinWithPassword(principal.getName(), request));
    }
}
