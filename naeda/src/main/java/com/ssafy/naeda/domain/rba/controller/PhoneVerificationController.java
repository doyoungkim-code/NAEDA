package com.ssafy.naeda.domain.rba.controller;

import com.ssafy.naeda.domain.rba.dto.request.PhoneVerifyRequest;
import com.ssafy.naeda.domain.rba.dto.response.PhoneVerifyResponse;
import com.ssafy.naeda.domain.rba.service.PhoneVerificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/rba/phone")
@Tag(name = "RBA 전화번호 인증", description = "전화번호 가운데 4자리 검증 API")
public class PhoneVerificationController {

    private final PhoneVerificationService phoneVerificationService;

    @PostMapping("/verify")
    @Operation(summary = "전화번호 가운데 4자리 검증", description = "입력한 전화번호 가운데 4자리가 사용자 전화번호와 일치하는지 검증합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "검증 성공 또는 실패"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청", content = @Content(schema = @Schema(implementation = com.ssafy.naeda.global.exception.ErrorResponse.class)))
    })
    public ResponseEntity<PhoneVerifyResponse> verify(@Valid @RequestBody PhoneVerifyRequest request) {
        boolean verified = phoneVerificationService.verify(request.getUserNo(), request.getMiddleDigits());
        String message = verified
                ? "전화번호 확인이 완료되었습니다."
                : "전화번호 가운데 4자리가 일치하지 않습니다.";
        return ResponseEntity.ok(
                PhoneVerifyResponse.builder()
                        .verified(verified)
                        .message(message)
                        .build()
        );
    }
}
