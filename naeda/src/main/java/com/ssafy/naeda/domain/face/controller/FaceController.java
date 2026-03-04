package com.ssafy.naeda.domain.face.controller;

import com.ssafy.naeda.domain.face.dto.response.EnrollResponse;
import com.ssafy.naeda.domain.face.dto.response.SearchResponse;
import com.ssafy.naeda.domain.face.service.FaceService;
import com.ssafy.naeda.global.exception.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/face")
@RequiredArgsConstructor
@Tag(name = "얼굴 인식", description = "얼굴 등록 및 유사도 검색 API")
public class FaceController {

    private final FaceService faceService;

    /**
     * 서버 상태 확인
     * GET /api/v1/face/health
     */
    @GetMapping("/health")
    @Operation(summary = "얼굴 서비스 상태 확인", description = "얼굴 인식 서비스의 헬스 체크 결과를 반환합니다.")
    @ApiResponse(responseCode = "200", description = "정상 응답")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    /**
     * 얼굴 등록
     * POST /api/v1/face/enroll
     * multipart: userId(String), pose(String), image(파일)
     */
    @PostMapping(value = "/enroll", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "얼굴 등록", description = "사용자 얼굴 이미지를 받아 임베딩을 저장합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "등록 성공"),
            @ApiResponse(responseCode = "400", description = "요청 값 오류", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<EnrollResponse> enroll(
            @Parameter(description = "사용자 ID", example = "user-1001", required = true)
            @RequestPart("userId") String userId,
            @Parameter(description = "얼굴 포즈(정면/좌/우 등)", example = "front", required = true)
            @RequestPart("pose") String pose,
            @Parameter(description = "얼굴 이미지 파일", required = true)
            @RequestPart("image") MultipartFile image) {
        return ResponseEntity.ok(faceService.enroll(userId, pose, image));
    }

    /**
     * 얼굴 검색
     * POST /api/v1/face/search
     * multipart: image(파일), topK(숫자, 기본값 3)
     */
    @PostMapping(value = "/search", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "얼굴 검색", description = "입력 이미지와 저장된 얼굴 임베딩을 비교하여 유사한 사용자를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "검색 성공"),
            @ApiResponse(responseCode = "400", description = "요청 값 오류", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<SearchResponse> search(
            @Parameter(description = "검색할 얼굴 이미지 파일", required = true)
            @RequestPart("image") MultipartFile image,
            @Parameter(description = "상위 후보 개수(미입력 시 3)", example = "3")
            @RequestPart(value = "topK", required = false) String topK) {
        int k = (topK != null && !topK.isBlank()) ? Integer.parseInt(topK) : 3;
        return ResponseEntity.ok(faceService.search(image, k));
    }
}
