package com.ssafy.naeda.domain.recommend.dto.response;

import com.ssafy.naeda.domain.store.entity.Store;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RecommendResponse {

    private Long storeId;
    private String storeName;
    private String categoryName;
    private String roadAddress;
    private Double latitude;
    private Double longitude;
    private Double rating;
    private String imageUrl;
    private String description;
    private Long visitCount;     // 결제(방문) 횟수
    private Double score;        // 추천 점수 (별점 * 0.6 + 정규화 방문수 * 0.4)

    public static RecommendResponse from(Store store, long visitCount, double score) {
        return RecommendResponse.builder()
                .storeId(store.getStoreId())
                .storeName(store.getStoreName())
                .categoryName(store.getCategoryName())
                .roadAddress(store.getRoadAddress())
                .latitude(store.getLatitude())
                .longitude(store.getLongitude())
                .rating(store.getRating())
                .imageUrl(store.getImageUrl())
                .description(store.getDescription())
                .visitCount(visitCount)
                .score(score)
                .build();
    }
}