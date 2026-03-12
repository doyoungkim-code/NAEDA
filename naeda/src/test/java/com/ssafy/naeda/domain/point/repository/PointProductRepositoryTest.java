package com.ssafy.naeda.domain.point.repository;

import com.ssafy.naeda.domain.point.entity.PointProduct;
import com.ssafy.naeda.domain.point.entity.PointProductStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PointProductRepositoryTest {

    @Autowired
    private PointProductRepository pointProductRepository;

    @BeforeEach
    void setUp() {
        pointProductRepository.deleteAll();
    }

    private PointProduct buildProduct(String name, String category, PointProductStatus status) {
        return PointProduct.builder()
                .productName(name)
                .description(name + " 설명")
                .category(category)
                .pointPrice(1000L)
                .stockQuantity(10)
                .status(status)
                .build();
    }

    @Test
    @DisplayName("포인트 상품 저장 및 단건 조회")
    void saveAndFindById() {
        PointProduct saved = pointProductRepository.save(
                PointProduct.builder()
                        .productName("아메리카노 쿠폰")
                        .description("스타벅스 아메리카노")
                        .category("카페")
                        .imageUrl("https://example.com/image.png")
                        .pointPrice(3000L)
                        .stockQuantity(100)
                        .startsAt(LocalDateTime.of(2026, 1, 1, 0, 0))
                        .endsAt(LocalDateTime.of(2026, 12, 31, 23, 59))
                        .build()
        );

        PointProduct found = pointProductRepository.findById(saved.getProductId()).orElseThrow();
        assertThat(found.getProductName()).isEqualTo("아메리카노 쿠폰");
        assertThat(found.getDescription()).isEqualTo("스타벅스 아메리카노");
        assertThat(found.getCategory()).isEqualTo("카페");
        assertThat(found.getImageUrl()).isEqualTo("https://example.com/image.png");
        assertThat(found.getPointPrice()).isEqualTo(3000L);
        assertThat(found.getStockQuantity()).isEqualTo(100);
        assertThat(found.getStatus()).isEqualTo(PointProductStatus.ON_SALE);
        assertThat(found.getStartsAt()).isNotNull();
        assertThat(found.getEndsAt()).isNotNull();
    }

    @Test
    @DisplayName("상태별 조회 - ON_SALE만")
    void findByStatus() {
        pointProductRepository.save(buildProduct("상품A", "카페", PointProductStatus.ON_SALE));
        pointProductRepository.save(buildProduct("상품B", "카페", PointProductStatus.ON_SALE));
        pointProductRepository.save(buildProduct("상품C", "음식", PointProductStatus.SOLD_OUT));

        List<PointProduct> onSale = pointProductRepository.findByStatus(PointProductStatus.ON_SALE);
        assertThat(onSale).hasSize(2);
        assertThat(onSale).extracting(PointProduct::getStatus)
                .containsOnly(PointProductStatus.ON_SALE);

        List<PointProduct> soldOut = pointProductRepository.findByStatus(PointProductStatus.SOLD_OUT);
        assertThat(soldOut).hasSize(1);
        assertThat(soldOut.get(0).getProductName()).isEqualTo("상품C");
    }

    @Test
    @DisplayName("카테고리별 조회")
    void findByCategory() {
        pointProductRepository.save(buildProduct("상품A", "카페", PointProductStatus.ON_SALE));
        pointProductRepository.save(buildProduct("상품B", "음식", PointProductStatus.ON_SALE));
        pointProductRepository.save(buildProduct("상품C", "카페", PointProductStatus.SOLD_OUT));

        List<PointProduct> cafeProducts = pointProductRepository.findByCategory("카페");
        assertThat(cafeProducts).hasSize(2);
        assertThat(cafeProducts).extracting(PointProduct::getCategory).containsOnly("카페");
    }

    @Test
    @DisplayName("카테고리 + 상태 조합 조회")
    void findByCategoryAndStatus() {
        pointProductRepository.save(buildProduct("상품A", "카페", PointProductStatus.ON_SALE));
        pointProductRepository.save(buildProduct("상품B", "카페", PointProductStatus.SOLD_OUT));
        pointProductRepository.save(buildProduct("상품C", "음식", PointProductStatus.ON_SALE));

        List<PointProduct> result = pointProductRepository.findByCategoryAndStatus("카페", PointProductStatus.ON_SALE);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getProductName()).isEqualTo("상품A");
    }

    @Test
    @DisplayName("상품명 부분 검색")
    void findByProductNameContaining() {
        pointProductRepository.save(buildProduct("아메리카노 쿠폰", "카페", PointProductStatus.ON_SALE));
        pointProductRepository.save(buildProduct("카페라떼 쿠폰", "카페", PointProductStatus.ON_SALE));
        pointProductRepository.save(buildProduct("치킨 교환권", "음식", PointProductStatus.ON_SALE));

        List<PointProduct> result = pointProductRepository.findByProductNameContaining("쿠폰");
        assertThat(result).hasSize(2);
        assertThat(result).extracting(PointProduct::getProductName)
                .containsExactlyInAnyOrder("아메리카노 쿠폰", "카페라떼 쿠폰");

        List<PointProduct> noResult = pointProductRepository.findByProductNameContaining("없는상품");
        assertThat(noResult).isEmpty();
    }

    @Test
    @DisplayName("상품명 검색 + ON_SALE 상태 조합")
    void findByProductNameContainingAndStatus() {
        pointProductRepository.save(buildProduct("아메리카노 쿠폰", "카페", PointProductStatus.ON_SALE));
        pointProductRepository.save(buildProduct("카페라떼 쿠폰", "카페", PointProductStatus.SOLD_OUT));
        pointProductRepository.save(buildProduct("치킨 교환권", "음식", PointProductStatus.ON_SALE));

        List<PointProduct> result = pointProductRepository.findByProductNameContainingAndStatus("쿠폰", PointProductStatus.ON_SALE);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getProductName()).isEqualTo("아메리카노 쿠폰");
    }

    @Test
    @DisplayName("상품명 검색 + 카테고리 + ON_SALE 상태 조합")
    void findByProductNameContainingAndCategoryAndStatus() {
        pointProductRepository.save(buildProduct("아메리카노 쿠폰", "카페", PointProductStatus.ON_SALE));
        pointProductRepository.save(buildProduct("카페라떼 쿠폰", "카페", PointProductStatus.ON_SALE));
        pointProductRepository.save(buildProduct("치킨 쿠폰", "음식", PointProductStatus.ON_SALE));

        List<PointProduct> result = pointProductRepository.findByProductNameContainingAndCategoryAndStatus("쿠폰", "카페", PointProductStatus.ON_SALE);
        assertThat(result).hasSize(2);
        assertThat(result).extracting(PointProduct::getCategory).containsOnly("카페");

        List<PointProduct> foodResult = pointProductRepository.findByProductNameContainingAndCategoryAndStatus("쿠폰", "음식", PointProductStatus.ON_SALE);
        assertThat(foodResult).hasSize(1);
        assertThat(foodResult.get(0).getProductName()).isEqualTo("치킨 쿠폰");
    }

    @Test
    @DisplayName("상품 수정 (update 메서드)")
    void updateProduct() {
        PointProduct saved = pointProductRepository.save(buildProduct("원래 상품", "카페", PointProductStatus.ON_SALE));

        saved.update("수정된 상품", "수정된 설명", "음식", "https://new.png",
                5000L, 50, PointProductStatus.SOLD_OUT, null, null);
        pointProductRepository.save(saved);

        PointProduct found = pointProductRepository.findById(saved.getProductId()).orElseThrow();
        assertThat(found.getProductName()).isEqualTo("수정된 상품");
        assertThat(found.getDescription()).isEqualTo("수정된 설명");
        assertThat(found.getCategory()).isEqualTo("음식");
        assertThat(found.getPointPrice()).isEqualTo(5000L);
        assertThat(found.getStockQuantity()).isEqualTo(50);
        assertThat(found.getStatus()).isEqualTo(PointProductStatus.SOLD_OUT);
    }

    @Test
    @DisplayName("상품 삭제")
    void deleteProduct() {
        PointProduct saved = pointProductRepository.save(buildProduct("삭제 대상", "카페", PointProductStatus.ON_SALE));
        Long id = saved.getProductId();

        pointProductRepository.deleteById(id);

        assertThat(pointProductRepository.findById(id)).isEmpty();
    }

    @Test
    @DisplayName("isAvailable - 판매중 + 기간 내")
    void isAvailable_true() {
        PointProduct product = PointProduct.builder()
                .productName("판매중 상품")
                .pointPrice(1000L)
                .stockQuantity(10)
                .status(PointProductStatus.ON_SALE)
                .startsAt(LocalDateTime.now().minusDays(1))
                .endsAt(LocalDateTime.now().plusDays(1))
                .build();

        assertThat(product.isAvailable()).isTrue();
    }

    @Test
    @DisplayName("isAvailable - SOLD_OUT이면 false")
    void isAvailable_soldOut() {
        PointProduct product = PointProduct.builder()
                .productName("품절 상품")
                .pointPrice(1000L)
                .stockQuantity(0)
                .status(PointProductStatus.SOLD_OUT)
                .build();

        assertThat(product.isAvailable()).isFalse();
    }

    @Test
    @DisplayName("isAvailable - 판매 시작 전이면 false")
    void isAvailable_beforeStart() {
        PointProduct product = PointProduct.builder()
                .productName("예정 상품")
                .pointPrice(1000L)
                .stockQuantity(10)
                .status(PointProductStatus.ON_SALE)
                .startsAt(LocalDateTime.now().plusDays(1))
                .build();

        assertThat(product.isAvailable()).isFalse();
    }

    @Test
    @DisplayName("isAvailable - 판매 종료 후면 false")
    void isAvailable_afterEnd() {
        PointProduct product = PointProduct.builder()
                .productName("종료 상품")
                .pointPrice(1000L)
                .stockQuantity(10)
                .status(PointProductStatus.ON_SALE)
                .endsAt(LocalDateTime.now().minusDays(1))
                .build();

        assertThat(product.isAvailable()).isFalse();
    }
}
