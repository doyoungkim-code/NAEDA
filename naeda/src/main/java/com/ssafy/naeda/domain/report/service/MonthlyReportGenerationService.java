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
import com.ssafy.naeda.domain.user.repository.UserRepository;
import com.ssafy.naeda.global.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class MonthlyReportGenerationService {

    private static final DateTimeFormatter SSAFY_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final UserRepository userRepository;
    private final CreditCardRepository creditCardRepository;
    private final DebitCardRepository debitCardRepository;
    private final CardService cardService;
    private final ConsumptionMonthlyInsightAiClient consumptionMonthlyInsightAiClient;
    private final ConsumptionReportRepository consumptionReportRepository;

    public ReportResponse generateMonthlyReport(Long userNo, YearMonth targetMonth) {
        userRepository.findById(userNo)
                .orElseThrow(() -> new NotFoundException("존재하지 않는 사용자입니다."));

        LocalDate periodStart = targetMonth.atDay(1);
        LocalDate periodEnd = targetMonth.atEndOfMonth();
        CardTransactionRequest request = buildCardTransactionRequest(periodStart, periodEnd);

        List<CardCredential> cards = loadActiveCards(userNo);
        List<CardTransactionResponse> transactions = new ArrayList<>();
        for (CardCredential card : cards) {
            transactions.addAll(
                    cardService.getCardTransactionsByCredentials(
                            userNo,
                            card.cardNo(),
                            card.cvc(),
                            card.accountId(),
                            request
                    )
            );
        }

        List<CardTransactionResponse> uniqueTransactions = deduplicateTransactions(transactions);
        Map<String, Long> categoryBreakdown = aggregateCategoryBreakdown(uniqueTransactions);
        long totalSpending = uniqueTransactions.stream()
                .filter(this::isSpendingTransaction)
                .map(CardTransactionResponse::getAmount)
                .filter(Objects::nonNull)
                .mapToLong(Long::longValue)
                .sum();

        YearMonth previousMonth = targetMonth.minusMonths(1);
        LocalDate previousStart = previousMonth.atDay(1);
        LocalDate previousEnd = previousMonth.atEndOfMonth();
        Optional<ConsumptionReport> previousReport = consumptionReportRepository
                .findByUserNoAndPeriodTypeAndPeriodStartAndPeriodEnd(
                        userNo,
                        PeriodType.MONTHLY,
                        previousStart,
                        previousEnd
                );

        List<String> insights = consumptionMonthlyInsightAiClient.generateMonthlyInsights(
                periodStart,
                periodEnd,
                totalSpending,
                categoryBreakdown,
                previousReport.map(ConsumptionReport::getTotalSpending).orElse(null),
                previousReport.map(ConsumptionReport::getCategoryBreakdown).orElse(null)
        );

        ConsumptionReport existingReport = consumptionReportRepository
                .findByUserNoAndPeriodTypeAndPeriodStartAndPeriodEnd(
                        userNo,
                        PeriodType.MONTHLY,
                        periodStart,
                        periodEnd
                )
                .orElse(null);

        ConsumptionReport report = ConsumptionReport.builder()
                .reportId(existingReport != null ? existingReport.getReportId() : null)
                .userNo(userNo)
                .periodType(PeriodType.MONTHLY)
                .periodStart(periodStart)
                .periodEnd(periodEnd)
                .categoryBreakdown(categoryBreakdown)
                .totalSpending(totalSpending)
                .localSpending(null)
                .localRatio(null)
                .localGrade(null)
                .insights(insights)
                .generated(LocalDateTime.now())
                .build();

        return ReportResponse.from(consumptionReportRepository.save(report));
    }

    private CardTransactionRequest buildCardTransactionRequest(LocalDate periodStart, LocalDate periodEnd) {
        CardTransactionRequest request = new CardTransactionRequest();
        request.setStartDate(periodStart.format(SSAFY_DATE_FORMAT));
        request.setEndDate(periodEnd.format(SSAFY_DATE_FORMAT));
        return request;
    }

    private List<CardCredential> loadActiveCards(Long userNo) {
        List<CardCredential> cards = new ArrayList<>();
        for (CreditCard card : creditCardRepository.findByUserNoAndIsActiveTrue(userNo)) {
            cards.add(new CardCredential(card.getCardNo(), card.getCvc(), card.getAccountId()));
        }
        for (DebitCard card : debitCardRepository.findByUserNoAndIsActiveTrue(userNo)) {
            cards.add(new CardCredential(card.getCardNo(), card.getCvc(), card.getAccountId()));
        }
        return cards;
    }

    private List<CardTransactionResponse> deduplicateTransactions(List<CardTransactionResponse> transactions) {
        Map<String, CardTransactionResponse> uniqueTransactions = new LinkedHashMap<>();
        for (CardTransactionResponse transaction : transactions) {
            if (transaction == null) {
                continue;
            }
            String key = buildTransactionKey(transaction);
            uniqueTransactions.putIfAbsent(key, transaction);
        }
        return new ArrayList<>(uniqueTransactions.values());
    }

    private String buildTransactionKey(CardTransactionResponse transaction) {
        if (hasText(transaction.getTransactionUniqueNo())) {
            return transaction.getTransactionUniqueNo();
        }
        return String.join(
                "|",
                transaction.getMerchantName() == null ? "" : transaction.getMerchantName(),
                transaction.getTransactionDate() == null ? "" : transaction.getTransactionDate(),
                transaction.getTransactionTime() == null ? "" : transaction.getTransactionTime(),
                String.valueOf(transaction.getAmount() == null ? 0L : transaction.getAmount())
        );
    }

    private Map<String, Long> aggregateCategoryBreakdown(List<CardTransactionResponse> transactions) {
        Map<String, Long> aggregated = new LinkedHashMap<>();
        for (CardTransactionResponse transaction : transactions) {
            if (!isSpendingTransaction(transaction)) {
                continue;
            }
            String category = hasText(transaction.getAiCategory())
                    ? transaction.getAiCategory()
                    : hasText(transaction.getCategoryName()) ? transaction.getCategoryName() : "기타";
            aggregated.merge(category, transaction.getAmount(), Long::sum);
        }

        return aggregated.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder()))
                .collect(
                        LinkedHashMap::new,
                        (map, entry) -> map.put(entry.getKey(), entry.getValue()),
                        LinkedHashMap::putAll
                );
    }

    private boolean isSpendingTransaction(CardTransactionResponse transaction) {
        if (transaction == null || transaction.getAmount() == null || transaction.getAmount() <= 0) {
            return false;
        }
        String cardStatus = transaction.getCardStatus();
        if (!hasText(cardStatus)) {
            return true;
        }
        return !cardStatus.contains("취소");
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private record CardCredential(String cardNo, String cvc, Long accountId) {
    }
}
