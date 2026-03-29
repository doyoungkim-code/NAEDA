package com.ssafy.naeda.domain.account.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.naeda.domain.account.dto.response.AccountResponse;
import com.ssafy.naeda.domain.account.entity.Account;
import com.ssafy.naeda.domain.account.repository.AccountRepository;
import com.ssafy.naeda.domain.user.entity.User;
import com.ssafy.naeda.domain.user.repository.UserRepository;
import com.ssafy.naeda.global.exception.NotFoundException;
import com.ssafy.naeda.global.ssafy.SsafyApiClient;
import com.ssafy.naeda.global.ssafy.SsafyHeaderFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SsafyApiClient ssafyApiClient;

    @Mock
    private SsafyHeaderFactory ssafyHeaderFactory;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private AccountService accountService;

    private User mockUser;
    private Account mockAccount;

    @BeforeEach
    void setUp() {
        mockUser = User.builder()
                .userNo(1L)
                .userId("test@ssafy.co.kr")
                .password("encoded")
                .username("테스터")
                .residentNo("9901011")
                .phone("01012345678")
                .institutionCode("00100")
                .userKey("test-user-key")
                .build();

        mockAccount = Account.builder()
                .userNo(1L)
                .bankCode("999")
                .bankName("싸피은행")
                .accountNo("9990000000001234")
                .accountName("내 계좌")
                .build();
    }

    // ── getAccounts ─────────────────────────────────────────────────────

    @Test
    @DisplayName("계좌 목록 조회 - DB에 있는 계좌")
    void getAccounts_withDbAccount() {
        given(userRepository.findById(1L)).willReturn(Optional.of(mockUser));
        given(ssafyHeaderFactory.create(anyString(), anyString())).willReturn(Map.of());
        given(ssafyApiClient.buildBody(anyMap())).willReturn(Map.of());
        given(ssafyApiClient.post(anyString(), anyMap())).willReturn(Map.of(
                "REC", List.of(Map.of(
                        "bankCode", "999",
                        "bankName", "싸피은행",
                        "accountNo", "9990000000001234",
                        "accountName", "싸피은행 수시입출금",
                        "accountBalance", "5000000",
                        "currency", "KRW"
                ))
        ));
        given(accountRepository.findByUserNo(1L)).willReturn(List.of(mockAccount));

        List<AccountResponse> result = accountService.getAccounts(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAccountNo()).isEqualTo("9990000000001234");
        assertThat(result.get(0).getAccountBalance()).isEqualTo(5_000_000L);
        assertThat(result.get(0).getAccountName()).isEqualTo("내 계좌"); // DB 우선
    }

    @Test
    @DisplayName("계좌 목록 조회 - DB에 없는 계좌 (SSAFY에만 존재)")
    void getAccounts_ssafyOnly() {
        given(userRepository.findById(1L)).willReturn(Optional.of(mockUser));
        given(ssafyHeaderFactory.create(anyString(), anyString())).willReturn(Map.of());
        given(ssafyApiClient.buildBody(anyMap())).willReturn(Map.of());
        given(ssafyApiClient.post(anyString(), anyMap())).willReturn(Map.of(
                "REC", List.of(Map.of(
                        "bankCode", "001",
                        "bankName", "한국은행",
                        "accountNo", "0010000000005678",
                        "accountName", "한국은행 수시입출금",
                        "accountBalance", "0",
                        "currency", "KRW"
                ))
        ));
        given(accountRepository.findByUserNo(1L)).willReturn(List.of());

        List<AccountResponse> result = accountService.getAccounts(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAccountId()).isNull(); // DB에 없으므로 null
        assertThat(result.get(0).getAccountNo()).isEqualTo("0010000000005678");
    }

    @Test
    @DisplayName("계좌 목록 조회 - SSAFY 응답 REC이 null이면 빈 리스트")
    void getAccounts_emptyRec() {
        given(userRepository.findById(1L)).willReturn(Optional.of(mockUser));
        given(ssafyHeaderFactory.create(anyString(), anyString())).willReturn(Map.of());
        given(ssafyApiClient.buildBody(anyMap())).willReturn(Map.of());
        given(ssafyApiClient.post(anyString(), anyMap())).willReturn(Map.of());
        given(accountRepository.findByUserNo(1L)).willReturn(List.of());

        List<AccountResponse> result = accountService.getAccounts(1L);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("계좌 목록 조회 - 존재하지 않는 사용자")
    void getAccounts_userNotFound() {
        given(userRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.getAccounts(999L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("존재하지 않는 사용자입니다.");
    }

    // ── getAccount ──────────────────────────────────────────────────────

    @Test
    @DisplayName("계좌 단건 조회 - DB에 있는 계좌")
    void getAccount_withDbAccount() {
        given(userRepository.findById(1L)).willReturn(Optional.of(mockUser));
        given(ssafyHeaderFactory.create(anyString(), anyString())).willReturn(Map.of());
        given(ssafyApiClient.buildBody(anyMap(), anyString(), anyString())).willReturn(Map.of());
        given(ssafyApiClient.post(anyString(), anyMap())).willReturn(Map.of(
                "REC", Map.of(
                        "bankCode", "999",
                        "bankName", "싸피은행",
                        "accountNo", "9990000000001234",
                        "accountName", "싸피은행 수시입출금",
                        "accountBalance", "3000000",
                        "currency", "KRW"
                )
        ));
        given(accountRepository.findByAccountNo("9990000000001234"))
                .willReturn(Optional.of(mockAccount));

        AccountResponse result = accountService.getAccount(1L, "9990000000001234");

        assertThat(result.getAccountNo()).isEqualTo("9990000000001234");
        assertThat(result.getAccountBalance()).isEqualTo(3_000_000L);
        assertThat(result.getAccountName()).isEqualTo("내 계좌");
    }

    @Test
    @DisplayName("계좌 단건 조회 - DB에 없는 계좌")
    void getAccount_ssafyOnly() {
        given(userRepository.findById(1L)).willReturn(Optional.of(mockUser));
        given(ssafyHeaderFactory.create(anyString(), anyString())).willReturn(Map.of());
        given(ssafyApiClient.buildBody(anyMap(), anyString(), anyString())).willReturn(Map.of());
        given(ssafyApiClient.post(anyString(), anyMap())).willReturn(Map.of(
                "REC", Map.of(
                        "bankCode", "001",
                        "bankName", "한국은행",
                        "accountNo", "0010000000005678",
                        "accountName", "한국은행 수시입출금",
                        "accountBalance", "100000",
                        "currency", "KRW"
                )
        ));
        given(accountRepository.findByAccountNo("0010000000005678"))
                .willReturn(Optional.empty());

        AccountResponse result = accountService.getAccount(1L, "0010000000005678");

        assertThat(result.getAccountId()).isNull();
        assertThat(result.getAccountBalance()).isEqualTo(100_000L);
    }

    @Test
    @DisplayName("계좌 단건 조회 - SSAFY 응답 REC이 null이면 예외")
    void getAccount_recNull() {
        given(userRepository.findById(1L)).willReturn(Optional.of(mockUser));
        given(ssafyHeaderFactory.create(anyString(), anyString())).willReturn(Map.of());
        given(ssafyApiClient.buildBody(anyMap(), anyString(), anyString())).willReturn(Map.of());
        given(ssafyApiClient.post(anyString(), anyMap())).willReturn(Map.of());

        assertThatThrownBy(() -> accountService.getAccount(1L, "0000000000000000"))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("계좌 정보를 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("계좌 단건 조회 - 존재하지 않는 사용자")
    void getAccount_userNotFound() {
        given(userRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.getAccount(999L, "9990000000001234"))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("존재하지 않는 사용자입니다.");
    }
}
