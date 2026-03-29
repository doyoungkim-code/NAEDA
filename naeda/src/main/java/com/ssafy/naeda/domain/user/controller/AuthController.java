package com.ssafy.naeda.domain.user.controller;

import com.ssafy.naeda.domain.user.dto.request.*;
import com.ssafy.naeda.domain.user.dto.response.*;
import com.ssafy.naeda.domain.user.service.AuthService;
import com.ssafy.naeda.global.exception.AuthenticationFailedException;
import com.ssafy.naeda.global.exception.DuplicateException;
import com.ssafy.naeda.global.exception.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "인증", description = "회원가입, 로그인, 토큰 재발급 API")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    @Operation(summary = "회원가입", description = "새로운 사용자를 등록합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "회원가입 성공"),
            @ApiResponse(responseCode = "400", description = "입력값 검증 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "아이디 중복",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<SignupResponse> signup(@Valid @RequestBody SignupRequest request) {
        SignupResponse response = authService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/check-email")
    @Operation(summary = "이메일 중복 확인", description = "이미 등록된 이메일(아이디)인지 확인합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "사용 가능한 이메일"),
            @ApiResponse(responseCode = "409", description = "이미 등록된 이메일",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> checkEmail(@RequestParam String email) {
        if (authService.isUserIdDuplicate(email)) {
            throw new DuplicateException("이미 사용 중인 이메일입니다.");
        }
        return ResponseEntity.ok().build();
    }

    @GetMapping("/check-phone")
    @Operation(summary = "전화번호 중복 확인", description = "이미 등록된 전화번호인지 확인합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "사용 가능한 전화번호"),
            @ApiResponse(responseCode = "409", description = "이미 등록된 전화번호",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> checkPhone(@RequestParam String phone) {
        if (authService.isPhoneDuplicate(phone)) {
            throw new DuplicateException("이미 등록된 전화번호입니다.");
        }
        return ResponseEntity.ok().build();
    }

    @PostMapping("/login")
    @Operation(summary = "로그인", description = "아이디와 비밀번호로 로그인합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그인 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = org.springframework.web.ErrorResponse.class))),
            @ApiResponse(responseCode = "502", description = "SSAFY API 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request){
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }


    @PostMapping("/refresh")
    @Operation(summary = "토큰 재발급", description = "RefreshToken으로 새로운 AccessToken과 RefreshToken을 발급합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "재발급 성공"),
            @ApiResponse(responseCode = "401", description = "유효하지 않은 리프레시 토큰", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<LoginResponse> refresh(@Valid @RequestBody RefreshTokenRequest request){
        LoginResponse response = authService.refresh(request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/withdraw")
    @Operation(summary = "회원탈퇴", description = "사용자의 모든 데이터를 삭제하고 계정을 탈퇴합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "회원탈퇴 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> withdraw(HttpServletRequest httpRequest) {
        String authHeader = httpRequest.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new AuthenticationFailedException("AccessToken이 필요합니다.");
        }
        String accessToken = authHeader.substring(7);
        String userId = authService.getUserIdFromToken(accessToken);
        authService.withdraw(userId, accessToken);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/logout")
    @Operation(summary = "로그아웃", description = "RefreshToken을 무효화하여 로그아웃합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그아웃 성공"),
            @ApiResponse(responseCode = "401", description = "유효하지 않은 토큰",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> logout(
            HttpServletRequest httpRequest,
            @Valid @RequestBody RefreshTokenRequest request
    ){
        String authHeader = httpRequest.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new AuthenticationFailedException("AccessToken이 필요합니다.");
        }
        String accessToken = authHeader.substring(7);
        authService.logout(request, accessToken);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/password-reset/request")
    @Operation(summary = "비밀번호 재설정 요청", description = "휴대폰번호로 6자리 인증 코드를 발급합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "인증 코드 발급 성공"),
            @ApiResponse(responseCode = "404", description = "등록되지 않은 휴대폰번호",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<PasswordResetCodeResponse> requestPasswordReset(
            @Valid @RequestBody PasswordResetRequestDto request) {
        PasswordResetCodeResponse response = authService.requestPasswordReset(request.getPhone());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/password-reset/verify")
    @Operation(summary = "비밀번호 재설정 코드 검증", description = "발급된 인증 코드를 검증하고 비밀번호 변경용 토큰을 발급합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "코드 검증 성공"),
            @ApiResponse(responseCode = "400", description = "인증 코드 불일치 또는 만료",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<PasswordResetVerifyResponse> verifyPasswordResetCode(
            @Valid @RequestBody PasswordResetVerifyRequest request) {
        PasswordResetVerifyResponse response = authService.verifyPasswordResetCode(
                request.getPhone(), request.getCode());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/password-reset/confirm")
    @Operation(summary = "비밀번호 재설정 확인", description = "검증 토큰과 새 비밀번호로 비밀번호를 변경합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "비밀번호 변경 성공"),
            @ApiResponse(responseCode = "400", description = "유효하지 않거나 만료된 토큰",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> confirmPasswordReset(
            @Valid @RequestBody PasswordResetConfirmRequest request) {
        authService.confirmPasswordReset(request.getToken(), request.getNewPassword());
        return ResponseEntity.ok().build();
    }
}


//"accessToken": "eyJhbGciOiJIUzM4NCJ9.eyJzdWIiOiJ0ZXN0MTBAc3NhZnkuY28ua3IiLCJ0eXBlIjoiYWNjZXNzIiwidXNlcktleSI6IjEwODVkZDYxLTQwODktNGVjZC05Yjc5LWI5NTMxMzhlYmI4YyIsImlhdCI6MTc3Mjk4NjMyMiwiZXhwIjoxNzcyOTg4MTIyfQ.g-RtPmaE8l6q4Slmw_HB7Z4w1_GmpVzMoGSD9Ho4luvQB9eRtPq21jbDymvzKb6u",   "refreshToken": "eyJhbGciOiJIUzM4NCJ9.eyJzdWIiOiJ0ZXN0MTBAc3NhZnkuY28ua3IiLCJ0eXBlIjoicmVmcmVzaCIsInVzZXJLZXkiOiIxMDg1ZGQ2MS00MDg5LTRlY2QtOWI3OS1iOTUzMTM4ZWJiOGMiLCJpYXQiOjE3NzI5ODYzMjIsImV4cCI6MTc3MzU5MTEyMn0.xCyIrsSSVTbGqEVzYoJG8LLdosuUKqhnhrEAGm_pR8Mzm_ZoKWEJCLe7JNatUgHO"