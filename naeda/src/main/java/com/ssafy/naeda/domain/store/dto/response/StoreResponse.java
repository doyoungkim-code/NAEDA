package com.ssafy.naeda.domain.store.dto.response;

import com.ssafy.naeda.domain.store.dto.ssafy.SsafyMerchantRec;
import com.ssafy.naeda.domain.store.entity.Store;
import com.ssafy.naeda.domain.store.entity.StoreSourceType;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StoreResponse {

    private Long storeId;
    private Long userNo;
    private String storeName;
    private String categoryId;
    private String categoryName;
    private String roadAddress;
    private String numberAddress;
    private Double latitude;
    private Double longitude;
    private String phone;
    private Boolean isLocalBusiness;
    private Boolean facePayEnabled;
    private Double rating;
    private String imageUrl;
    private String description;
    private StoreSourceType sourceType;
    private Boolean isActive;

    public static StoreResponse from(Store store) {
        return StoreResponse.builder()
                .storeId(store.getStoreId())
                .userNo(store.getUserNo())
                .storeName(store.getStoreName())
                .categoryId(store.getCategoryId())
                .categoryName(store.getCategoryName())
                .roadAddress(store.getRoadAddress())
                .numberAddress(store.getNumberAddress())
                .latitude(store.getLatitude())
                .longitude(store.getLongitude())
                .phone(store.getPhone())
                .isLocalBusiness(store.getIsLocalBusiness())
                .facePayEnabled(store.getFacePayEnabled())
                .rating(store.getRating())
                .imageUrl(store.getImageUrl())
                .description(store.getDescription())
                .sourceType(store.getSourceType() == null ? StoreSourceType.SSAFY : store.getSourceType())
                .isActive(store.getIsActive() == null ? true : store.getIsActive())
                .build();
    }

    /**
     * SSAFY 가맹점 정보만으로 DTO 생성 (우리 DB에 아직 등록되지 않은 가맹점).
     */
    public static StoreResponse fromSsafy(SsafyMerchantRec rec) {
        return StoreResponse.builder()
                .storeId(parseLong(rec.getMerchantId()))
                .storeName(rec.getMerchantName())
                .categoryId(rec.getCategoryId())
                .categoryName(rec.getCategoryName())
                .sourceType(StoreSourceType.SSAFY)
                .isActive(true)
                .build();
    }

    private static Long parseLong(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
