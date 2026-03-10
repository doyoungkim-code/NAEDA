package com.ssafy.naeda.domain.point.controller;

import com.ssafy.naeda.domain.point.dto.request.PointOrderCreateRequest;
import com.ssafy.naeda.domain.point.dto.response.PointOrderResponse;
import com.ssafy.naeda.domain.point.service.PointOrderService;
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
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Validated
public class PointOrderController {

    private final PointOrderService pointOrderService;

    @PostMapping
    public ResponseEntity<PointOrderResponse> purchaseProduct (
            @RequestParam @Positive Long userNo,
            @RequestBody @Valid PointOrderCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(pointOrderService.purchaseProduct(userNo, request));
    }

    @GetMapping
    public ResponseEntity<List<PointOrderResponse>> getMyOrders(
            @RequestParam @Positive Long userNo,
            @RequestParam(defaultValue = "100") @Positive @Max(500) int size
    ) {
        return ResponseEntity.ok(pointOrderService.getMyOrders(userNo).stream().limit(size).toList());
    }
}
