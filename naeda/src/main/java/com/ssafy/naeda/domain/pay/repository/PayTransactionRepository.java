package com.ssafy.naeda.domain.pay.repository;

import com.ssafy.naeda.domain.pay.entity.PayStatus;
import com.ssafy.naeda.domain.pay.entity.PayTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PayTransactionRepository extends JpaRepository<PayTransaction, Long> {

    List<PayTransaction> findByUserNoOrderByCreatedAtDesc(Long userNo);

    List<PayTransaction> findByUserNoAndCreatedAtBetweenOrderByCreatedAtDesc(
            Long userNo, LocalDateTime from, LocalDateTime to);

    List<PayTransaction> findByStoreIdOrderByCreatedAtDesc(Long storeId);

    Optional<PayTransaction> findByIdempotencyKey(String idempotencyKey);

    boolean existsByIdempotencyKey(String idempotencyKey);

    boolean existsBySsafyTransactionId(String ssafyTransactionId);

    // FDS 메트릭: 최근 N분 결제 건수
    int countByUserNoAndStatusAndCreatedAtAfter(Long userNo, PayStatus status, LocalDateTime after);

    // FDS 메트릭: 최근 N일 결제 금액 합계
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM PayTransaction t WHERE t.userNo = :userNo AND t.status = :status AND t.createdAt > :since")
    long sumAmountByUserNoAndStatusAndCreatedAtAfter(
            @Param("userNo") Long userNo,
            @Param("status") PayStatus status,
            @Param("since") LocalDateTime since);
}
