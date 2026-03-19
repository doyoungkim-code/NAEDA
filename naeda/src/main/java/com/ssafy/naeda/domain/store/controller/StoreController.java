package com.ssafy.naeda.domain.store.controller;

import com.ssafy.naeda.domain.store.dto.request.PublicStoreCreateRequest;
import com.ssafy.naeda.domain.store.dto.request.StoreCreateRequest;
import com.ssafy.naeda.domain.store.dto.response.StoreResponse;
import com.ssafy.naeda.domain.store.service.StoreService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/stores")
@RequiredArgsConstructor
@Validated
public class StoreController {

    private final StoreService storeService;

    @GetMapping
    public ResponseEntity<List<StoreResponse>> getStores(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Boolean facePayOnly
    ) {
        return ResponseEntity.ok(storeService.getStores(category, facePayOnly));
    }

    @GetMapping("/map")
    public ResponseEntity<List<StoreResponse>> getMapStores() {
        return ResponseEntity.ok(storeService.getMapStores());
    }

    @GetMapping("/{storeId}")
    public ResponseEntity<StoreResponse> getStore(@PathVariable Long storeId) {
        return ResponseEntity.ok(storeService.getStore(storeId));
    }

    @PostMapping
    public ResponseEntity<StoreResponse> createStore(@Valid @RequestBody StoreCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(storeService.createStore(request));
    }

    @PostMapping("/public")
    public ResponseEntity<StoreResponse> createPublicStore(@Valid @RequestBody PublicStoreCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(storeService.createPublicStore(request));
    }
}
