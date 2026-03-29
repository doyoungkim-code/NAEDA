package com.ssafy.naeda.domain.pay.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Getter
@Builder
public class CurrentMonthSpendingAnalysisResponse {

    private LocalDateTime periodStart;
    private LocalDateTime periodEnd;
    private Long totalSpending;
    private Integer transactionCount;
    private String topCategory;
    private Long topAmount;
    private Map<String, Long> categoryBreakdown;
    private List<String> insights;
}
