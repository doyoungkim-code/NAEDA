package com.ssafy.naeda.domain.point.service;

import com.ssafy.naeda.domain.point.dto.request.PointProductCreateRequest;
import com.ssafy.naeda.domain.point.dto.request.PointProductUpdateRequest;
import com.ssafy.naeda.domain.point.dto.response.PointProductResponse;
import com.ssafy.naeda.domain.point.entity.PointProduct;
import com.ssafy.naeda.domain.point.entity.PointProductStatus;
import com.ssafy.naeda.domain.point.repository.PointProductRepository;
import com.ssafy.naeda.global.exception.NotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class PointProductServiceTest {

    @Mock
    private PointProductRepository pointProductRepository;

    @InjectMocks
    private PointProductService pointProductService;

    private PointProduct buildProduct(Long id) {
        return PointProduct.builder()
                .productName("아메리카노 쿠폰")
                .description("스타벅스 아메리카노")
                .category("카페")
                .imageUrl("https://example.com/image.png")
                .pointPrice(3000L)
                .stockQuantity(100)
                .startsAt(LocalDateTime.of(2026, 1, 1, 0, 0))
                .endsAt(LocalDateTime.of(2026, 12, 31, 23, 59))
                .build();
    }

    @Test
    @DisplayName("상품 등록 성공")
    void createProduct() {
        PointProductCreateRequest request = PointProductCreateRequest.builder()
                .productName("아메리카노 쿠폰")
                .description("스타벅스 아메리카노")
                .category("카페")
                .imageUrl("https://example.com/image.png")
                .pointPrice(3000L)
                .stockQuantity(100)
                .startsAt(LocalDateTime.of(2026, 1, 1, 0, 0))
                .endsAt(LocalDateTime.of(2026, 12, 31, 23, 59))
                .build();

        PointProduct saved = buildProduct(1L);
        given(pointProductRepository.save(any(PointProduct.class))).willReturn(saved);

        PointProductResponse response = pointProductService.createProduct(request);

        assertThat(response.getProductName()).isEqualTo("아메리카노 쿠폰");
        assertThat(response.getCategory()).isEqualTo("카페");
        assertThat(response.getPointPrice()).isEqualTo(3000L);
        assertThat(response.getStockQuantity()).isEqualTo(100);
        assertThat(response.getStatus()).isEqualTo(PointProductStatus.ON_SALE);
        then(pointProductRepository).should().save(any(PointProduct.class));
    }

    @Test
    @DisplayName("상품 단건 조회 성공")
    void getProduct() {
        PointProduct product = buildProduct(1L);
        given(pointProductRepository.findById(1L)).willReturn(Optional.of(product));

        PointProductResponse response = pointProductService.getProduct(1L);

        assertThat(response.getProductName()).isEqualTo("아메리카노 쿠폰");
        assertThat(response.getPointPrice()).isEqualTo(3000L);
    }

    @Test
    @DisplayName("상품 단건 조회 - 없는 상품 시 예외")
    void getProduct_notFound() {
        given(pointProductRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> pointProductService.getProduct(999L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("999");
    }

    @Test
    @DisplayName("상품 전체 목록 조회")
    void getAllProducts() {
        PointProduct p1 = buildProduct(1L);
        PointProduct p2 = PointProduct.builder()
                .productName("치킨 교환권")
                .pointPrice(10000L)
                .stockQuantity(50)
                .category("음식")
                .build();
        given(pointProductRepository.findAll()).willReturn(List.of(p1, p2));

        List<PointProductResponse> result = pointProductService.getAllProducts();

        assertThat(result).hasSize(2);
        assertThat(result).extracting(PointProductResponse::getProductName)
                .containsExactly("아메리카노 쿠폰", "치킨 교환권");
    }

    @Test
    @DisplayName("상품 수정 성공")
    void updateProduct() {
        PointProduct product = buildProduct(1L);
        given(pointProductRepository.findById(1L)).willReturn(Optional.of(product));

        PointProductUpdateRequest request = PointProductUpdateRequest.builder()
                .productName("수정된 상품")
                .description("수정된 설명")
                .category("음식")
                .imageUrl("https://new.png")
                .pointPrice(5000L)
                .stockQuantity(50)
                .status(PointProductStatus.SOLD_OUT)
                .build();

        PointProductResponse response = pointProductService.updateProduct(1L, request);

        assertThat(response.getProductName()).isEqualTo("수정된 상품");
        assertThat(response.getCategory()).isEqualTo("음식");
        assertThat(response.getPointPrice()).isEqualTo(5000L);
        assertThat(response.getStockQuantity()).isEqualTo(50);
        assertThat(response.getStatus()).isEqualTo(PointProductStatus.SOLD_OUT);
    }

    @Test
    @DisplayName("상품 수정 - 없는 상품 시 예외")
    void updateProduct_notFound() {
        given(pointProductRepository.findById(999L)).willReturn(Optional.empty());

        PointProductUpdateRequest request = PointProductUpdateRequest.builder()
                .productName("수정").pointPrice(1000L).stockQuantity(1)
                .status(PointProductStatus.ON_SALE).build();

        assertThatThrownBy(() -> pointProductService.updateProduct(999L, request))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("상품 삭제 성공")
    void deleteProduct() {
        PointProduct product = buildProduct(1L);
        given(pointProductRepository.findById(1L)).willReturn(Optional.of(product));

        pointProductService.deleteProduct(1L);

        then(pointProductRepository).should().delete(product);
    }

    @Test
    @DisplayName("상품 삭제 - 없는 상품 시 예외")
    void deleteProduct_notFound() {
        given(pointProductRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> pointProductService.deleteProduct(999L))
                .isInstanceOf(NotFoundException.class);
    }
}
