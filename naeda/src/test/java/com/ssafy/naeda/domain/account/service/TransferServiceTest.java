package com.ssafy.naeda.domain.account.service;

import com.ssafy.naeda.domain.account.dto.request.TransferRequest;
import com.ssafy.naeda.domain.account.dto.response.TransferResponse;
import com.ssafy.naeda.domain.account.entity.Account;
import com.ssafy.naeda.domain.account.repository.AccountRepository;
import com.ssafy.naeda.domain.transaction.entity.TransactionLog;
import com.ssafy.naeda.domain.transaction.repository.TransactionLogRepository;
import com.ssafy.naeda.domain.user.entity.User;
import com.ssafy.naeda.domain.user.repository.UserRepository;
import com.ssafy.naeda.global.exception.NotFoundException;
import com.ssafy.naeda.global.ssafy.SsafyApiClient;
import com.ssafy.naeda.global.ssafy.SsafyHeaderFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TransferServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private AccountRepository accountRepository;
    @Mock private TransactionLogRepository transactionLogRepository;
    @Mock private SsafyApiClient ssafyApiClient;
    @Mock private SsafyHeaderFactory ssafyHeaderFactory;

    @InjectMocks
    private TransferService transferService;

    private static final Long USER_NO = 1L;
    private static final String WITHDRAWAL_ACCOUNT = "9990000000001234";
    private static final String DEPOSIT_ACCOUNT = "0010000000005678";

    private User stubUser;
    private Account stubAccount;

    @BeforeEach
    void setUp() {
        stubUser = User.builder()
                .userId("testuser").password("pw").username("테스트")
                .residentNo("0101011").phone("01012345678")
                .institutionCode("00100").userKey("test-user-key")
                .build();

        stubAccount = Account.builder()
                .userNo(USER_NO)
                .bankCode("999").bankName("싸피은행")
                .accountNo(WITHDRAWAL_ACCOUNT).accountName("내 계좌")
                .build();
    }

    private TransferRequest createRequest(String withdrawalAccountNo, String depositAccountNo,
                                          Long amount, String memo) throws Exception {
        TransferRequest request = new TransferRequest();
        setField(request, "withdrawalAccountNo", withdrawalAccountNo);
        setField(request, "depositAccountNo", depositAccountNo);
        setField(request, "amount", amount);
        setField(request, "memo", memo);
        return request;
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private Map<String, Object> stubTransferResponse() {
        return Map.of("REC", List.of(
                Map.of("transactionUniqueNo", "61", "transactionType", "2",
                        "transactionDate", "20260306"),
                Map.of("transactionUniqueNo", "62", "transactionType", "1",
                        "transactionDate", "20260306")
        ));
    }

    private Map<String, Object> stubBalanceResponse(String balance) {
        return Map.of("REC", Map.of("accountBalance", balance));
    }

    // ── 이체 성공 ────────────────────────────────────────────────────────

    @Test
    @DisplayName("이체 성공 - TransferResponse를 반환하고 TransactionLog를 저장한다")
    void transfer_success() throws Exception {
        TransferRequest request = createRequest(WITHDRAWAL_ACCOUNT, DEPOSIT_ACCOUNT, 50_000L, "용돈");

        given(userRepository.findById(USER_NO)).willReturn(Optional.of(stubUser));
        given(accountRepository.findByAccountNoForUpdate(WITHDRAWAL_ACCOUNT)).willReturn(Optional.of(stubAccount));
        given(ssafyHeaderFactory.create(anyString(), anyString())).willReturn(Map.of());
        lenient().when(ssafyApiClient.buildBody(anyMap(), (Object[]) any())).thenReturn(Map.of());
        given(ssafyApiClient.post(eq("/edu/demandDeposit/updateDemandDepositAccountTransfer"), anyMap()))
                .willReturn(stubTransferResponse());
        given(ssafyApiClient.post(eq("/edu/demandDeposit/inquireDemandDepositAccountBalance"), anyMap()))
                .willReturn(stubBalanceResponse("950000"));

        TransferResponse result = transferService.transfer(USER_NO, request);

        assertThat(result.getWithdrawalAccountNo()).isEqualTo(WITHDRAWAL_ACCOUNT);
        assertThat(result.getDepositAccountNo()).isEqualTo(DEPOSIT_ACCOUNT);
        assertThat(result.getAmount()).isEqualTo(50_000L);
        assertThat(result.getTransactionDate()).isEqualTo("20260306");
        assertThat(result.getWithdrawalTransactionNo()).isEqualTo("61");
        assertThat(result.getDepositTransactionNo()).isEqualTo("62");

        ArgumentCaptor<TransactionLog> captor = ArgumentCaptor.forClass(TransactionLog.class);
        verify(transactionLogRepository).save(captor.capture());
        TransactionLog saved = captor.getValue();
        assertThat(saved.getTransactionType()).isEqualTo(com.ssafy.naeda.domain.transaction.entity.TransactionType.WITHDRAW);
        assertThat(saved.getAmount()).isEqualTo(50_000L);
        assertThat(saved.getBalanceAfter()).isEqualTo(950_000L);
        assertThat(saved.getCounterpart()).isEqualTo(DEPOSIT_ACCOUNT);
        assertThat(saved.getMemo()).isEqualTo("용돈");
        assertThat(saved.getSsafyTransactionId()).isEqualTo("61");
    }

    @Test
    @DisplayName("이체 성공 - memo가 null이면 기본값 '이체'로 전달된다")
    void transfer_nullMemo() throws Exception {
        TransferRequest request = createRequest(WITHDRAWAL_ACCOUNT, DEPOSIT_ACCOUNT, 10_000L, null);

        given(userRepository.findById(USER_NO)).willReturn(Optional.of(stubUser));
        given(accountRepository.findByAccountNoForUpdate(WITHDRAWAL_ACCOUNT)).willReturn(Optional.of(stubAccount));
        given(ssafyHeaderFactory.create(anyString(), anyString())).willReturn(Map.of());
        lenient().when(ssafyApiClient.buildBody(anyMap(), (Object[]) any())).thenReturn(Map.of());
        given(ssafyApiClient.post(eq("/edu/demandDeposit/updateDemandDepositAccountTransfer"), anyMap()))
                .willReturn(stubTransferResponse());
        given(ssafyApiClient.post(eq("/edu/demandDeposit/inquireDemandDepositAccountBalance"), anyMap()))
                .willReturn(stubBalanceResponse("990000"));

        TransferResponse result = transferService.transfer(USER_NO, request);
        assertThat(result.getAmount()).isEqualTo(10_000L);
    }

    // ── 잔액 관련 ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("이체 성공 - 잔액 조회 응답의 accountBalance가 null이면 balanceAfter를 0으로 저장한다")
    void transfer_nullBalance_savesZero() throws Exception {
        TransferRequest request = createRequest(WITHDRAWAL_ACCOUNT, DEPOSIT_ACCOUNT, 10_000L, null);

        Map<String, Object> nullBalanceRec = new HashMap<>();
        nullBalanceRec.put("accountBalance", null);
        Map<String, Object> nullBalanceResponse = Map.of("REC", nullBalanceRec);

        given(userRepository.findById(USER_NO)).willReturn(Optional.of(stubUser));
        given(accountRepository.findByAccountNoForUpdate(WITHDRAWAL_ACCOUNT)).willReturn(Optional.of(stubAccount));
        given(ssafyHeaderFactory.create(anyString(), anyString())).willReturn(Map.of());
        lenient().when(ssafyApiClient.buildBody(anyMap(), (Object[]) any())).thenReturn(Map.of());
        given(ssafyApiClient.post(eq("/edu/demandDeposit/updateDemandDepositAccountTransfer"), anyMap()))
                .willReturn(stubTransferResponse());
        given(ssafyApiClient.post(eq("/edu/demandDeposit/inquireDemandDepositAccountBalance"), anyMap()))
                .willReturn(nullBalanceResponse);

        transferService.transfer(USER_NO, request);

        ArgumentCaptor<TransactionLog> captor = ArgumentCaptor.forClass(TransactionLog.class);
        verify(transactionLogRepository).save(captor.capture());
        assertThat(captor.getValue().getBalanceAfter()).isEqualTo(0L);
    }

    @Test
    @DisplayName("이체 성공 - 잔액이 정상이면 balanceAfter에 정확한 값이 저장된다")
    void transfer_balanceAfter_savedCorrectly() throws Exception {
        TransferRequest request = createRequest(WITHDRAWAL_ACCOUNT, DEPOSIT_ACCOUNT, 30_000L, "테스트");

        given(userRepository.findById(USER_NO)).willReturn(Optional.of(stubUser));
        given(accountRepository.findByAccountNoForUpdate(WITHDRAWAL_ACCOUNT)).willReturn(Optional.of(stubAccount));
        given(ssafyHeaderFactory.create(anyString(), anyString())).willReturn(Map.of());
        lenient().when(ssafyApiClient.buildBody(anyMap(), (Object[]) any())).thenReturn(Map.of());
        given(ssafyApiClient.post(eq("/edu/demandDeposit/updateDemandDepositAccountTransfer"), anyMap()))
                .willReturn(stubTransferResponse());
        given(ssafyApiClient.post(eq("/edu/demandDeposit/inquireDemandDepositAccountBalance"), anyMap()))
                .willReturn(stubBalanceResponse("1470000"));

        transferService.transfer(USER_NO, request);

        ArgumentCaptor<TransactionLog> captor = ArgumentCaptor.forClass(TransactionLog.class);
        verify(transactionLogRepository).save(captor.capture());
        assertThat(captor.getValue().getBalanceAfter()).isEqualTo(1_470_000L);
    }

    // ── 이체 실패 ────────────────────────────────────────────────────────

    @Test
    @DisplayName("이체 실패 - 존재하지 않는 사용자이면 NotFoundException")
    void transfer_userNotFound() throws Exception {
        TransferRequest request = createRequest(WITHDRAWAL_ACCOUNT, DEPOSIT_ACCOUNT, 50_000L, null);

        given(userRepository.findById(USER_NO)).willReturn(Optional.empty());

        assertThatThrownBy(() -> transferService.transfer(USER_NO, request))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("존재하지 않는 사용자입니다.");
    }

    @Test
    @DisplayName("이체 실패 - 출금 계좌가 존재하지 않으면 NotFoundException")
    void transfer_accountNotFound() throws Exception {
        TransferRequest request = createRequest(WITHDRAWAL_ACCOUNT, DEPOSIT_ACCOUNT, 50_000L, null);

        given(userRepository.findById(USER_NO)).willReturn(Optional.of(stubUser));
        given(accountRepository.findByAccountNoForUpdate(WITHDRAWAL_ACCOUNT)).willReturn(Optional.empty());

        assertThatThrownBy(() -> transferService.transfer(USER_NO, request))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("계좌를 찾을 수 없거나 접근 권한이 없습니다.");
    }

    @Test
    @DisplayName("이체 실패 - 출금 계좌 소유권 불일치이면 NotFoundException")
    void transfer_ownershipMismatch() throws Exception {
        TransferRequest request = createRequest(WITHDRAWAL_ACCOUNT, DEPOSIT_ACCOUNT, 50_000L, null);
        Account otherAccount = Account.builder()
                .userNo(999L)
                .bankCode("999").bankName("싸피은행")
                .accountNo(WITHDRAWAL_ACCOUNT).accountName("남의계좌")
                .build();

        given(userRepository.findById(USER_NO)).willReturn(Optional.of(stubUser));
        given(accountRepository.findByAccountNoForUpdate(WITHDRAWAL_ACCOUNT)).willReturn(Optional.of(otherAccount));

        assertThatThrownBy(() -> transferService.transfer(USER_NO, request))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("계좌를 찾을 수 없거나 접근 권한이 없습니다.");
    }
}
