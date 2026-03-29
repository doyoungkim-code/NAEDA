package com.ssafy.naeda.domain.store.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class StoreCreateRequest {

    @NotBlank
    private String categoryId;    // SSAFY 카테고리 ID (예: "CG-4fa85f6425ad1d3")
    @NotBlank
    private String storeName;
    @NotNull
    private Long userNo;
    @NotNull
    private Long accountId;
    private String roadAddress;
    private String numberAddress;
    private Double latitude;
    private Double longitude;
    private String phone;
    private Boolean isLocalBusiness;
    private Boolean facePayEnabled;
}
