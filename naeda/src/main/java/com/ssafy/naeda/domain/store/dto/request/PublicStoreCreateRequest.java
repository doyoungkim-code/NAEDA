package com.ssafy.naeda.domain.store.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PublicStoreCreateRequest {

    @NotBlank
    private String categoryId;
    private String categoryName;

    @NotBlank
    private String storeName;

    @NotNull
    private Long userNo;

    @NotNull
    private Long accountId;

    private String roadAddress;
    private String numberAddress;

    @NotNull
    private Double latitude;

    @NotNull
    private Double longitude;

    private String phone;
    private Boolean isLocalBusiness;
    private Boolean facePayEnabled;
}
