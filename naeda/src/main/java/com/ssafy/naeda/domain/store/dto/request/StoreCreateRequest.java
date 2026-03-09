package com.ssafy.naeda.domain.store.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class StoreCreateRequest {

    private String categoryId;    // SSAFY 카테고리 ID (예: "CG-4fa85f6425ad1d3")
    private String storeName;
    private Long userNo;
    private Long accountId;
    private String roadAddress;
    private String numberAddress;
    private Double latitude;
    private Double longitude;
    private String phone;
    private Boolean isLocalBusiness;
    private Boolean facePayEnabled;
}
