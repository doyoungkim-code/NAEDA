package com.ssafy.naeda.domain.point.service;

import com.ssafy.naeda.domain.point.dto.request.PointOrderCreateRequest;
import com.ssafy.naeda.domain.point.dto.response.PointOrderResponse;
import com.ssafy.naeda.domain.point.entity.*;
import com.ssafy.naeda.domain.point.repository.PointHistoryRepository;
import com.ssafy.naeda.domain.point.repository.PointOrderRepository;
import com.ssafy.naeda.domain.point.repository.PointProductRepository;
import com.ssafy.naeda.domain.point.repository.PointWalletRepository;
import com.ssafy.naeda.global.exception.InsufficientBalanceException;
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
class PointOrderServiceTest {

    @Mock
    private PointOrderRepository pointOrderRepository;

    @Mock
    private PointProductRepository pointProductRepository;

    @Mock
    private PointWalletRepository pointWalletRepository;

    @Mock
    private PointHistoryRepository pointHistoryRepository;

    @InjectMocks
    private PointOrderService pointOrderService;

    private PointProduct buildAvailableProduct() {
        return PointProduct.builder()
                .productName("아메리카노 쿠폰")
                .description("스타벅스 아메리카노")
                .category("카페")
                .pointPrice(3000L)
                .stockQuantity(10)
                .status(PointProductStatus.ON_SALE)
                .startsAt(LocalDateTime.now().minusDays(1))
                .endsAt(LocalDateTime.now().plusDays(1))
                .build();
    }

    private PointWallet buildWallet(Long balance) {
        return PointWallet.builder()
                .walletId(1L)
                .userNo(1L)
                .balance(balance)
                .totalEarned(balance)
                .totalUsed(0L)
                .build();
    }

    private PointOrderCreateRequest buildRequest(Long productId) {
        return PointOrderCreateRequest.builder()
                .productId(productId)
                .addressId(1L)
                .build();
    }

    @Test
    @DisplayName("구매 성공 - 포인트 차감, 재고 차감, 이력 기록, 주문 생성")
    void purchaseProduct_success() {
        // given
        PointProduct product = buildAvailableProduct();
        PointWallet wallet = buildWallet(10000L);
        PointOrderCreateRequest request = buildRequest(1L);

        given(pointProductRepository.findByIdForUpdate(1L)).willReturn(Optional.of(product));
        given(pointWalletRepository.findByUserNoForUpdate(1L)).willReturn(Optional.of(wallet));
        given(pointOrderRepository.save(any(PointOrder.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        PointOrderResponse response = pointOrderService.purchaseProduct(1L, request);

        // then
        assertThat(response.getProductName()).isEqualTo("아메리카노 쿠폰");
        assertThat(response.getPointPrice()).isEqualTo(3000L);
        assertThat(response.getAddressId()).isEqualTo(1L);

        assertThat(wallet.getBalance()).isEqualTo(7000L);
        assertThat(wallet.getTotalUsed()).isEqualTo(3000L);
        assertThat(product.getStockQuantity()).isEqualTo(9);

        then(pointHistoryRepository).should().save(any(PointHistory.class));
        then(pointOrderRepository).should().save(any(PointOrder.class));
    }

    @Test
    @DisplayName("구매 실패 - 상품 없음")
    void purchaseProduct_productNotFound() {
        // given
        PointOrderCreateRequest request = buildRequest(999L);
        given(pointProductRepository.findByIdForUpdate(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> pointOrderService.purchaseProduct(1L, request))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("구매 실패 - 상품 판매 불가 (SOLD_OUT)")
    void purchaseProduct_notAvailable() {
        // given
        PointProduct soldOutProduct = PointProduct.builder()
                .productName("품절 상품")
                .pointPrice(3000L)
                .stockQuantity(0)
                .status(PointProductStatus.SOLD_OUT)
                .build();
        PointOrderCreateRequest request = buildRequest(1L);

        given(pointProductRepository.findByIdForUpdate(1L)).willReturn(Optional.of(soldOutProduct));

        // when & then
        assertThatThrownBy(() -> pointOrderService.purchaseProduct(1L, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("구매할 수 없는 상품");
    }

    @Test
    @DisplayName("구매 실패 - 포인트 지갑 없음")
    void purchaseProduct_walletNotFound() {
        // given
        PointProduct product = buildAvailableProduct();
        PointOrderCreateRequest request = buildRequest(1L);

        given(pointProductRepository.findByIdForUpdate(1L)).willReturn(Optional.of(product));
        given(pointWalletRepository.findByUserNoForUpdate(1L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> pointOrderService.purchaseProduct(1L, request))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("포인트 지갑");
    }

    @Test
    @DisplayName("구매 실패 - 포인트 잔액 부족")
    void purchaseProduct_insufficientBalance() {
        // given
        PointProduct product = buildAvailableProduct();
        PointWallet wallet = buildWallet(1000L); // 잔액 1000 < 가격 3000
        PointOrderCreateRequest request = buildRequest(1L);

        given(pointProductRepository.findByIdForUpdate(1L)).willReturn(Optional.of(product));
        given(pointWalletRepository.findByUserNoForUpdate(1L)).willReturn(Optional.of(wallet));

        // when & then
        assertThatThrownBy(() -> pointOrderService.purchaseProduct(1L, request))
                .isInstanceOf(InsufficientBalanceException.class);
    }

    @Test
    @DisplayName("구매 실패 - 재고 부족")
    void purchaseProduct_outOfStock() {
        // given
        PointProduct product = PointProduct.builder()
                .productName("재고 없는 상품")
                .pointPrice(3000L)
                .stockQuantity(0)
                .status(PointProductStatus.ON_SALE)
                .build();
        PointWallet wallet = buildWallet(10000L);
        PointOrderCreateRequest request = buildRequest(1L);

        given(pointProductRepository.findByIdForUpdate(1L)).willReturn(Optional.of(product));
        given(pointWalletRepository.findByUserNoForUpdate(1L)).willReturn(Optional.of(wallet));

        // when & then
        assertThatThrownBy(() -> pointOrderService.purchaseProduct(1L, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("재고");
    }

    @Test
    @DisplayName("구매 성공 - 마지막 재고 구매 시 SOLD_OUT 전환")
    void purchaseProduct_lastStock_soldOut() {
        // given
        PointProduct product = PointProduct.builder()
                .productName("마지막 재고")
                .pointPrice(3000L)
                .stockQuantity(1)
                .status(PointProductStatus.ON_SALE)
                .startsAt(LocalDateTime.now().minusDays(1))
                .endsAt(LocalDateTime.now().plusDays(1))
                .build();
        PointWallet wallet = buildWallet(10000L);
        PointOrderCreateRequest request = buildRequest(1L);

        given(pointProductRepository.findByIdForUpdate(1L)).willReturn(Optional.of(product));
        given(pointWalletRepository.findByUserNoForUpdate(1L)).willReturn(Optional.of(wallet));
        given(pointOrderRepository.save(any(PointOrder.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        pointOrderService.purchaseProduct(1L, request);

        // then
        assertThat(product.getStockQuantity()).isEqualTo(0);
        assertThat(product.getStatus()).isEqualTo(PointProductStatus.SOLD_OUT);
    }

    @Test
    @DisplayName("구매 성공 - 주소 없이 주문")
    void purchaseProduct_withoutAddress() {
        // given
        PointProduct product = buildAvailableProduct();
        PointWallet wallet = buildWallet(10000L);
        PointOrderCreateRequest request = PointOrderCreateRequest.builder()
                .productId(1L)
                .build();

        given(pointProductRepository.findByIdForUpdate(1L)).willReturn(Optional.of(product));
        given(pointWalletRepository.findByUserNoForUpdate(1L)).willReturn(Optional.of(wallet));
        given(pointOrderRepository.save(any(PointOrder.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        PointOrderResponse response = pointOrderService.purchaseProduct(1L, request);

        // then
        assertThat(response.getAddressId()).isNull();
    }

    // === getMyOrders 테스트 ===

    @Test
    @DisplayName("내 주문 내역 조회 - 최신순 정렬")
    void getMyOrders_success() {
        // given
        PointOrder order1 = PointOrder.builder()
                .userNo(1L).productId(1L)
                .orderAt(LocalDateTime.now().minusHours(2))
                .addressId(1L).build();
        PointOrder order2 = PointOrder.builder()
                .userNo(1L).productId(2L)
                .orderAt(LocalDateTime.now().minusHours(1))
                .addressId(2L).build();

        PointProduct product1 = PointProduct.builder()
                .productId(1L)
                .productName("아메리카노 쿠폰")
                .pointPrice(3000L)
                .stockQuantity(10)
                .status(PointProductStatus.ON_SALE)
                .build();
        PointProduct product2 = PointProduct.builder()
                .productId(2L)
                .productName("치킨 교환권")
                .pointPrice(15000L)
                .stockQuantity(50)
                .status(PointProductStatus.ON_SALE)
                .build();

        given(pointOrderRepository.findByUserNoOrderByOrderAtDesc(1L)).willReturn(List.of(order1, order2));
        given(pointProductRepository.findAllById(List.of(1L, 2L))).willReturn(List.of(product1, product2));

        // when
        List<PointOrderResponse> result = pointOrderService.getMyOrders(1L);

        // then
        assertThat(result).hasSize(2);
        then(pointOrderRepository).should().findByUserNoOrderByOrderAtDesc(1L);
        then(pointProductRepository).should().findAllById(List.of(1L, 2L));
    }

    @Test
    @DisplayName("내 주문 내역 조회 - 주문 없으면 빈 리스트")
    void getMyOrders_empty() {
        // given
        given(pointOrderRepository.findByUserNoOrderByOrderAtDesc(1L)).willReturn(List.of());

        // when
        List<PointOrderResponse> result = pointOrderService.getMyOrders(1L);

        // then
        assertThat(result).isEmpty();
    }
}
