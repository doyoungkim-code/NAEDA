package com.ssafy.naeda.domain.point.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.naeda.domain.point.dto.request.PointProductCreateRequest;
import com.ssafy.naeda.domain.point.dto.request.PointProductUpdateRequest;
import com.ssafy.naeda.domain.point.dto.response.PointProductResponse;
import com.ssafy.naeda.domain.point.entity.PointProductStatus;
import com.ssafy.naeda.domain.point.service.PointProductService;
import com.ssafy.naeda.global.exception.NotFoundException;
import com.ssafy.naeda.global.security.JwtAuthenticationFilter;
import com.ssafy.naeda.global.security.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PointProductController.class)
@AutoConfigureMockMvc(addFilters = false)
class PointProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PointProductService pointProductService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    private PointProductResponse buildResponse(Long id) {
        return PointProductResponse.builder()
                .productId(id)
                .productName("아메리카노 쿠폰")
                .description("스타벅스 아메리카노")
                .category("카페")
                .imageUrl("https://example.com/image.png")
                .pointPrice(3000L)
                .stockQuantity(100)
                .status(PointProductStatus.ON_SALE)
                .startsAt(LocalDateTime.of(2026, 1, 1, 0, 0))
                .endsAt(LocalDateTime.of(2026, 12, 31, 23, 59))
                .build();
    }

    @Test
    @DisplayName("POST /api/products - 상품 등록 성공 (201)")
    void createProduct() throws Exception {
        PointProductCreateRequest request = PointProductCreateRequest.builder()
                .productName("아메리카노 쿠폰")
                .description("스타벅스 아메리카노")
                .category("카페")
                .pointPrice(3000L)
                .stockQuantity(100)
                .build();

        given(pointProductService.createProduct(any())).willReturn(buildResponse(1L));

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.productId").value(1))
                .andExpect(jsonPath("$.productName").value("아메리카노 쿠폰"))
                .andExpect(jsonPath("$.category").value("카페"))
                .andExpect(jsonPath("$.pointPrice").value(3000))
                .andExpect(jsonPath("$.stockQuantity").value(100))
                .andExpect(jsonPath("$.status").value("ON_SALE"));
    }

    @Test
    @DisplayName("POST /api/products - 상품명 누락 시 400")
    void createProduct_noName() throws Exception {
        PointProductCreateRequest request = PointProductCreateRequest.builder()
                .pointPrice(3000L)
                .stockQuantity(100)
                .build();

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/products - 가격 음수 시 400")
    void createProduct_negativePrice() throws Exception {
        PointProductCreateRequest request = PointProductCreateRequest.builder()
                .productName("테스트 상품")
                .pointPrice(-1000L)
                .stockQuantity(100)
                .build();

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/products - 가격 1억 초과 시 400")
    void createProduct_priceExceedsMax() throws Exception {
        PointProductCreateRequest request = PointProductCreateRequest.builder()
                .productName("테스트 상품")
                .pointPrice(100_000_001L)
                .stockQuantity(100)
                .build();

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/products/{id} - 단건 조회 성공")
    void getProduct() throws Exception {
        given(pointProductService.getProduct(1L)).willReturn(buildResponse(1L));

        mockMvc.perform(get("/api/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(1))
                .andExpect(jsonPath("$.productName").value("아메리카노 쿠폰"));
    }

    @Test
    @DisplayName("GET /api/products/{id} - 없는 상품 시 404")
    void getProduct_notFound() throws Exception {
        given(pointProductService.getProduct(999L))
                .willThrow(new NotFoundException("포인트 상품을 찾을 수 없습니다. id=999"));

        mockMvc.perform(get("/api/products/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/products - 전체 목록 조회")
    void getAllProducts() throws Exception {
        PointProductResponse r1 = buildResponse(1L);
        PointProductResponse r2 = PointProductResponse.builder()
                .productId(2L)
                .productName("치킨 교환권")
                .pointPrice(10000L)
                .stockQuantity(50)
                .category("음식")
                .status(PointProductStatus.ON_SALE)
                .build();
        given(pointProductService.getAllProducts()).willReturn(List.of(r1, r2));

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].productName").value("아메리카노 쿠폰"))
                .andExpect(jsonPath("$[1].productName").value("치킨 교환권"));
    }

    @Test
    @DisplayName("GET /api/products/available - 파라미터 없이 조회")
    void getAvailableProducts_noParams() throws Exception {
        given(pointProductService.getAvailableProducts(null, null))
                .willReturn(List.of(buildResponse(1L)));

        mockMvc.perform(get("/api/products/available"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].productName").value("아메리카노 쿠폰"));
    }

    @Test
    @DisplayName("GET /api/products/available?category=카페 - 카테고리 필터")
    void getAvailableProducts_withCategory() throws Exception {
        given(pointProductService.getAvailableProducts("카페", null))
                .willReturn(List.of(buildResponse(1L)));

        mockMvc.perform(get("/api/products/available").param("category", "카페"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("GET /api/products/available?keyword=쿠폰 - 키워드 검색")
    void getAvailableProducts_withKeyword() throws Exception {
        given(pointProductService.getAvailableProducts(null, "쿠폰"))
                .willReturn(List.of(buildResponse(1L)));

        mockMvc.perform(get("/api/products/available").param("keyword", "쿠폰"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("GET /api/products/available?category=카페&keyword=아메리카노 - 카테고리 + 키워드")
    void getAvailableProducts_withCategoryAndKeyword() throws Exception {
        given(pointProductService.getAvailableProducts("카페", "아메리카노"))
                .willReturn(List.of(buildResponse(1L)));

        mockMvc.perform(get("/api/products/available")
                        .param("category", "카페")
                        .param("keyword", "아메리카노"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].productName").value("아메리카노 쿠폰"));
    }

    @Test
    @DisplayName("PUT /api/products/{id} - 수정 성공")
    void updateProduct() throws Exception {
        PointProductUpdateRequest request = PointProductUpdateRequest.builder()
                .productName("수정된 상품")
                .pointPrice(5000L)
                .stockQuantity(50)
                .status(PointProductStatus.SOLD_OUT)
                .build();

        PointProductResponse updated = PointProductResponse.builder()
                .productId(1L)
                .productName("수정된 상품")
                .pointPrice(5000L)
                .stockQuantity(50)
                .status(PointProductStatus.SOLD_OUT)
                .build();
        given(pointProductService.updateProduct(eq(1L), any())).willReturn(updated);

        mockMvc.perform(put("/api/products/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productName").value("수정된 상품"))
                .andExpect(jsonPath("$.pointPrice").value(5000))
                .andExpect(jsonPath("$.status").value("SOLD_OUT"));
    }

    @Test
    @DisplayName("PUT /api/products/{id} - status 누락 시 400")
    void updateProduct_noStatus() throws Exception {
        PointProductUpdateRequest request = PointProductUpdateRequest.builder()
                .productName("수정된 상품")
                .pointPrice(5000L)
                .stockQuantity(50)
                .build();

        mockMvc.perform(put("/api/products/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("DELETE /api/products/{id} - 삭제 성공 (204)")
    void deleteProduct() throws Exception {
        willDoNothing().given(pointProductService).deleteProduct(1L);

        mockMvc.perform(delete("/api/products/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /api/products/{id} - 없는 상품 시 404")
    void deleteProduct_notFound() throws Exception {
        willThrow(new NotFoundException("포인트 상품을 찾을 수 없습니다. id=999"))
                .given(pointProductService).deleteProduct(999L);

        mockMvc.perform(delete("/api/products/999"))
                .andExpect(status().isNotFound());
    }
}
