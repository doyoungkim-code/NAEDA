package com.ssafy.naeda.domain.recommend.controller;

import com.ssafy.naeda.domain.recommend.dto.response.RecommendResponse;
import com.ssafy.naeda.domain.recommend.service.RecommendService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recommend")
@RequiredArgsConstructor
@Tag(name = "Recommend", description = "구미 추천 API")
public class RecommendController {

    private final RecommendService recommendService;

    /**
     * GET /api/recommend/stores?dong=진평동&category=식당&sort=rating
     */
    @GetMapping("/stores")
    @Operation(summary = "추천 가게 목록 조회", description = "동/카테고리/정렬 필터 적용 가능")
    public ResponseEntity<List<RecommendResponse>> getRecommendStores(
            @RequestParam(required = false) String dong,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String sort
    ) {
        return ResponseEntity.ok(recommendService.getRecommendStores(dong, category, sort));
    }

    /**
     * GET /api/recommend/dongs
     */
    @GetMapping("/dongs")
    @Operation(summary = "동 목록 조회", description = "가게가 존재하는 동 목록 반환")
    public ResponseEntity<List<String>> getDongs() {
        return ResponseEntity.ok(recommendService.getDongs());
    }
}
