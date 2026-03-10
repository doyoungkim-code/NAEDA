package com.ssafy.naeda.domain.point.controller;

import com.ssafy.naeda.domain.point.dto.request.PointProductCreateRequest;
import com.ssafy.naeda.domain.point.dto.request.PointProductUpdateRequest;
import com.ssafy.naeda.domain.point.dto.response.PointProductResponse;
import com.ssafy.naeda.domain.point.service.PointProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/products")
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
    public ResponseEntity<List<PointProductResponse>> getAllProducts () {
        return ResponseEntity.ok(pointProductService.getAllProducts());
    }

    @GetMapping("/available")
    public ResponseEntity<List<PointProductResponse>> getAvailableProducts(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword
    ) {
        return ResponseEntity.ok(pointProductService.getAvailableProducts(category, keyword));
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
