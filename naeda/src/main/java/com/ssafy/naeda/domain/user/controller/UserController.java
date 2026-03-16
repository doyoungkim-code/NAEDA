package com.ssafy.naeda.domain.user.controller;


import com.ssafy.naeda.domain.user.dto.response.UserResponse;
import com.ssafy.naeda.domain.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User", description = "회원 API")
public class UserController {

    private final UserService userService;

    @Operation(summary = "회원 정보 조회",description = "userNo로 회원 정보를 조회합니다.")
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMyInfo(@RequestParam Long userNo){
        return ResponseEntity.ok(userService.getMyInfo(userNo));
    }
}
