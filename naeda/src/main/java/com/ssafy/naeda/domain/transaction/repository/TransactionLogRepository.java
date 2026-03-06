package com.ssafy.naeda.domain.transaction.repository;

import com.ssafy.naeda.domain.transaction.entity.TransactionLog;
import com.ssafy.naeda.domain.transaction.entity.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface TransactionLogRepository extends JpaRepository<TransactionLog, Long> {

    List<TransactionLog> findByAccountId(Long accountId);

    List<TransactionLog> findByAccountIdAndTransactedBetweenOrderByTransacted(
            Long accountId, LocalDateTime from, LocalDateTime to
    );

    List<TransactionLog> findByAccountIdAndTransactionTypeOrderByTransacted(
            Long accountId, TransactionType transactionType
    );
}
