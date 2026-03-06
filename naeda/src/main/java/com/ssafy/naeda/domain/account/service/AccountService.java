package com.ssafy.naeda.domain.account.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.naeda.domain.account.dto.response.AccountResponse;
import com.ssafy.naeda.domain.account.dto.ssafy.SsafyAccountListResponse;
import com.ssafy.naeda.domain.account.dto.ssafy.SsafyAccountListResponse.SsafyAccountRec;
import com.ssafy.naeda.domain.account.entity.Account;
import com.ssafy.naeda.domain.account.repository.AccountRepository;
import com.ssafy.naeda.domain.user.entity.User;
import com.ssafy.naeda.domain.user.repository.UserRepository;
import com.ssafy.naeda.global.exception.NotFoundException;
import com.ssafy.naeda.global.ssafy.SsafyApiClient;
import com.ssafy.naeda.global.ssafy.SsafyHeaderFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AccountService {

    private static final String ACCOUNT_LIST_API  = "/edu/demandDeposit/inquireDemandDepositAccountList";
    private static final String ACCOUNT_DETAIL_API = "/edu/demandDeposit/inquireDemandDepositAccount";

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final SsafyApiClient ssafyApiClient;
    private final SsafyHeaderFactory ssafyHeaderFactory;
    private final ObjectMapper objectMapper;

    /**
     * BE-004 계좌 목록 조회.
     *
     * 흐름:
     * 1. userNo로 User 조회 → userKey 획득
     * 2. SSAFY API 호출 → 실시간 계좌 목록 + 잔액
     * 3. 우리 DB 계좌 목록 조회
     * 4. accountNo 기준으로 매핑 → AccountResponse 리스트 반환
     *
     * SSAFY가 원본이므로 SSAFY에 있는 계좌가 기준.
     * 우리 DB에 없는 계좌(신규 등록 등)도 응답에 포함시킨다.
     */
    @Transactional(readOnly = true)
    public List<AccountResponse> getAccounts(Long userNo) {
        User user = userRepository.findById(userNo)
                .orElseThrow(() -> new NotFoundException("존재하지 않는 사용자입니다."));

        // 1. SSAFY API 호출
        Map<String, Object> header = ssafyHeaderFactory.create(
                "inquireDemandDepositAccountList", user.getUserKey()
        );
        Map<String, Object> body = ssafyApiClient.buildBody(header);
        Map<String, Object> response = ssafyApiClient.post(ACCOUNT_LIST_API, body);

        List<SsafyAccountRec> ssafyAccounts = parseSsafyAccountList(response);

        // 2. 우리 DB 계좌 목록 → accountNo로 빠르게 찾기 위해 Map으로 변환
        Map<String, Account> dbAccountMap = accountRepository.findByUserNo(userNo)
                .stream()
                .collect(Collectors.toMap(Account::getAccountNo, a -> a));

        // 3. SSAFY 계좌 기준으로 매핑
        List<AccountResponse> result = new ArrayList<>();
        for (SsafyAccountRec rec : ssafyAccounts) {
            Account dbAccount = dbAccountMap.get(rec.getAccountNo());
            if (dbAccount != null) {
                result.add(AccountResponse.of(dbAccount, rec));
            } else {
                // SSAFY에는 있지만 우리 DB에 없는 경우 (accountId null)
                result.add(AccountResponse.ofSsafy(rec));
            }
        }

        return result;
    }

    /**
     * BE-004 계좌 단건 조회.
     */
    @Transactional(readOnly = true)
    public AccountResponse getAccount(Long userNo, String accountNo) {
        User user = userRepository.findById(userNo)
                .orElseThrow(() -> new NotFoundException("존재하지 않는 사용자입니다."));

        // 1. SSAFY API 호출
        Map<String, Object> header = ssafyHeaderFactory.create(
                "inquireDemandDepositAccount", user.getUserKey()
        );
        Map<String, Object> body = ssafyApiClient.buildBody(header, "accountNo", accountNo);
        Map<String, Object> response = ssafyApiClient.post(ACCOUNT_DETAIL_API, body);

        SsafyAccountRec rec = parseSsafyAccountRec(response);

        // 2. 우리 DB에서 조회
        return accountRepository.findByAccountNo(accountNo)
                .map(account -> AccountResponse.of(account, rec))
                .orElse(AccountResponse.ofSsafy(rec));
    }

    // ── 파싱 헬퍼 ──────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private List<SsafyAccountRec> parseSsafyAccountList(Map<String, Object> response) {
        Object rec = response.get("REC");
        if (rec == null) return List.of();
        return objectMapper.convertValue(
                rec,
                objectMapper.getTypeFactory().constructCollectionType(List.class, SsafyAccountRec.class)
        );
    }

    @SuppressWarnings("unchecked")
    private SsafyAccountRec parseSsafyAccountRec(Map<String, Object> response) {
        Object rec = response.get("REC");
        if (rec == null) throw new NotFoundException("계좌 정보를 찾을 수 없습니다.");
        return objectMapper.convertValue(rec, SsafyAccountRec.class);
    }
}