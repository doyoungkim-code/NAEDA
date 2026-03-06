package com.ssafy.naeda.domain.transaction.service;

import com.ssafy.naeda.domain.account.repository.AccountRepository;
import com.ssafy.naeda.domain.transaction.dto.response.TransactionLogResponse;
import com.ssafy.naeda.domain.transaction.entity.TransactionLog;
import com.ssafy.naeda.domain.transaction.entity.TransactionType;
import com.ssafy.naeda.domain.transaction.repository.TransactionLogRepository;
import com.ssafy.naeda.global.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TransactionLogService {

    private final TransactionLogRepository transactionLogRepository;
    private final AccountRepository accountRepository;

    /**
     * 전체 거래내역 조회 (최신순)
     */
    @Transactional(readOnly = true)
    public List<TransactionLogResponse> getTransactions(Long userNo, Long accountId) {
        validateOwnership(userNo, accountId);

        return transactionLogRepository.findByAccountId(accountId)
                .stream()
                .sorted(Comparator.comparing(TransactionLog::getTransacted).reversed())
                .map(TransactionLogResponse::from)
                .toList();
    }

    /**
     * 기간 필터 거래내역 조회 (최신순)
     */
    @Transactional(readOnly = true)
    public List<TransactionLogResponse> getTransactionsByPeriod (
            Long userNo, Long accountId, LocalDateTime from, LocalDateTime to
    ) {
        validateOwnership(userNo, accountId);

        return transactionLogRepository.findByAccountIdAndTransactedBetweenOrderByTransacted(accountId, from, to)
                .stream()
                .sorted(Comparator.comparing(TransactionLog::getTransacted).reversed())
                .map(TransactionLogResponse::from)
                .toList();
    }

    /**
     * 거래유형 필터 거래내역 조회 (최신순)
     */
    @Transactional(readOnly = true)
    public List<TransactionLogResponse> getTransactionsByType(
            Long userNo, Long accountId, TransactionType transactionType
    ) {
        validateOwnership(userNo, accountId);

        return transactionLogRepository
                .findByAccountIdAndTransactionTypeOrderByTransacted(accountId, transactionType)
                .stream()
                .sorted(Comparator.comparing(TransactionLog::getTransacted).reversed())
                .map(TransactionLogResponse::from)
                .toList();
    }

    // ── 소유권 검증 ────────────────────────────────────────────────────────
    private void validateOwnership(Long userNo, Long accountId) {
        accountRepository.findById(accountId)
                .filter(account -> account.getUserNo().equals(userNo))
                .orElseThrow(() -> new NotFoundException("계좌를 찾을 수 없거나 접근할 수 없습니다."));
    }
}
