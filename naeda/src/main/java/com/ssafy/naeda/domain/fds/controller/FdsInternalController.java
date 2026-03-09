package com.ssafy.naeda.domain.fds.controller;

import com.ssafy.naeda.domain.fds.dto.response.FdsVersionResponse;
import com.ssafy.naeda.domain.fds.service.FdsVersionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/internal/fds")
@RequiredArgsConstructor
@Tag(name = "FDS Internal", description = "FDS 내부 버전 조회 API")
public class FdsInternalController {

    private final FdsVersionService fdsVersionService;

    @GetMapping("/version")
    @Operation(summary = "현재 FDS 버전 조회", description = "현재 적용 중인 feature/rule/model 버전을 조회합니다.")
    public ResponseEntity<FdsVersionResponse> getCurrentVersion() {
        return ResponseEntity.ok(fdsVersionService.getCurrentVersion());
    }
}
