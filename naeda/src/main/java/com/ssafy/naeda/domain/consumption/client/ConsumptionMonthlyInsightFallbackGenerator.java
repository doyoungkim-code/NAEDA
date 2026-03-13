package com.ssafy.naeda.domain.consumption.client;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

public final class ConsumptionMonthlyInsightFallbackGenerator {

    private ConsumptionMonthlyInsightFallbackGenerator() {
    }

    public static List<String> generate(Map<String, Long> categoryBreakdown, Long totalSpending) {
        long total = totalSpending == null ? 0L : totalSpending;
        if (total <= 0 || categoryBreakdown == null || categoryBreakdown.isEmpty()) {
            return List.of("이번 달 카드 소비 내역이 거의 없었어요. 다음 달 데이터가 쌓이면 더 정확한 조언을 드릴게요.");
        }

        String topCategory = categoryBreakdown.entrySet().stream()
                .max(Comparator.comparingLong(Map.Entry::getValue))
                .map(Map.Entry::getKey)
                .orElse("기타");

        return switch (topCategory) {
            case "카페" -> List.of("이번 달 카페 지출이 많았어요. 커피 한두 잔만 줄여도 절약 효과가 분명해요.");
            case "식비" -> List.of("이번 달 식비 비중이 높았어요. 외식 횟수를 조금만 줄여도 부담을 낮출 수 있어요.");
            case "교통" -> List.of("이번 달 교통비가 크게 나갔어요. 반복되는 이동 비용을 한 번 점검해보세요.");
            default -> List.of("이번 달 " + topCategory + " 지출이 가장 컸어요. 반복 지출인지 한 번 점검해보세요.");
        };
    }
}
