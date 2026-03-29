package com.ssafy.naeda.domain.address.controller;

import com.ssafy.naeda.domain.address.dto.request.AddressRequest;
import com.ssafy.naeda.domain.address.dto.response.AddressResponse;
import com.ssafy.naeda.domain.address.service.AddressService;
import com.ssafy.naeda.global.exception.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/addresses")
@RequiredArgsConstructor
@Validated
@Tag(name = "배송 주소", description = "배송 주소 CRUD 및 기본 배송지 설정 API")
public class AddressController {

    private final AddressService addressService;

    @GetMapping("/{userNo}")
    @Operation(summary = "주소 목록 조회", description = "사용자의 전체 배송 주소 목록을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    public ResponseEntity<List<AddressResponse>> getAddresses(@PathVariable @Positive Long userNo) {
        return ResponseEntity.ok(addressService.getAddresses(userNo));
    }

    @GetMapping("/{userNo}/{addressId}")
    @Operation(summary = "주소 단건 조회", description = "배송 주소를 단건 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "주소를 찾을 수 없음", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<AddressResponse> getAddress(
            @PathVariable @Positive Long userNo,
            @PathVariable @Positive Long addressId) {
        return ResponseEntity.ok(addressService.getAddress(userNo, addressId));
    }

    @PostMapping("/{userNo}")
    @Operation(summary = "주소 생성", description = "새 배송 주소를 등록합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "생성 성공"),
            @ApiResponse(responseCode = "400", description = "요청 값 오류", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<AddressResponse> createAddress(
            @PathVariable @Positive Long userNo,
            @Valid @RequestBody AddressRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(addressService.createAddress(userNo, request));
    }

    @PutMapping("/{userNo}/{addressId}")
    @Operation(summary = "주소 수정", description = "배송 주소를 수정합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수정 성공"),
            @ApiResponse(responseCode = "404", description = "주소를 찾을 수 없음", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<AddressResponse> updateAddress(
            @PathVariable @Positive Long userNo,
            @PathVariable @Positive Long addressId,
            @Valid @RequestBody AddressRequest request) {
        return ResponseEntity.ok(addressService.updateAddress(userNo, addressId, request));
    }

    @DeleteMapping("/{userNo}/{addressId}")
    @Operation(summary = "주소 삭제", description = "배송 주소를 삭제합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "삭제 성공"),
            @ApiResponse(responseCode = "404", description = "주소를 찾을 수 없음", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> deleteAddress(
            @PathVariable @Positive Long userNo,
            @PathVariable @Positive Long addressId) {
        addressService.deleteAddress(userNo, addressId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{userNo}/{addressId}/default")
    @Operation(summary = "기본 배송지 설정", description = "해당 주소를 기본 배송지로 설정합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "설정 성공"),
            @ApiResponse(responseCode = "404", description = "주소를 찾을 수 없음", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<AddressResponse> setDefaultAddress(
            @PathVariable @Positive Long userNo,
            @PathVariable @Positive Long addressId) {
        return ResponseEntity.ok(addressService.setDefaultAddress(userNo, addressId));
    }
}