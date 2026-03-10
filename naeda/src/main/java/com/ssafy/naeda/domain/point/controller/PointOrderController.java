package com.ssafy.naeda.domain.point.controller;

import com.ssafy.naeda.domain.point.dto.request.PointOrderCreateRequest;
import com.ssafy.naeda.domain.point.dto.response.PointOrderResponse;
import com.ssafy.naeda.domain.point.service.PointOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class PointOrderController {

    private final PointOrderService pointOrderService;

    @PostMapping
    public ResponseEntity<PointOrderResponse> purchaseProduct (
            @RequestParam Long userNo,
            @RequestBody @Valid PointOrderCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(pointOrderService.purchaseProduct(userNo, request));
    }
}
