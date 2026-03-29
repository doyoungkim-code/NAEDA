package com.ssafy.naeda.domain.transaction.repository;

import com.ssafy.naeda.domain.transaction.entity.TransactionLog;
import com.ssafy.naeda.domain.transaction.entity.TransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class TransactionLogRepositoryTest {

    @Autowired
    private TransactionLogRepository transactionLogRepository;

    @Autowired
    private TestEntityManager em;

    @BeforeEach
    void setUp() {
        transactionLogRepository.deleteAll();
    }

    @Test
    @DisplayName("거래내역 저장 및 단건 조회")
    void saveAndFindById() {
        TransactionLog log = transactionLogRepository.save(
                TransactionLog.builder()
                        .accountId(1L)
                        .transactionType(TransactionType.DEPOSIT)
                        .amount(100_000L)
                        .balanceAfter(500_000L)
                        .counterpart("홍길동")
                        .memo("용돈")
                        .category("이체")
                        .ssafyTransactionId("TXN001")
                        .build()
        );

        TransactionLog found = transactionLogRepository.findById(log.getLogId()).orElseThrow();
        assertThat(found.getAccountId()).isEqualTo(1L);
        assertThat(found.getTransactionType()).isEqualTo(TransactionType.DEPOSIT);
        assertThat(found.getAmount()).isEqualTo(100_000L);
        assertThat(found.getBalanceAfter()).isEqualTo(500_000L);
        assertThat(found.getCounterpart()).isEqualTo("홍길동");
        assertThat(found.getTransacted()).isNotNull();
    }

    @Test
    @DisplayName("accountId로 거래내역 목록 조회")
    void findByAccountId() {
        transactionLogRepository.save(TransactionLog.builder()
                .accountId(1L).transactionType(TransactionType.DEPOSIT)
                .amount(50_000L).balanceAfter(50_000L).build());
        transactionLogRepository.save(TransactionLog.builder()
                .accountId(1L).transactionType(TransactionType.WITHDRAW)
                .amount(10_000L).balanceAfter(40_000L).build());
        transactionLogRepository.save(TransactionLog.builder()
                .accountId(2L).transactionType(TransactionType.DEPOSIT)
                .amount(30_000L).balanceAfter(30_000L).build());

        List<TransactionLog> logs = transactionLogRepository.findByAccountIdOrderByTransactedDesc(1L);
        assertThat(logs).hasSize(2);
        assertThat(logs).extracting(TransactionLog::getAccountId).containsOnly(1L);
    }

    @Test
    @DisplayName("accountId + 기간으로 거래내역 조회")
    void findByAccountIdAndTransactedBetween() {
        LocalDateTime now = LocalDateTime.now();

        TransactionLog log1 = TransactionLog.builder()
                .accountId(1L).transactionType(TransactionType.DEPOSIT)
                .amount(10_000L).balanceAfter(10_000L).build();
        TransactionLog log2 = TransactionLog.builder()
                .accountId(1L).transactionType(TransactionType.WITHDRAW)
                .amount(5_000L).balanceAfter(5_000L).build();

        transactionLogRepository.save(log1);
        transactionLogRepository.save(log2);
        em.flush();

        List<TransactionLog> logs = transactionLogRepository
                .findByAccountIdAndTransactedBetweenOrderByTransactedDesc(
                        1L, now.minusMinutes(1), now.plusMinutes(1)
                );
        assertThat(logs).hasSize(2);

        List<TransactionLog> empty = transactionLogRepository
                .findByAccountIdAndTransactedBetweenOrderByTransactedDesc(
                        1L, now.minusDays(10), now.minusDays(9)
                );
        assertThat(empty).isEmpty();
    }

    @Test
    @DisplayName("accountId + 거래유형으로 거래내역 조회")
    void findByAccountIdAndTransactionType() {
        transactionLogRepository.save(TransactionLog.builder()
                .accountId(1L).transactionType(TransactionType.DEPOSIT)
                .amount(50_000L).balanceAfter(50_000L).build());
        transactionLogRepository.save(TransactionLog.builder()
                .accountId(1L).transactionType(TransactionType.WITHDRAW)
                .amount(10_000L).balanceAfter(40_000L).build());
        transactionLogRepository.save(TransactionLog.builder()
                .accountId(1L).transactionType(TransactionType.DEPOSIT)
                .amount(20_000L).balanceAfter(60_000L).build());

        List<TransactionLog> deposits = transactionLogRepository
                .findByAccountIdAndTransactionTypeOrderByTransactedDesc(1L, TransactionType.DEPOSIT);
        assertThat(deposits).hasSize(2);
        assertThat(deposits).extracting(TransactionLog::getTransactionType)
                .containsOnly(TransactionType.DEPOSIT);

        List<TransactionLog> withdrawals = transactionLogRepository
                .findByAccountIdAndTransactionTypeOrderByTransactedDesc(1L, TransactionType.WITHDRAW);
        assertThat(withdrawals).hasSize(1);
    }
}
