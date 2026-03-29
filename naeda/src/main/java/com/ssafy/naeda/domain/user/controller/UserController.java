package com.ssafy.naeda.domain.user.controller;

import com.ssafy.naeda.domain.user.dto.request.UpdateFcmTokenRequest;
import com.ssafy.naeda.domain.user.dto.response.UserResponse;
import com.ssafy.naeda.domain.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User", description = "회원 API")
public class UserController {

    private final UserService userService;

    @Operation(summary = "회원 정보 조회", description = "userNo로 회원 정보를 조회합니다.")
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMyInfo(@RequestParam Long userNo) {
        return ResponseEntity.ok(userService.getMyInfo(userNo));
    }

    @Operation(summary = "FCM 토큰 등록/갱신", description = "사용자의 FCM 디바이스 토큰을 등록하거나 갱신합니다.")
    @PutMapping("/me/fcm-token")
    public ResponseEntity<Void> updateFcmToken(
            @RequestParam Long userNo,
            @Valid @RequestBody UpdateFcmTokenRequest request
    ) {
        userService.updateFcmToken(userNo, request.getFcmToken());
        return ResponseEntity.ok().build();
    }
}
