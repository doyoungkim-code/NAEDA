//package com.ssafy.naeda.domain.payment.repository;
//
//import com.ssafy.naeda.domain.payment.entity.Payment;
//import com.ssafy.naeda.domain.payment.entity.PaymentStatus;
//import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.data.jpa.repository.Query;
//import org.springframework.data.repository.query.Param;
//
//import java.time.LocalDateTime;
//import java.util.List;
//
//public interface PaymentRepository extends JpaRepository<Payment, Long> {
//
//    List<Payment> findByUserNoOrderByPaidDesc(Long userNo);
//
//    List<Payment> findByUserNoAndPaidBetweenOrderByPaidDesc(Long userNo, LocalDateTime from, LocalDateTime to);
//
//    List<Payment> findByStoreIdOrderByPaidDesc(Long storeId);
//
//    List<Payment> findByUserNoAndStatus(Long userNo, PaymentStatus status);
//
//    boolean existsBySsafyTransactionId(String ssafyTransactionId);
//
//    // FDS: 최근 10분 내 성공 결제 횟수
//    int countByUserNoAndStatusAndPaidAfter(Long userNo, PaymentStatus status, LocalDateTime after);
//
//    // FDS: 최근 30일 일평균 결제 금액
//    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.userNo = :userNo AND p.status = :status AND p.paid >= :since")
//    long sumAmountByUserNoAndStatusAndPaidAfter(@Param("userNo") Long userNo, @Param("status") PaymentStatus status, @Param("since") LocalDateTime since);
//}
