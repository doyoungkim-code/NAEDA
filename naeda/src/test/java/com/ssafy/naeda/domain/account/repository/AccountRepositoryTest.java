package com.ssafy.naeda.domain.account.repository;

import com.ssafy.naeda.domain.account.entity.Account;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AccountRepositoryTest {

    @Autowired
    private AccountRepository accountRepository;

    @BeforeEach
    void setUp() {
        accountRepository.deleteAll();
    }

    @Test
    @DisplayName("계좌 저장 및 단건 조회")
    void saveAndFindById() {
        Account account = accountRepository.save(
                Account.builder()
                        .userNo(1L)
                        .bankCode("999")
                        .bankName("싸피은행")
                        .accountNo("9990000000001234")
                        .accountName("싸피은행 수시입출금")
                        .build()
        );

        Account found = accountRepository.findById(account.getAccountId()).orElseThrow();
        assertThat(found.getUserNo()).isEqualTo(1L);
        assertThat(found.getBankCode()).isEqualTo("999");
        assertThat(found.getAccountNo()).isEqualTo("9990000000001234");
        assertThat(found.getCreated()).isNotNull();
    }

    @Test
    @DisplayName("userNo로 계좌 목록 조회")
    void findByUserNo() {
        accountRepository.save(Account.builder()
                .userNo(1L).bankCode("999").bankName("싸피은행")
                .accountNo("9990000000001111").accountName("계좌1").build());
        accountRepository.save(Account.builder()
                .userNo(1L).bankCode("001").bankName("한국은행")
                .accountNo("0010000000002222").accountName("계좌2").build());
        accountRepository.save(Account.builder()
                .userNo(2L).bankCode("999").bankName("싸피은행")
                .accountNo("9990000000003333").accountName("다른유저").build());

        List<Account> accounts = accountRepository.findByUserNo(1L);
        assertThat(accounts).hasSize(2);
        assertThat(accounts).extracting(Account::getUserNo)
                .containsOnly(1L);
    }

    @Test
    @DisplayName("계좌번호로 단건 조회")
    void findByAccountNo() {
        accountRepository.save(Account.builder()
                .userNo(1L).bankCode("999").bankName("싸피은행")
                .accountNo("9990000000001234").accountName("테스트계좌").build());

        Optional<Account> found = accountRepository.findByAccountNo("9990000000001234");
        assertThat(found).isPresent();
        assertThat(found.get().getBankName()).isEqualTo("싸피은행");

        Optional<Account> notFound = accountRepository.findByAccountNo("0000000000000000");
        assertThat(notFound).isEmpty();
    }

    @Test
    @DisplayName("계좌번호 존재 여부 확인")
    void existsByAccountNo() {
        accountRepository.save(Account.builder()
                .userNo(1L).bankCode("999").bankName("싸피은행")
                .accountNo("9990000000001234").accountName("테스트계좌").build());

        assertThat(accountRepository.existsByAccountNo("9990000000001234")).isTrue();
        assertThat(accountRepository.existsByAccountNo("0000000000000000")).isFalse();
    }
}
