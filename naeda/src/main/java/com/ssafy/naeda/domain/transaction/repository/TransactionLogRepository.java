package com.ssafy.naeda.domain.transaction.repository;

import com.ssafy.naeda.domain.transaction.entity.TransactionLog;
import com.ssafy.naeda.domain.transaction.entity.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TransactionLogRepository extends JpaRepository<TransactionLog, Long> {

    List<TransactionLog> findByAccountIdOrderByTransactedDesc(Long accountId);

    List<TransactionLog> findByAccountIdAndTransactedBetweenOrderByTransactedDesc(
            Long accountId, LocalDateTime from, LocalDateTime to
    );

    List<TransactionLog> findByAccountIdAndTransactionTypeOrderByTransactedDesc(
            Long accountId, TransactionType transactionType
    );

    boolean existsBySsafyTransactionId(String ssafyTransactionId);

    Optional<TransactionLog> findBySsafyTransactionId(String ssafyTransactionId);

    List<TransactionLog> findBySsafyTransactionIdIn(List<String> ssafyTransactionIds);
}
