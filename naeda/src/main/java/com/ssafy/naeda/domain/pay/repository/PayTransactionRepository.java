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

    // ── 추천 API 용: 가게별 결제(방문) 횟수 ──

    @Query("select p.storeId, count(p) from PayTransaction p where p.status = 'SUCCESS' group by p.storeId")
    List<Object[]> countVisitsByStore();

    boolean existsBySsafyTransactionId(String ssafyTransactionId);

    // FDS 메트릭: 최근 N분 결제 건수
    int countByUserNoAndStatusAndCreatedAtAfter(Long userNo, PayStatus status, LocalDateTime after);

    // FDS 메트릭: 최근 N일 결제 금액 합계
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM PayTransaction t WHERE t.userNo = :userNo AND t.status = :status AND t.createdAt > :since")
    long sumAmountByUserNoAndStatusAndCreatedAtAfter(
            @Param("userNo") Long userNo,
            @Param("status") PayStatus status,
            @Param("since") LocalDateTime since);

    void deleteByUserNo(Long userNo);

    /**
     * 특정 카드번호로 결제된 트랜잭션의 ssafyTransactionId 목록 조회.
     * pay_transaction → payment_method → credit_card/debit_card 를 조인하여
     * 해당 카드로 결제한 건만 필터링한다.
     */
    @Query("""
        SELECT pt.ssafyTransactionId FROM PayTransaction pt
        WHERE pt.ssafyTransactionId IS NOT NULL
          AND pt.status = 'SUCCESS'
          AND pt.paymentMethodId IN (
              SELECT pm.paymentMethodId FROM PayMethod pm
              WHERE pm.creditCardId IN (SELECT cc.creditCardId FROM CreditCard cc WHERE cc.cardNo = :cardNo)
                 OR pm.debitCardId IN (SELECT dc.debitCardId FROM DebitCard dc WHERE dc.cardNo = :cardNo)
          )
        """)
    List<String> findSsafyTransactionIdsByCardNo(@Param("cardNo") String cardNo);
}
