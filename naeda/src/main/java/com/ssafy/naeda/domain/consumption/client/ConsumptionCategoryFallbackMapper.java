package com.ssafy.naeda.domain.consumption.client;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class ConsumptionCategoryFallbackMapper {

    private static final Map<String, String> RAW_CATEGORY_MAP = Map.ofEntries(
            Map.entry("대형마트", "마트"),
            Map.entry("마트", "마트"),
            Map.entry("편의점", "생활"),
            Map.entry("카페", "카페"),
            Map.entry("디저트", "카페"),
            Map.entry("식비", "식비"),
            Map.entry("음식", "식비"),
            Map.entry("외식", "식비"),
            Map.entry("배달", "식비"),
            Map.entry("교통", "교통"),
            Map.entry("주유", "교통"),
            Map.entry("택시", "교통"),
            Map.entry("쇼핑", "쇼핑"),
            Map.entry("패션", "쇼핑"),
            Map.entry("뷰티", "쇼핑"),
            Map.entry("생활", "생활"),
            Map.entry("교육", "교육"),
            Map.entry("교육/육아", "교육"),
            Map.entry("의료", "의료"),
            Map.entry("병원", "의료"),
            Map.entry("약국", "의료"),
            Map.entry("통신", "주거/통신"),
            Map.entry("공과금", "주거/통신"),
            Map.entry("주거", "주거/통신"),
            Map.entry("문화", "문화/여가"),
            Map.entry("영화", "문화/여가"),
            Map.entry("도서", "문화/여가"),
            Map.entry("레저", "문화/여가"),
            Map.entry("숙박", "여행/숙박"),
            Map.entry("여행", "여행/숙박"),
            Map.entry("항공", "여행/숙박"),
            Map.entry("해외", "여행/숙박"),
            Map.entry("금융", "금융/보험"),
            Map.entry("보험", "금융/보험")
    );

    private static final Map<String, String> MERCHANT_CATEGORY_MAP = new LinkedHashMap<>();

    static {
        MERCHANT_CATEGORY_MAP.put("스타벅스", "카페");
        MERCHANT_CATEGORY_MAP.put("투썸", "카페");
        MERCHANT_CATEGORY_MAP.put("이디야", "카페");
        MERCHANT_CATEGORY_MAP.put("메가커피", "카페");
        MERCHANT_CATEGORY_MAP.put("빽다방", "카페");
        MERCHANT_CATEGORY_MAP.put("할리스", "카페");
        MERCHANT_CATEGORY_MAP.put("coffee", "카페");
        MERCHANT_CATEGORY_MAP.put("cafe", "카페");

        MERCHANT_CATEGORY_MAP.put("배달의민족", "식비");
        MERCHANT_CATEGORY_MAP.put("요기요", "식비");
        MERCHANT_CATEGORY_MAP.put("쿠팡이츠", "식비");
        MERCHANT_CATEGORY_MAP.put("맥도날드", "식비");
        MERCHANT_CATEGORY_MAP.put("버거킹", "식비");
        MERCHANT_CATEGORY_MAP.put("롯데리아", "식비");
        MERCHANT_CATEGORY_MAP.put("식당", "식비");

        MERCHANT_CATEGORY_MAP.put("이마트", "마트");
        MERCHANT_CATEGORY_MAP.put("홈플러스", "마트");
        MERCHANT_CATEGORY_MAP.put("롯데마트", "마트");
        MERCHANT_CATEGORY_MAP.put("코스트코", "마트");
        MERCHANT_CATEGORY_MAP.put("하나로마트", "마트");

        MERCHANT_CATEGORY_MAP.put("gs25", "생활");
        MERCHANT_CATEGORY_MAP.put("cu", "생활");
        MERCHANT_CATEGORY_MAP.put("세븐일레븐", "생활");
        MERCHANT_CATEGORY_MAP.put("emart24", "생활");
        MERCHANT_CATEGORY_MAP.put("올리브영", "생활");
        MERCHANT_CATEGORY_MAP.put("다이소", "생활");

        MERCHANT_CATEGORY_MAP.put("카카오택시", "교통");
        MERCHANT_CATEGORY_MAP.put("카카오t", "교통");
        MERCHANT_CATEGORY_MAP.put("s-oil", "교통");
        MERCHANT_CATEGORY_MAP.put("sk에너지", "교통");
        MERCHANT_CATEGORY_MAP.put("gs칼텍스", "교통");
    }

    private ConsumptionCategoryFallbackMapper() {
    }

    public static String mapToServiceCategory(String rawCategory, String merchantName, String memo) {
        String normalizedText = ((merchantName == null ? "" : merchantName) + " " + (memo == null ? "" : memo))
                .toLowerCase(Locale.ROOT);

        for (Map.Entry<String, String> entry : MERCHANT_CATEGORY_MAP.entrySet()) {
            if (normalizedText.contains(entry.getKey().toLowerCase(Locale.ROOT))) {
                return entry.getValue();
            }
        }

        if (rawCategory == null || rawCategory.isBlank()) {
            return "기타";
        }
        return RAW_CATEGORY_MAP.getOrDefault(rawCategory.trim(), "기타");
    }
}
