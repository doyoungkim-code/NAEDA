package com.ssafy.naeda.domain.store.controller;

import com.ssafy.naeda.domain.store.dto.request.StoreCreateRequest;
import com.ssafy.naeda.domain.store.dto.response.StoreResponse;
import com.ssafy.naeda.domain.store.service.StoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import org.springframework.http.HttpStatus;

import java.util.List;

@RestController
@RequestMapping("/api/stores")
@RequiredArgsConstructor
@Validated
public class StoreController {

    private final StoreService storeService;

    /**
     * GET /api/stores?category=CG-9ca85f66311a23d&facePayOnly=true
     */
    @GetMapping
    public ResponseEntity<List<StoreResponse>> getStores(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Boolean facePayOnly
    ) {
        return ResponseEntity.ok(storeService.getStores(category, facePayOnly));
    }

    /**
     * GET /api/stores/map
     */
    @GetMapping("/map")
    public ResponseEntity<List<StoreResponse>> getMapStores() {
        return ResponseEntity.ok(storeService.getMapStores());
    }

    /**
     * GET /api/stores/{storeId}
     */
    @GetMapping("/{storeId}")
    public ResponseEntity<StoreResponse> getStore(@PathVariable Long storeId) {
        return ResponseEntity.ok(storeService.getStore(storeId));
    }

    /**
     * POST /api/stores
     */
    @PostMapping
    public ResponseEntity<StoreResponse> createStore(@Valid @RequestBody StoreCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(storeService.createStore(request));
    }
}
