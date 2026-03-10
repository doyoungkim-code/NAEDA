package com.ssafy.naeda.domain.identity.controller;

import com.ssafy.naeda.domain.identity.dto.request.ResidentIdConfirmRequest;
import com.ssafy.naeda.domain.identity.dto.response.ResidentIdExtractResponse;
import com.ssafy.naeda.domain.identity.dto.response.ResidentIdVerifyResponse;
import com.ssafy.naeda.domain.identity.service.ResidentIdVerifyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/identity/id-card")
@RequiredArgsConstructor
@Tag(name = "신분증 OCR", description = "주민등록증/운전면허증 OCR 기반 본인 확인 API")
public class ResidentIdVerifyController {

    private final ResidentIdVerifyService residentIdVerifyService;

    @PostMapping(value = "/extract", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "신분증 OCR 추출", description = "주민등록증 또는 운전면허증 이미지에서 이름과 주민등록번호 일부를 추출해 확인 화면에 사용할 값을 반환합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OCR 추출 완료"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 또는 OCR 인식 실패", content = @Content(schema = @Schema(implementation = Object.class))),
            @ApiResponse(responseCode = "503", description = "OCR 서비스 장애", content = @Content(schema = @Schema(implementation = Object.class)))
    })
    public ResponseEntity<ResidentIdExtractResponse> extract(
            @RequestPart("image") MultipartFile image
    ) {
        return ResponseEntity.ok(residentIdVerifyService.extract(image));
    }

    @PostMapping(value = "/confirm", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "신분증 OCR 확인", description = "사용자가 확인 또는 수정한 이름과 주민등록번호 일부를 로그인 사용자 정보와 비교합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "확인 완료"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청", content = @Content(schema = @Schema(implementation = Object.class))),
            @ApiResponse(responseCode = "404", description = "사용자 없음", content = @Content(schema = @Schema(implementation = Object.class)))
    })
    public ResponseEntity<ResidentIdVerifyResponse> confirm(
            @Valid @RequestBody ResidentIdConfirmRequest request,
            Principal principal
    ) {
        return ResponseEntity.ok(residentIdVerifyService.confirm(principal.getName(), request));
    }
}
