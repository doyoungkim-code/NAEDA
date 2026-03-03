package com.ssafy.naeda.domain.face.controller;

import com.ssafy.naeda.domain.face.dto.response.EnrollResponse;
import com.ssafy.naeda.domain.face.dto.response.SearchResponse;
import com.ssafy.naeda.domain.face.service.FaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/face")
@RequiredArgsConstructor
public class FaceController {

    private final FaceService faceService;

    /**
     * 서버 상태 확인
     * GET /api/v1/face/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    /**
     * 얼굴 등록
     * POST /api/v1/face/enroll
     * multipart: userId(String), pose(String), image(파일)
     */
    @PostMapping(value = "/enroll", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<EnrollResponse> enroll(
            @RequestPart("userId") String userId,
            @RequestPart("pose") String pose,
            @RequestPart("image") MultipartFile image) {
        return ResponseEntity.ok(faceService.enroll(userId, pose, image));
    }

    /**
     * 얼굴 검색
     * POST /api/v1/face/search
     * multipart: image(파일), topK(숫자, 기본값 3)
     */
    @PostMapping(value = "/search", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SearchResponse> search(
            @RequestPart("image") MultipartFile image,
            @RequestPart(value = "topK", required = false) String topK) {
        int k = (topK != null && !topK.isBlank()) ? Integer.parseInt(topK) : 3;
        return ResponseEntity.ok(faceService.search(image, k));
    }
}
