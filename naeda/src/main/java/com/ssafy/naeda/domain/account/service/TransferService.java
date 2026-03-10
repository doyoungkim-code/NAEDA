package com.ssafy.naeda.domain.account.service;

import com.ssafy.naeda.domain.account.dto.request.TransferRequest;
import com.ssafy.naeda.domain.account.dto.response.TransferResponse;
import com.ssafy.naeda.domain.account.repository.AccountRepository;
import com.ssafy.naeda.domain.transaction.entity.TransactionLog;
import com.ssafy.naeda.domain.transaction.entity.TransactionType;
import com.ssafy.naeda.domain.transaction.repository.TransactionLogRepository;
import com.ssafy.naeda.domain.user.entity.User;
import com.ssafy.naeda.domain.user.repository.UserRepository;
import com.ssafy.naeda.global.exception.NotFoundException;
import com.ssafy.naeda.global.ssafy.SsafyApiClient;
import com.ssafy.naeda.global.ssafy.SsafyHeaderFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransferService {

    private static final String TRANSFER_API = "/edu/demandDeposit/updateDemandDepositAccountTransfer";
    private static final String BALANCE_API  = "/edu/demandDeposit/inquireDemandDepositAccountBalance";

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final TransactionLogRepository transactionLogRepository;
    private final SsafyApiClient ssafyApiClient;
    private final SsafyHeaderFactory ssafyHeaderFactory;

    /**
     * BE-005 이체.
     *
     * 1. User 조회 → userKey 획득
     * 2. 출금 계좌 소유권 검증
     * 3. SSAFY 이체 API 호출
     * 4. SSAFY 잔액 조회 API 호출 → balanceAfter 획득
     * 5. TransactionLog 저장 (WITHDRAW)
     * 6. TransferResponse 반환
     */
    @SuppressWarnings("unchecked")
    @Transactional
    public TransferResponse transfer(Long userNo, TransferRequest request) {
        // 1. User 조회
        User user = userRepository.findById(userNo)
                .orElseThrow(() -> new NotFoundException("존재하지 않는 사용자입니다."));

        // 2. 출금 계좌 소유권 검증 (비관적 잠금)
        Long withdrawalAccountId = accountRepository.findByAccountNoForUpdate(request.getWithdrawalAccountNo())
                .filter(account -> account.getUserNo().equals(userNo))
                .orElseThrow(() -> new NotFoundException("계좌를 찾을 수 없거나 접근 권한이 없습니다."))
                .getAccountId();

        // 3. SSAFY 이체 API 호출
        Map<String, Object> transferBody = ssafyApiClient.buildBody(
                ssafyHeaderFactory.create("updateDemandDepositAccountTransfer", user.getUserKey()),
                "depositAccountNo",           request.getDepositAccountNo(),
                "depositTransactionSummary",  "이체",
                "transactionBalance",         request.getAmount().toString(),
                "withdrawalAccountNo",        request.getWithdrawalAccountNo(),
                "withdrawalTransactionSummary", request.getMemo() != null ? request.getMemo() : "이체"
        );
        Map<String, Object> transferResponse = ssafyApiClient.post(TRANSFER_API, transferBody);

        // 4. SSAFY 잔액 조회 → balanceAfter 획득
        long balanceAfter = getBalanceAfter(user.getUserKey(), request.getWithdrawalAccountNo());

        // 5. TransactionLog 저장
        List<Map<String, Object>> recList = (List<Map<String, Object>>) transferResponse.get("REC");
        String ssafyTransactionId = extractWithdrawalTransactionNo(recList);
        if (ssafyTransactionId == null) {
            log.warn("[TransferService] 출금 거래번호를 추출할 수 없습니다. withdrawalAccountNo={}", request.getWithdrawalAccountNo());
        }

        transactionLogRepository.save(TransactionLog.builder()
                .accountId(withdrawalAccountId)
                .transactionType(TransactionType.WITHDRAW)
                .amount(request.getAmount())
                .balanceAfter(balanceAfter)
                .counterpart(request.getDepositAccountNo())
                .memo(request.getMemo())
                .ssafyTransactionId(ssafyTransactionId)
                .build());

        // 6. TransferResponse 반환
        return TransferResponse.builder()
                .withdrawalAccountNo(request.getWithdrawalAccountNo())
                .depositAccountNo(request.getDepositAccountNo())
                .amount(request.getAmount())
                .transactionDate(extractTransactionDate(recList))
                .withdrawalTransactionNo(ssafyTransactionId)
                .depositTransactionNo(extractDepositTransactionNo(recList))
                .build();
    }

    // ── 헬퍼 ──────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private long getBalanceAfter(String userKey, String accountNo) {
        Map<String, Object> balanceBody = ssafyApiClient.buildBody(
                ssafyHeaderFactory.create("inquireDemandDepositAccountBalance", userKey),
                "accountNo", accountNo
        );
        Map<String, Object> balanceResponse = ssafyApiClient.post(BALANCE_API, balanceBody);
        Map<String, Object> rec = (Map<String, Object>) balanceResponse.get("REC");
        if (rec == null) {
            log.warn("[TransferService] 잔액 조회 응답에 REC이 없습니다. accountNo={}", accountNo);
            return 0L;
        }
        String balance = (String) rec.get("accountBalance");
        if (balance == null) {
            log.warn("[TransferService] 잔액 값이 null입니다. accountNo={}", accountNo);
            return 0L;
        }
        try {
            return Long.parseLong(balance);
        } catch (NumberFormatException e) {
            log.warn("[TransferService] 잔액 파싱 실패: balance={}, accountNo={}", balance, accountNo);
            return 0L;
        }
    }

    /**
     * SSAFY 이체 응답 REC 구조:
     * [
     *   { "transactionUniqueNo": "61", "transactionType": "2", ... },  ← 출금
     *   { "transactionUniqueNo": "62", "transactionType": "1", ... }   ← 입금
     * ]
     */
    @SuppressWarnings("unchecked")
    private String extractWithdrawalTransactionNo(List<Map<String, Object>> recList) {
        if (recList == null || recList.isEmpty()) return null;
        return recList.stream()
                .filter(rec -> "2".equals(rec.get("transactionType")))
                .map(rec -> (String) rec.get("transactionUniqueNo"))
                .findFirst()
                .orElse(null);
    }

    @SuppressWarnings("unchecked")
    private String extractDepositTransactionNo(List<Map<String, Object>> recList) {
        if (recList == null || recList.isEmpty()) return null;
        return recList.stream()
                .filter(rec -> "1".equals(rec.get("transactionType")))
                .map(rec -> (String) rec.get("transactionUniqueNo"))
                .findFirst()
                .orElse(null);
    }

    @SuppressWarnings("unchecked")
    private String extractTransactionDate(List<Map<String, Object>> recList) {
        if (recList == null || recList.isEmpty()) return null;
        Map<String, Object> first = recList.get(0);
        if (first == null) return null;
        Object value = first.get("transactionDate");
        return value != null ? value.toString() : null;
    }
}