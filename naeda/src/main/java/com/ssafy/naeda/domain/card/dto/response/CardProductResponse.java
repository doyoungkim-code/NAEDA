package com.ssafy.naeda.domain.card.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardProductResponse {

    private String cardUniqueNo;
    private String cardIssuerCode;
    private String cardIssuerName;
    private String cardName;
    private String cardTypeCode;
    private String cardTypeName;
    private String baselinePerformance;
    private String maxBenefitLimit;
    private String cardDescription;
    private List<BenefitInfo> cardBenefitsInfo;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BenefitInfo {
        private String categoryId;
        private String categoryName;
        private String discountRate;
    }

    @SuppressWarnings("unchecked")
    public static CardProductResponse fromSsafyRec(java.util.Map<String, Object> rec) {
        List<BenefitInfo> benefits = List.of();
        Object benefitsRaw = rec.get("cardBenefitsInfo");
        if (benefitsRaw instanceof List<?> list) {
            benefits = list.stream()
                    .filter(item -> item instanceof java.util.Map)
                    .map(item -> {
                        java.util.Map<String, Object> m = (java.util.Map<String, Object>) item;
                        return BenefitInfo.builder()
                                .categoryId((String) m.get("categoryId"))
                                .categoryName((String) m.get("categoryName"))
                                .discountRate((String) m.get("discountRate"))
                                .build();
                    })
                    .toList();
        }

        return CardProductResponse.builder()
                .cardUniqueNo((String) rec.get("cardUniqueNo"))
                .cardIssuerCode((String) rec.get("cardIssuerCode"))
                .cardIssuerName((String) rec.get("cardIssuerName"))
                .cardName((String) rec.get("cardName"))
                .cardTypeCode((String) rec.get("cardTypeCode"))
                .cardTypeName((String) rec.get("cardTypeName"))
                .baselinePerformance((String) rec.get("baselinePerformance"))
                .maxBenefitLimit((String) rec.get("maxBenefitLimit"))
                .cardDescription((String) rec.get("cardDescription"))
                .cardBenefitsInfo(benefits)
                .build();
    }
}
