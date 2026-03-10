package com.ssafy.naeda.domain.point.controller;

import com.ssafy.naeda.domain.point.dto.request.PointProductCreateRequest;
import com.ssafy.naeda.domain.point.dto.request.PointProductUpdateRequest;
import com.ssafy.naeda.domain.point.dto.response.PointProductResponse;
import com.ssafy.naeda.domain.point.service.PointProductService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/products")
@Validated
public class PointProductController {

    private final PointProductService pointProductService;

    @PostMapping
    public ResponseEntity<PointProductResponse> createProduct (
            @RequestBody @Valid PointProductCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(pointProductService.createProduct(request));
    }

    @GetMapping("/{productId}")
    public ResponseEntity<PointProductResponse> getProduct(
            @PathVariable Long productId
    ) {
        return ResponseEntity.ok(pointProductService.getProduct(productId));
    }

    @GetMapping
    public ResponseEntity<List<PointProductResponse>> getAllProducts (
            @RequestParam(defaultValue = "100") @Positive @Max(500) int size
    ) {
        return ResponseEntity.ok(pointProductService.getAllProducts().stream().limit(size).toList());
    }

    @GetMapping("/available")
    public ResponseEntity<List<PointProductResponse>> getAvailableProducts(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "100") @Positive @Max(500) int size
    ) {
        return ResponseEntity.ok(pointProductService.getAvailableProducts(category, keyword).stream().limit(size).toList());
    }

    @PutMapping("/{productId}")
    public ResponseEntity<PointProductResponse> updateProduct (
            @PathVariable Long productId,
            @RequestBody @Valid PointProductUpdateRequest request
    ) {
        return ResponseEntity.ok(pointProductService.updateProduct(productId, request));
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> deleteProduct(
            @PathVariable Long productId
    ) {
        pointProductService.deleteProduct(productId);
        return ResponseEntity.noContent().build();
    }
}
