package com.ssafy.naeda.domain.card.repository;

import com.ssafy.naeda.domain.card.entity.CreditCard;
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
class CreditCardRepositoryTest {

    @Autowired
    private CreditCardRepository creditCardRepository;

    @BeforeEach
    void setUp() {
        creditCardRepository.deleteAll();
    }

    private CreditCard buildCreditCard(Long userNo, String cardNo) {
        return CreditCard.builder()
                .userNo(userNo)
                .cardNo(cardNo)
                .cvc("123")
                .cardUniqueNo("1003-unique-" + cardNo)
                .cardIssuerCode("1003")
                .cardIssuerName("롯데카드")
                .cardName("테스트 신용카드")
                .cardExpiryDate("20290101")
                .creditLimit(1_000_000L)
                .billingDate(15)
                .accountId(1L)
                .build();
    }

    @Test
    @DisplayName("신용카드 저장 및 단건 조회")
    void saveAndFindById() {
        CreditCard saved = creditCardRepository.save(buildCreditCard(1L, "1003000000001111"));

        CreditCard found = creditCardRepository.findById(saved.getCreditCardId()).orElseThrow();
        assertThat(found.getUserNo()).isEqualTo(1L);
        assertThat(found.getCardNo()).isEqualTo("1003000000001111");
        assertThat(found.getCvc()).isEqualTo("123");
        assertThat(found.getCardIssuerCode()).isEqualTo("1003");
        assertThat(found.getCreditLimit()).isEqualTo(1_000_000L);
        assertThat(found.getBillingDate()).isEqualTo(15);
        assertThat(found.getIsActive()).isTrue();
        assertThat(found.getCreated()).isNotNull();
    }

    @Test
    @DisplayName("카드번호로 활성 카드 단건 조회")
    void findByCardNoAndIsActiveTrue() {
        CreditCard card = creditCardRepository.save(buildCreditCard(1L, "1003000000001111"));

        Optional<CreditCard> found = creditCardRepository.findByCardNoAndIsActiveTrue("1003000000001111");
        assertThat(found).isPresent();
        assertThat(found.get().getUserNo()).isEqualTo(1L);

        // 비활성화 후 조회 시 빈 결과
        card.deactivate();
        creditCardRepository.save(card);
        Optional<CreditCard> deactivated = creditCardRepository.findByCardNoAndIsActiveTrue("1003000000001111");
        assertThat(deactivated).isEmpty();

        Optional<CreditCard> notFound = creditCardRepository.findByCardNoAndIsActiveTrue("9999999999999999");
        assertThat(notFound).isEmpty();
    }

    @Test
    @DisplayName("카드번호 중복 확인")
    void existsByCardNo() {
        creditCardRepository.save(buildCreditCard(1L, "1003000000001111"));

        assertThat(creditCardRepository.existsByCardNo("1003000000001111")).isTrue();
        assertThat(creditCardRepository.existsByCardNo("9999999999999999")).isFalse();
    }

    @Test
    @DisplayName("userNo + 활성 카드만 조회")
    void findByUserNoAndIsActiveTrue() {
        CreditCard active1 = creditCardRepository.save(buildCreditCard(1L, "1003000000001111"));
        CreditCard active2 = creditCardRepository.save(buildCreditCard(1L, "1003000000002222"));
        CreditCard inactive = creditCardRepository.save(buildCreditCard(1L, "1003000000003333"));
        inactive.deactivate();
        creditCardRepository.save(inactive);

        creditCardRepository.save(buildCreditCard(2L, "1003000000004444"));

        List<CreditCard> cards = creditCardRepository.findByUserNoAndIsActiveTrue(1L);
        assertThat(cards).hasSize(2);
        assertThat(cards).extracting(CreditCard::getCardNo)
                .containsExactlyInAnyOrder("1003000000001111", "1003000000002222");
    }
}
