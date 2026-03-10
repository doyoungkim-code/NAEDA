package com.ssafy.naeda.domain.payment.service;

import com.ssafy.naeda.domain.payment.dto.PaymentRequestData;
import com.ssafy.naeda.domain.payment.entity.PaymentRequestStatus;
import com.ssafy.naeda.domain.store.entity.Store;
import com.ssafy.naeda.domain.store.repository.StoreRepository;
import com.ssafy.naeda.global.exception.BadRequestException;
import com.ssafy.naeda.global.exception.NotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class PaymentRequestServiceTest {

    @Mock private StoreRepository storeRepository;
    @Mock private PaymentRequestRedisService redisService;

    @InjectMocks
    private PaymentRequestService service;

    private static final Long STORE_ID = 100L;
    private static final Long AMOUNT = 15000L;

    // ── createPaymentRequest ─────────────────────────────────────────────

    @Test
    @DisplayName("결제 요청 생성 - 매장 검증 후 Redis에 저장한다")
    void createPaymentRequest_success() {
        Store store = Store.builder()
                .storeId(STORE_ID)
                .storeName("테스트 매장")
                .facePayEnabled(true)
                .build();

        PaymentRequestData expected = PaymentRequestData.builder()
                .requestId("test-uuid")
                .storeId(STORE_ID)
                .amount(AMOUNT)
                .status(PaymentRequestStatus.PENDING)
                .createdAt(System.currentTimeMillis())
                .updatedAt(System.currentTimeMillis())
                .build();

        given(storeRepository.findById(STORE_ID)).willReturn(Optional.of(store));
        given(redisService.createRequest(STORE_ID, AMOUNT)).willReturn(expected);

        PaymentRequestData result = service.createPaymentRequest(STORE_ID, AMOUNT);

        assertThat(result.getRequestId()).isEqualTo("test-uuid");
        assertThat(result.getStatus()).isEqualTo(PaymentRequestStatus.PENDING);
    }

    @Test
    @DisplayName("결제 요청 생성 - 존재하지 않는 매장이면 NotFoundException")
    void createPaymentRequest_storeNotFound() {
        given(storeRepository.findById(STORE_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.createPaymentRequest(STORE_ID, AMOUNT))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("존재하지 않는 매장입니다");
    }

    @Test
    @DisplayName("결제 요청 생성 - 페이스페이 미지원 매장이면 BadRequestException")
    void createPaymentRequest_facePayDisabled() {
        Store store = Store.builder()
                .storeId(STORE_ID)
                .storeName("테스트 매장")
                .facePayEnabled(false)
                .build();

        given(storeRepository.findById(STORE_ID)).willReturn(Optional.of(store));

        assertThatThrownBy(() -> service.createPaymentRequest(STORE_ID, AMOUNT))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("페이스페이를 지원하지 않습니다");
    }

    // ── getPaymentRequest ────────────────────────────────────────────────

    @Test
    @DisplayName("결제 요청 조회 - 존재하는 요청을 정상 반환한다")
    void getPaymentRequest_success() {
        PaymentRequestData data = PaymentRequestData.builder()
                .requestId("test-uuid")
                .storeId(STORE_ID)
                .amount(AMOUNT)
                .status(PaymentRequestStatus.PENDING)
                .build();

        given(redisService.getRequest("test-uuid")).willReturn(data);

        PaymentRequestData result = service.getPaymentRequest("test-uuid");

        assertThat(result).isNotNull();
        assertThat(result.getRequestId()).isEqualTo("test-uuid");
    }

    @Test
    @DisplayName("결제 요청 조회 - 만료/미존재 시 NotFoundException")
    void getPaymentRequest_notFound() {
        given(redisService.getRequest("expired-uuid")).willReturn(null);

        assertThatThrownBy(() -> service.getPaymentRequest("expired-uuid"))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("결제 요청이 만료되었거나 존재하지 않습니다");
    }

    // ── getPaymentRequestsByStore ────────────────────────────────────────

    @Test
    @DisplayName("매장별 조회 - redisService에 위임한다")
    void getPaymentRequestsByStore_success() {
        PaymentRequestData data1 = PaymentRequestData.builder()
                .requestId("uuid-1").storeId(STORE_ID).amount(10000L)
                .status(PaymentRequestStatus.PENDING).build();
        PaymentRequestData data2 = PaymentRequestData.builder()
                .requestId("uuid-2").storeId(STORE_ID).amount(20000L)
                .status(PaymentRequestStatus.PROCESSING).build();

        given(redisService.getRequestsByStore(STORE_ID)).willReturn(List.of(data1, data2));

        List<PaymentRequestData> result = service.getPaymentRequestsByStore(STORE_ID);

        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("매장별 조회 - 빈 목록을 정상 반환한다")
    void getPaymentRequestsByStore_empty() {
        given(redisService.getRequestsByStore(STORE_ID)).willReturn(List.of());

        List<PaymentRequestData> result = service.getPaymentRequestsByStore(STORE_ID);

        assertThat(result).isEmpty();
    }
}
