package com.ssafy.naeda.domain.transaction.service;

import com.ssafy.naeda.domain.account.entity.Account;
import com.ssafy.naeda.domain.account.repository.AccountRepository;
import com.ssafy.naeda.domain.transaction.dto.response.TransactionLogResponse;
import com.ssafy.naeda.domain.transaction.entity.TransactionLog;
import com.ssafy.naeda.domain.transaction.entity.TransactionType;
import com.ssafy.naeda.domain.transaction.repository.TransactionLogRepository;
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
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class TransactionLogServiceTest {

    @Mock
    private TransactionLogRepository transactionLogRepository;

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private TransactionLogService transactionLogService;

    private final Long USER_NO = 1L;
    private final Long ACCOUNT_ID = 10L;

    private Account stubAccount() {
        return Account.builder()
                .userNo(USER_NO)
                .bankCode("999").bankName("싸피은행")
                .accountNo("9990000000001234").accountName("테스트계좌")
                .build();
    }

    private TransactionLog stubLog(TransactionType type, Long amount, LocalDateTime transacted) {
        return TransactionLog.builder()
                .accountId(ACCOUNT_ID)
                .transactionType(type)
                .amount(amount)
                .balanceAfter(100_000L)
                .counterpart("홍길동")
                .memo("테스트")
                .category("이체")
                .ssafyTransactionId("TXN001")
                .transacted(transacted)
                .build();
    }

    // ── getTransactions ──────────────────────────────────────────────────

    @Test
    @DisplayName("전체 거래내역 조회 - 최신순 정렬하여 반환한다")
    void getTransactions_returnsSortedDesc() {
        LocalDateTime now = LocalDateTime.now();
        given(accountRepository.findById(ACCOUNT_ID))
                .willReturn(Optional.of(stubAccount()));
        given(transactionLogRepository.findByAccountIdOrderByTransactedDesc(ACCOUNT_ID))
                .willReturn(List.of(
                        stubLog(TransactionType.DEPOSIT, 20_000L, now),
                        stubLog(TransactionType.WITHDRAW, 5_000L, now.minusHours(1)),
                        stubLog(TransactionType.DEPOSIT, 10_000L, now.minusHours(2))
                ));

        List<TransactionLogResponse> result = transactionLogService.getTransactions(USER_NO, ACCOUNT_ID);

        assertThat(result).hasSize(3);
        assertThat(result.get(0).getTransacted()).isAfter(result.get(1).getTransacted());
        assertThat(result.get(1).getTransacted()).isAfter(result.get(2).getTransacted());
    }

    @Test
    @DisplayName("전체 거래내역 조회 - 빈 목록 반환")
    void getTransactions_emptyList() {
        given(accountRepository.findById(ACCOUNT_ID))
                .willReturn(Optional.of(stubAccount()));
        given(transactionLogRepository.findByAccountIdOrderByTransactedDesc(ACCOUNT_ID))
                .willReturn(List.of());

        List<TransactionLogResponse> result = transactionLogService.getTransactions(USER_NO, ACCOUNT_ID);
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("전체 거래내역 조회 - 소유권 검증 실패 시 NotFoundException")
    void getTransactions_ownershipFail() {
        given(accountRepository.findById(ACCOUNT_ID))
                .willReturn(Optional.of(Account.builder()
                        .userNo(999L)
                        .bankCode("999").bankName("싸피은행")
                        .accountNo("9990000000001234").accountName("남의계좌")
                        .build()));

        assertThatThrownBy(() -> transactionLogService.getTransactions(USER_NO, ACCOUNT_ID))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("전체 거래내역 조회 - 계좌가 존재하지 않으면 NotFoundException")
    void getTransactions_accountNotFound() {
        given(accountRepository.findById(ACCOUNT_ID))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> transactionLogService.getTransactions(USER_NO, ACCOUNT_ID))
                .isInstanceOf(NotFoundException.class);
    }

    // ── getTransactionsByPeriod ──────────────────────────────────────────

    @Test
    @DisplayName("기간 필터 조회 - 최신순 정렬하여 반환한다")
    void getTransactionsByPeriod_returnsSortedDesc() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime from = now.minusDays(7);
        LocalDateTime to = now;

        given(accountRepository.findById(ACCOUNT_ID))
                .willReturn(Optional.of(stubAccount()));
        given(transactionLogRepository
                .findByAccountIdAndTransactedBetweenOrderByTransactedDesc(ACCOUNT_ID, from, to))
                .willReturn(List.of(
                        stubLog(TransactionType.WITHDRAW, 5_000L, now.minusDays(1)),
                        stubLog(TransactionType.DEPOSIT, 10_000L, now.minusDays(3))
                ));

        List<TransactionLogResponse> result =
                transactionLogService.getTransactionsByPeriod(USER_NO, ACCOUNT_ID, from, to);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getTransacted()).isAfter(result.get(1).getTransacted());
    }

    @Test
    @DisplayName("기간 필터 조회 - 소유권 검증 실패 시 NotFoundException")
    void getTransactionsByPeriod_ownershipFail() {
        given(accountRepository.findById(ACCOUNT_ID))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> transactionLogService.getTransactionsByPeriod(
                USER_NO, ACCOUNT_ID, LocalDateTime.now().minusDays(7), LocalDateTime.now()))
                .isInstanceOf(NotFoundException.class);
    }

    // ── getTransactionsByType ────────────────────────────────────────────

    @Test
    @DisplayName("거래유형 필터 조회 - DEPOSIT만 반환한다")
    void getTransactionsByType_depositOnly() {
        LocalDateTime now = LocalDateTime.now();
        given(accountRepository.findById(ACCOUNT_ID))
                .willReturn(Optional.of(stubAccount()));
        given(transactionLogRepository
                .findByAccountIdAndTransactionTypeOrderByTransactedDesc(ACCOUNT_ID, TransactionType.DEPOSIT))
                .willReturn(List.of(
                        stubLog(TransactionType.DEPOSIT, 20_000L, now),
                        stubLog(TransactionType.DEPOSIT, 10_000L, now.minusHours(2))
                ));

        List<TransactionLogResponse> result =
                transactionLogService.getTransactionsByType(USER_NO, ACCOUNT_ID, TransactionType.DEPOSIT);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(TransactionLogResponse::getTransactionType)
                .containsOnly(TransactionType.DEPOSIT);
    }

    @Test
    @DisplayName("거래유형 필터 조회 - 소유권 검증 실패 시 NotFoundException")
    void getTransactionsByType_ownershipFail() {
        given(accountRepository.findById(ACCOUNT_ID))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> transactionLogService.getTransactionsByType(
                USER_NO, ACCOUNT_ID, TransactionType.WITHDRAW))
                .isInstanceOf(NotFoundException.class);
    }
}
