package com.ssafy.naeda.domain.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.naeda.domain.payment.dto.PaymentRequestData;
import com.ssafy.naeda.domain.payment.entity.PaymentRequestStatus;
import com.ssafy.naeda.global.exception.BadRequestException;
import com.ssafy.naeda.global.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ValueOperations;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentRequestRedisServiceTest {

    @Mock private RedisTemplate<String, String> redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;
    @Mock private SetOperations<String, String> setOperations;

    @InjectMocks
    private PaymentRequestRedisService service;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final Long STORE_ID = 100L;
    private static final Long AMOUNT = 15000L;

    @BeforeEach
    void setUp() throws Exception {
        setField(service, "ttlSeconds", 30L);
        setField(service, "processTtlSeconds", 300L);
        setField(service, "objectMapper", objectMapper);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(redisTemplate.opsForSet()).thenReturn(setOperations);
    }

    private void setField(Object target, String name, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(target, value);
    }

    // ── createRequest ─────────────────────────────────────────────────────

    @Test
    @DisplayName("결제 요청 생성 - PENDING 상태로 Redis에 저장하고 매장 인덱스에 추가한다")
    void createRequest_success() {
        PaymentRequestData result = service.createRequest(STORE_ID, AMOUNT);

        assertThat(result.getRequestId()).isNotNull();
        assertThat(result.getStoreId()).isEqualTo(STORE_ID);
        assertThat(result.getAmount()).isEqualTo(AMOUNT);
        assertThat(result.getStatus()).isEqualTo(PaymentRequestStatus.PENDING);
        assertThat(result.getCreatedAt()).isPositive();

        verify(valueOperations).set(
                eq("payment:request:" + result.getRequestId()),
                anyString(),
                eq(30L),
                eq(TimeUnit.SECONDS)
        );
        verify(setOperations).add(
                eq("payment:store:" + STORE_ID + ":requests"),
                eq(result.getRequestId())
        );
    }

    // ── getRequest ────────────────────────────────────────────────────────

    @Test
    @DisplayName("결제 요청 조회 - 존재하는 요청을 정상 반환한다")
    void getRequest_found() throws Exception {
        PaymentRequestData data = PaymentRequestData.builder()
                .requestId("test-uuid")
                .storeId(STORE_ID)
                .amount(AMOUNT)
                .status(PaymentRequestStatus.PENDING)
                .createdAt(System.currentTimeMillis())
                .updatedAt(System.currentTimeMillis())
                .build();

        given(valueOperations.get("payment:request:test-uuid"))
                .willReturn(objectMapper.writeValueAsString(data));

        PaymentRequestData result = service.getRequest("test-uuid");

        assertThat(result).isNotNull();
        assertThat(result.getRequestId()).isEqualTo("test-uuid");
        assertThat(result.getStatus()).isEqualTo(PaymentRequestStatus.PENDING);
    }

    @Test
    @DisplayName("결제 요청 조회 - 만료된 요청은 null을 반환한다")
    void getRequest_expired_returnsNull() {
        given(valueOperations.get("payment:request:expired-uuid")).willReturn(null);

        PaymentRequestData result = service.getRequest("expired-uuid");

        assertThat(result).isNull();
    }

    // ── updateStatus ──────────────────────────────────────────────────────

    @Test
    @DisplayName("상태 변경 - PENDING → PROCESSING 정상 전환")
    void updateStatus_pendingToProcessing() throws Exception {
        PaymentRequestData data = PaymentRequestData.builder()
                .requestId("test-uuid")
                .storeId(STORE_ID)
                .amount(AMOUNT)
                .status(PaymentRequestStatus.PENDING)
                .createdAt(System.currentTimeMillis())
                .updatedAt(System.currentTimeMillis())
                .build();

        given(valueOperations.get("payment:request:test-uuid"))
                .willReturn(objectMapper.writeValueAsString(data));

        PaymentRequestData result = service.updateStatus("test-uuid", PaymentRequestStatus.PROCESSING);

        assertThat(result.getStatus()).isEqualTo(PaymentRequestStatus.PROCESSING);
        verify(valueOperations).set(
                eq("payment:request:test-uuid"),
                anyString(),
                eq(300L),
                eq(TimeUnit.SECONDS)
        );
    }

    @Test
    @DisplayName("상태 변경 - PROCESSING → SUCCESS 정상 전환")
    void updateStatus_processingToSuccess() throws Exception {
        PaymentRequestData data = PaymentRequestData.builder()
                .requestId("test-uuid")
                .storeId(STORE_ID)
                .amount(AMOUNT)
                .status(PaymentRequestStatus.PROCESSING)
                .createdAt(System.currentTimeMillis())
                .updatedAt(System.currentTimeMillis())
                .build();

        given(valueOperations.get("payment:request:test-uuid"))
                .willReturn(objectMapper.writeValueAsString(data));

        PaymentRequestData result = service.updateStatus("test-uuid", PaymentRequestStatus.SUCCESS);

        assertThat(result.getStatus()).isEqualTo(PaymentRequestStatus.SUCCESS);
    }

    @Test
    @DisplayName("상태 변경 - 잘못된 전환(PENDING → SUCCESS)이면 BadRequestException")
    void updateStatus_invalidTransition_throwsBadRequest() throws Exception {
        PaymentRequestData data = PaymentRequestData.builder()
                .requestId("test-uuid")
                .storeId(STORE_ID)
                .amount(AMOUNT)
                .status(PaymentRequestStatus.PENDING)
                .createdAt(System.currentTimeMillis())
                .updatedAt(System.currentTimeMillis())
                .build();

        given(valueOperations.get("payment:request:test-uuid"))
                .willReturn(objectMapper.writeValueAsString(data));

        assertThatThrownBy(() -> service.updateStatus("test-uuid", PaymentRequestStatus.SUCCESS))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("잘못된 상태 전환입니다");
    }

    @Test
    @DisplayName("상태 변경 - 만료된 요청이면 NotFoundException")
    void updateStatus_notFound_throwsNotFound() {
        given(valueOperations.get("payment:request:expired-uuid")).willReturn(null);

        assertThatThrownBy(() -> service.updateStatus("expired-uuid", PaymentRequestStatus.PROCESSING))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("결제 요청이 만료되었거나 존재하지 않습니다");
    }

    // ── updateResult ──────────────────────────────────────────────────────

    @Test
    @DisplayName("결과 반영 - userNo, nextAction, paymentId가 업데이트된다")
    void updateResult_setsFieldsAndRefreshesTTL() throws Exception {
        PaymentRequestData data = PaymentRequestData.builder()
                .requestId("test-uuid")
                .storeId(STORE_ID)
                .amount(AMOUNT)
                .status(PaymentRequestStatus.PROCESSING)
                .createdAt(System.currentTimeMillis())
                .updatedAt(System.currentTimeMillis())
                .build();

        given(valueOperations.get("payment:request:test-uuid"))
                .willReturn(objectMapper.writeValueAsString(data));

        PaymentRequestData result = service.updateResult("test-uuid", 10L, "PASS", 42L, null);

        assertThat(result.getUserNo()).isEqualTo(10L);
        assertThat(result.getNextAction()).isEqualTo("PASS");
        assertThat(result.getPaymentId()).isEqualTo(42L);
        assertThat(result.getFailureReason()).isNull();

        ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);
        verify(valueOperations).set(eq("payment:request:test-uuid"), jsonCaptor.capture(), eq(300L), eq(TimeUnit.SECONDS));

        PaymentRequestData saved = objectMapper.readValue(jsonCaptor.getValue(), PaymentRequestData.class);
        assertThat(saved.getUserNo()).isEqualTo(10L);
        assertThat(saved.getNextAction()).isEqualTo("PASS");
    }

    @Test
    @DisplayName("결과 반영 - 만료된 요청이면 NotFoundException")
    void updateResult_notFound_throwsNotFound() {
        given(valueOperations.get("payment:request:expired-uuid")).willReturn(null);

        assertThatThrownBy(() -> service.updateResult("expired-uuid", 10L, "PASS", null, null))
                .isInstanceOf(NotFoundException.class);
    }

    // ── getRequestsByStore ────────────────────────────────────────────────

    @Test
    @DisplayName("매장별 조회 - 만료된 요청은 인덱스에서 제거하고 유효한 건만 반환한다")
    void getRequestsByStore_filtersExpired() throws Exception {
        PaymentRequestData validData = PaymentRequestData.builder()
                .requestId("valid-uuid")
                .storeId(STORE_ID)
                .amount(AMOUNT)
                .status(PaymentRequestStatus.PENDING)
                .createdAt(System.currentTimeMillis())
                .updatedAt(System.currentTimeMillis())
                .build();

        given(setOperations.members("payment:store:" + STORE_ID + ":requests"))
                .willReturn(Set.of("valid-uuid", "expired-uuid"));
        given(valueOperations.get("payment:request:valid-uuid"))
                .willReturn(objectMapper.writeValueAsString(validData));
        given(valueOperations.get("payment:request:expired-uuid"))
                .willReturn(null);

        List<PaymentRequestData> result = service.getRequestsByStore(STORE_ID);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getRequestId()).isEqualTo("valid-uuid");
        verify(setOperations).remove("payment:store:" + STORE_ID + ":requests", "expired-uuid");
    }

    // ── tryAcquireProcessingLock / releaseProcessingLock ───────────────────

    @Test
    @DisplayName("분산 락 획득 - SETNX 성공 시 true를 반환한다")
    void tryAcquireProcessingLock_success() {
        given(valueOperations.setIfAbsent("payment:lock:test-uuid", "1", 300L, TimeUnit.SECONDS))
                .willReturn(true);

        assertThat(service.tryAcquireProcessingLock("test-uuid")).isTrue();
    }

    @Test
    @DisplayName("분산 락 획득 - 이미 락이 존재하면 false를 반환한다")
    void tryAcquireProcessingLock_alreadyLocked() {
        given(valueOperations.setIfAbsent("payment:lock:test-uuid", "1", 300L, TimeUnit.SECONDS))
                .willReturn(false);

        assertThat(service.tryAcquireProcessingLock("test-uuid")).isFalse();
    }

    @Test
    @DisplayName("분산 락 해제 - Redis에서 락 키를 삭제한다")
    void releaseProcessingLock_deletesKey() {
        service.releaseProcessingLock("test-uuid");

        verify(redisTemplate).delete("payment:lock:test-uuid");
    }

    // ── deleteRequest ─────────────────────────────────────────────────────

    @Test
    @DisplayName("결제 요청 삭제 - Redis 키와 매장 인덱스에서 모두 제거한다")
    void deleteRequest_removesFromRedisAndIndex() throws Exception {
        PaymentRequestData data = PaymentRequestData.builder()
                .requestId("test-uuid")
                .storeId(STORE_ID)
                .amount(AMOUNT)
                .status(PaymentRequestStatus.SUCCESS)
                .createdAt(System.currentTimeMillis())
                .updatedAt(System.currentTimeMillis())
                .build();

        given(valueOperations.get("payment:request:test-uuid"))
                .willReturn(objectMapper.writeValueAsString(data));
        given(redisTemplate.delete("payment:request:test-uuid")).willReturn(true);

        service.deleteRequest("test-uuid");

        verify(redisTemplate).delete("payment:request:test-uuid");
        verify(setOperations).remove("payment:store:" + STORE_ID + ":requests", "test-uuid");
    }
}
