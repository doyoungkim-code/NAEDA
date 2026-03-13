package com.ssafy.naeda.domain.report.service;

import com.ssafy.naeda.domain.card.dto.request.CardTransactionRequest;
import com.ssafy.naeda.domain.card.dto.response.CardTransactionResponse;
import com.ssafy.naeda.domain.card.entity.CreditCard;
import com.ssafy.naeda.domain.card.entity.DebitCard;
import com.ssafy.naeda.domain.card.repository.CreditCardRepository;
import com.ssafy.naeda.domain.card.repository.DebitCardRepository;
import com.ssafy.naeda.domain.card.service.CardService;
import com.ssafy.naeda.domain.consumption.client.ConsumptionMonthlyInsightAiClient;
import com.ssafy.naeda.domain.report.dto.response.ReportResponse;
import com.ssafy.naeda.domain.report.entity.ConsumptionReport;
import com.ssafy.naeda.domain.report.entity.PeriodType;
import com.ssafy.naeda.domain.report.repository.ConsumptionReportRepository;
import com.ssafy.naeda.domain.user.entity.User;
import com.ssafy.naeda.domain.user.repository.UserRepository;
import com.ssafy.naeda.global.exception.NotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class MonthlyReportGenerationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CreditCardRepository creditCardRepository;

    @Mock
    private DebitCardRepository debitCardRepository;

    @Mock
    private CardService cardService;

    @Mock
    private ConsumptionMonthlyInsightAiClient consumptionMonthlyInsightAiClient;

    @Mock
    private ConsumptionReportRepository consumptionReportRepository;

    @InjectMocks
    private MonthlyReportGenerationService monthlyReportGenerationService;

    @Test
    @DisplayName("월간 리포트 생성 - 활성 카드 거래를 집계해 저장한다")
    void generateMonthlyReport() {
        Long userNo = 1L;
        YearMonth targetMonth = YearMonth.of(2026, 3);

        given(userRepository.findById(userNo)).willReturn(Optional.of(stubUser(userNo)));
        given(creditCardRepository.findByUserNoAndIsActiveTrue(userNo))
                .willReturn(List.of(stubCreditCard(userNo, 11L, "1111", "123", 10L)));
        given(debitCardRepository.findByUserNoAndIsActiveTrue(userNo))
                .willReturn(List.of(stubDebitCard(userNo, 22L, "2222", "456", 20L)));

        given(cardService.getCardTransactionsByCredentials(eq(userNo), eq("1111"), eq("123"), eq(10L), any(CardTransactionRequest.class)))
                .willReturn(List.of(
                        transaction("TX-1", "식비", "식비", "김밥천국", "승인", 12000L),
                        transaction("TX-2", "생활", "카페", "스타벅스", "승인", 6000L)
                ));
        given(cardService.getCardTransactionsByCredentials(eq(userNo), eq("2222"), eq("456"), eq(20L), any(CardTransactionRequest.class)))
                .willReturn(List.of(
                        transaction("TX-2", "생활", "카페", "스타벅스", "승인", 6000L),
                        transaction("TX-3", "교통", "교통", "카카오택시", "승인", 18000L)
                ));

        ConsumptionReport previousReport = ConsumptionReport.builder()
                .reportId(9L)
                .userNo(userNo)
                .periodType(PeriodType.MONTHLY)
                .periodStart(LocalDate.of(2026, 2, 1))
                .periodEnd(LocalDate.of(2026, 2, 28))
                .categoryBreakdown(Map.of("카페", 20000L, "식비", 15000L))
                .totalSpending(35000L)
                .generated(LocalDateTime.of(2026, 3, 1, 9, 0))
                .build();

        given(consumptionReportRepository.findByUserNoAndPeriodTypeAndPeriodStartAndPeriodEnd(
                userNo, PeriodType.MONTHLY, LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28)))
                .willReturn(Optional.of(previousReport));
        given(consumptionReportRepository.findByUserNoAndPeriodTypeAndPeriodStartAndPeriodEnd(
                userNo, PeriodType.MONTHLY, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31)))
                .willReturn(Optional.empty());
        given(consumptionMonthlyInsightAiClient.generateMonthlyInsights(
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 31),
                36000L,
                Map.of("교통", 18000L, "식비", 12000L, "카페", 6000L),
                35000L,
                Map.of("카페", 20000L, "식비", 15000L)
        )).willReturn(List.of("이번 달 교통비가 크게 나갔어요. 반복되는 이동 비용을 한 번 점검해보세요."));

        given(consumptionReportRepository.save(any(ConsumptionReport.class))).willAnswer(invocation -> {
            ConsumptionReport report = invocation.getArgument(0);
            return ConsumptionReport.builder()
                    .reportId(100L)
                    .userNo(report.getUserNo())
                    .periodType(report.getPeriodType())
                    .periodStart(report.getPeriodStart())
                    .periodEnd(report.getPeriodEnd())
                    .categoryBreakdown(report.getCategoryBreakdown())
                    .totalSpending(report.getTotalSpending())
                    .localSpending(report.getLocalSpending())
                    .localRatio(report.getLocalRatio())
                    .localGrade(report.getLocalGrade())
                    .insights(report.getInsights())
                    .generated(report.getGenerated())
                    .build();
        });

        ReportResponse response = monthlyReportGenerationService.generateMonthlyReport(userNo, targetMonth);

        assertThat(response.getReportId()).isEqualTo(100L);
        assertThat(response.getPeriodType()).isEqualTo(PeriodType.MONTHLY);
        assertThat(response.getTotalSpending()).isEqualTo(36000L);
        assertThat(response.getCategoryBreakdown()).containsEntry("교통", 18000L);
        assertThat(response.getCategoryBreakdown()).containsEntry("카페", 6000L);
        assertThat(response.getInsights()).hasSize(1);

        ArgumentCaptor<ConsumptionReport> captor = ArgumentCaptor.forClass(ConsumptionReport.class);
        verify(consumptionReportRepository).save(captor.capture());
        assertThat(captor.getValue().getCategoryBreakdown()).containsEntry("교통", 18000L);
        assertThat(captor.getValue().getLocalSpending()).isNull();
    }

    @Test
    @DisplayName("월간 리포트 생성 실패 - 존재하지 않는 사용자")
    void generateMonthlyReportUserNotFound() {
        given(userRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> monthlyReportGenerationService.generateMonthlyReport(999L, YearMonth.of(2026, 3)))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("존재하지 않는 사용자");
    }

    private User stubUser(Long userNo) {
        return User.builder()
                .userNo(userNo)
                .userId("report@test.com")
                .password("pw")
                .username("테스터")
                .residentNo("9901011")
                .phone("010-0000-0000")
                .institutionCode("M220516185630")
                .userKey("user-key")
                .build();
    }

    private CreditCard stubCreditCard(Long userNo, Long id, String cardNo, String cvc, Long accountId) {
        return CreditCard.builder()
                .creditCardId(id)
                .userNo(userNo)
                .cardNo(cardNo)
                .cvc(cvc)
                .cardUniqueNo("credit-unique")
                .cardIssuerCode("1003")
                .cardIssuerName("롯데카드")
                .cardName("credit")
                .cardExpiryDate("20290409")
                .creditLimit(1000000L)
                .billingDate(15)
                .accountId(accountId)
                .build();
    }

    private DebitCard stubDebitCard(Long userNo, Long id, String cardNo, String cvc, Long accountId) {
        return DebitCard.builder()
                .debitCardId(id)
                .userNo(userNo)
                .cardNo(cardNo)
                .cvc(cvc)
                .cardUniqueNo("debit-unique")
                .cardIssuerCode("1005")
                .cardIssuerName("신한카드")
                .cardName("debit")
                .cardExpiryDate("20290409")
                .accountId(accountId)
                .build();
    }

    private CardTransactionResponse transaction(
            String transactionUniqueNo,
            String categoryName,
            String aiCategory,
            String merchantName,
            String cardStatus,
            Long amount
    ) {
        return CardTransactionResponse.builder()
                .logId(1L)
                .transactionUniqueNo(transactionUniqueNo)
                .categoryName(categoryName)
                .aiCategory(aiCategory)
                .merchantName(merchantName)
                .transactionDate("2026-03-10")
                .transactionTime("14:30:00")
                .amount(amount)
                .cardStatus(cardStatus)
                .build();
    }
}
