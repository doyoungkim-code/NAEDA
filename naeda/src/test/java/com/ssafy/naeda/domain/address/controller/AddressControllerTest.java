package com.ssafy.naeda.domain.address.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.naeda.domain.address.dto.request.AddressRequest;
import com.ssafy.naeda.domain.address.dto.response.AddressResponse;
import com.ssafy.naeda.domain.address.service.AddressService;
import com.ssafy.naeda.global.exception.GlobalExceptionHandler;
import com.ssafy.naeda.global.exception.NotFoundException;
import com.ssafy.naeda.global.security.JwtAuthenticationFilter;
import com.ssafy.naeda.global.security.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AddressController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AddressControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AddressService addressService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    private AddressResponse response(Long id, boolean isDefault) {
        return AddressResponse.builder()
                .addressId(id)
                .addressName("집")
                .recipient("홍길동")
                .phone("01012345678")
                .roadAddress("서울시 강남구 테헤란로 1")
                .numberAddress("역삼동 123-4")
                .detailAddress("101호")
                .zipCode("06234")
                .isDefault(isDefault)
                .build();
    }

    private AddressRequest validRequest() {
        return AddressRequest.builder()
                .addressName("집")
                .recipient("홍길동")
                .phone("01012345678")
                .roadAddress("서울시 강남구 테헤란로 1")
                .numberAddress("역삼동 123-4")
                .detailAddress("101호")
                .zipCode("06234")
                .isDefault(false)
                .build();
    }

    // ── GET /api/addresses/{userNo} ───────────────────────────────────────

    @Test
    @DisplayName("주소 목록 조회 - 200")
    void getAddresses() throws Exception {
        given(addressService.getAddresses(1L))
                .willReturn(List.of(response(1L, true), response(2L, false)));

        mockMvc.perform(get("/api/addresses/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].addressId").value(1))
                .andExpect(jsonPath("$[1].addressId").value(2));
    }

    @Test
    @DisplayName("주소 목록 조회 - 빈 목록 200")
    void getAddresses_empty() throws Exception {
        given(addressService.getAddresses(1L)).willReturn(List.of());

        mockMvc.perform(get("/api/addresses/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ── GET /api/addresses/{userNo}/{addressId} ───────────────────────────

    @Test
    @DisplayName("주소 단건 조회 - 200")
    void getAddress() throws Exception {
        given(addressService.getAddress(1L, 10L)).willReturn(response(10L, false));

        mockMvc.perform(get("/api/addresses/1/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.addressId").value(10))
                .andExpect(jsonPath("$.recipient").value("홍길동"));
    }

    @Test
    @DisplayName("주소 단건 조회 - 존재하지 않는 주소 404")
    void getAddress_notFound() throws Exception {
        given(addressService.getAddress(1L, 999L))
                .willThrow(new NotFoundException("주소를 찾을 수 없습니다."));

        mockMvc.perform(get("/api/addresses/1/999"))
                .andExpect(status().isNotFound());
    }

    // ── POST /api/addresses/{userNo} ──────────────────────────────────────

    @Test
    @DisplayName("주소 생성 - 201")
    void createAddress() throws Exception {
        given(addressService.createAddress(eq(1L), any(AddressRequest.class)))
                .willReturn(response(10L, false));

        mockMvc.perform(post("/api/addresses/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.addressId").value(10));
    }

    @Test
    @DisplayName("주소 생성 - 필수값 누락 400")
    void createAddress_badRequest() throws Exception {
        AddressRequest invalid = AddressRequest.builder()
                .addressName("집")
                .isDefault(false)
                .build();

        mockMvc.perform(post("/api/addresses/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    // ── PUT /api/addresses/{userNo}/{addressId} ───────────────────────────

    @Test
    @DisplayName("주소 수정 - 200")
    void updateAddress() throws Exception {
        given(addressService.updateAddress(eq(1L), eq(10L), any(AddressRequest.class)))
                .willReturn(response(10L, false));

        mockMvc.perform(put("/api/addresses/1/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.addressId").value(10));
    }

    // ── DELETE /api/addresses/{userNo}/{addressId} ────────────────────────

    @Test
    @DisplayName("주소 삭제 - 204")
    void deleteAddress() throws Exception {
        willDoNothing().given(addressService).deleteAddress(1L, 10L);

        mockMvc.perform(delete("/api/addresses/1/10"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("주소 삭제 - 존재하지 않는 주소 404")
    void deleteAddress_notFound() throws Exception {
        willThrow(new NotFoundException("주소를 찾을 수 없습니다."))
                .given(addressService).deleteAddress(1L, 999L);

        mockMvc.perform(delete("/api/addresses/1/999"))
                .andExpect(status().isNotFound());
    }

    // ── PATCH /api/addresses/{userNo}/{addressId}/default ─────────────────

    @Test
    @DisplayName("기본 배송지 설정 - 200")
    void setDefaultAddress() throws Exception {
        given(addressService.setDefaultAddress(1L, 10L)).willReturn(response(10L, true));

        mockMvc.perform(patch("/api/addresses/1/10/default"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isDefault").value(true));
    }
}