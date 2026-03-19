package com.ssafy.naeda.domain.address.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddressRequest {

    private String addressName;

    @NotBlank
    private String recipient;

    @NotBlank
    private String phone;

    @NotBlank
    private String roadAddress;

    private String numberAddress;

    private String detailAddress;

    private String zipCode;

    @NotNull
    private Boolean isDefault;
}
