package com.ssafy.naeda.domain.payment.repository;

import com.ssafy.naeda.domain.payment.entity.MethodType;
import com.ssafy.naeda.domain.payment.entity.PaymentMethod;
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
class PaymentMethodRepositoryTest {

    @Autowired
    private PaymentMethodRepository paymentMethodRepository;

    @BeforeEach
    void setUp() {
        paymentMethodRepository.deleteAll();
    }

    @Test
    @DisplayName("결제수단 저장 및 단건 조회")
    void saveAndFindById() {
        PaymentMethod saved = paymentMethodRepository.save(
                PaymentMethod.builder()
                        .userNo(1L)
                        .methodType(MethodType.CREDIT_CARD)
                        .creditCardId(10L)
                        .build()
        );

        PaymentMethod found = paymentMethodRepository.findById(saved.getPaymentMethodId()).orElseThrow();
        assertThat(found.getUserNo()).isEqualTo(1L);
        assertThat(found.getMethodType()).isEqualTo(MethodType.CREDIT_CARD);
        assertThat(found.getCreditCardId()).isEqualTo(10L);
        assertThat(found.getIsDefault()).isFalse();
        assertThat(found.getIsFacePay()).isFalse();
        assertThat(found.getCreated()).isNotNull();
    }

    @Test
    @DisplayName("userNo로 결제수단 목록 조회")
    void findByUserNo() {
        paymentMethodRepository.save(PaymentMethod.builder()
                .userNo(1L).methodType(MethodType.ACCOUNT).accountId(1L).build());
        paymentMethodRepository.save(PaymentMethod.builder()
                .userNo(1L).methodType(MethodType.CREDIT_CARD).creditCardId(10L).build());
        paymentMethodRepository.save(PaymentMethod.builder()
                .userNo(2L).methodType(MethodType.DEBIT_CARD).debitCardId(5L).build());

        List<PaymentMethod> methods = paymentMethodRepository.findByUserNo(1L);
        assertThat(methods).hasSize(2);
        assertThat(methods).extracting(PaymentMethod::getUserNo).containsOnly(1L);
    }

    @Test
    @DisplayName("페이스페이 결제수단 조회 - 설정된 경우")
    void findByUserNoAndIsFacePayTrue_found() {
        PaymentMethod facePay = PaymentMethod.builder()
                .userNo(1L).methodType(MethodType.CREDIT_CARD).creditCardId(10L).build();
        facePay.setAsFacePay();
        paymentMethodRepository.save(facePay);

        paymentMethodRepository.save(PaymentMethod.builder()
                .userNo(1L).methodType(MethodType.ACCOUNT).accountId(1L).build());

        Optional<PaymentMethod> found = paymentMethodRepository.findByUserNoAndIsFacePayTrue(1L);
        assertThat(found).isPresent();
        assertThat(found.get().getIsFacePay()).isTrue();
        assertThat(found.get().getIsDefault()).isTrue();
        assertThat(found.get().getMethodType()).isEqualTo(MethodType.CREDIT_CARD);
    }

    @Test
    @DisplayName("페이스페이 결제수단 조회 - 설정 안 된 경우")
    void findByUserNoAndIsFacePayTrue_notFound() {
        paymentMethodRepository.save(PaymentMethod.builder()
                .userNo(1L).methodType(MethodType.ACCOUNT).accountId(1L).build());

        Optional<PaymentMethod> found = paymentMethodRepository.findByUserNoAndIsFacePayTrue(1L);
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("setAsFacePay 호출 시 isDefault, isFacePay 모두 true")
    void setAsFacePay() {
        PaymentMethod method = PaymentMethod.builder()
                .userNo(1L).methodType(MethodType.DEBIT_CARD).debitCardId(5L).build();

        assertThat(method.getIsDefault()).isFalse();
        assertThat(method.getIsFacePay()).isFalse();

        method.setAsFacePay();

        assertThat(method.getIsDefault()).isTrue();
        assertThat(method.getIsFacePay()).isTrue();
    }

    @Test
    @DisplayName("creditCardId로 결제수단 조회")
    void findByCreditCardId() {
        paymentMethodRepository.save(PaymentMethod.builder()
                .userNo(1L).methodType(MethodType.CREDIT_CARD).creditCardId(10L).build());
        paymentMethodRepository.save(PaymentMethod.builder()
                .userNo(1L).methodType(MethodType.CREDIT_CARD).creditCardId(20L).build());

        List<PaymentMethod> methods = paymentMethodRepository.findByCreditCardId(10L);
        assertThat(methods).hasSize(1);
        assertThat(methods.get(0).getCreditCardId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("debitCardId로 결제수단 조회")
    void findByDebitCardId() {
        paymentMethodRepository.save(PaymentMethod.builder()
                .userNo(1L).methodType(MethodType.DEBIT_CARD).debitCardId(5L).build());
        paymentMethodRepository.save(PaymentMethod.builder()
                .userNo(1L).methodType(MethodType.DEBIT_CARD).debitCardId(15L).build());

        List<PaymentMethod> methods = paymentMethodRepository.findByDebitCardId(5L);
        assertThat(methods).hasSize(1);
        assertThat(methods.get(0).getDebitCardId()).isEqualTo(5L);
    }
}
