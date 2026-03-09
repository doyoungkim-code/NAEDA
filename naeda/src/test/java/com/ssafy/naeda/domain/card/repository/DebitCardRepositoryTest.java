package com.ssafy.naeda.domain.card.repository;

import com.ssafy.naeda.domain.card.entity.DebitCard;
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
class DebitCardRepositoryTest {

    @Autowired
    private DebitCardRepository debitCardRepository;

    @BeforeEach
    void setUp() {
        debitCardRepository.deleteAll();
    }

    private DebitCard buildDebitCard(Long userNo, String cardNo) {
        return DebitCard.builder()
                .userNo(userNo)
                .cardNo(cardNo)
                .cvc("456")
                .cardUniqueNo("1005-unique-" + cardNo)
                .cardIssuerCode("1005")
                .cardIssuerName("신한카드")
                .cardName("테스트 체크카드")
                .cardExpiryDate("20290101")
                .accountId(1L)
                .build();
    }

    @Test
    @DisplayName("체크카드 저장 및 단건 조회")
    void saveAndFindById() {
        DebitCard saved = debitCardRepository.save(buildDebitCard(1L, "1005000000001111"));

        DebitCard found = debitCardRepository.findById(saved.getDebitCardId()).orElseThrow();
        assertThat(found.getUserNo()).isEqualTo(1L);
        assertThat(found.getCardNo()).isEqualTo("1005000000001111");
        assertThat(found.getCvc()).isEqualTo("456");
        assertThat(found.getCardIssuerCode()).isEqualTo("1005");
        assertThat(found.getIsActive()).isTrue();
        assertThat(found.getCreated()).isNotNull();
    }

    @Test
    @DisplayName("userNo로 체크카드 목록 조회")
    void findByUserNo() {
        debitCardRepository.save(buildDebitCard(1L, "1005000000001111"));
        debitCardRepository.save(buildDebitCard(1L, "1005000000002222"));
        debitCardRepository.save(buildDebitCard(2L, "1005000000003333"));

        List<DebitCard> cards = debitCardRepository.findByUserNo(1L);
        assertThat(cards).hasSize(2);
        assertThat(cards).extracting(DebitCard::getUserNo).containsOnly(1L);
    }

    @Test
    @DisplayName("카드번호로 단건 조회")
    void findByCardNo() {
        debitCardRepository.save(buildDebitCard(1L, "1005000000001111"));

        Optional<DebitCard> found = debitCardRepository.findByCardNo("1005000000001111");
        assertThat(found).isPresent();
        assertThat(found.get().getUserNo()).isEqualTo(1L);

        Optional<DebitCard> notFound = debitCardRepository.findByCardNo("9999999999999999");
        assertThat(notFound).isEmpty();
    }

    @Test
    @DisplayName("카드번호 중복 확인")
    void existsByCardNo() {
        debitCardRepository.save(buildDebitCard(1L, "1005000000001111"));

        assertThat(debitCardRepository.existsByCardNo("1005000000001111")).isTrue();
        assertThat(debitCardRepository.existsByCardNo("9999999999999999")).isFalse();
    }
}
