package com.ssafy.naeda.domain.address.dto.response;

import com.ssafy.naeda.domain.address.entity.Address;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AddressResponse {

    private Long addressId;
    private String addressName;
    private String recipient;
    private String phone;
    private String roadAddress;
    private String numberAddress;
    private String detailAddress;
    private String zipCode;
    private Boolean isDefault;

    public static AddressResponse from (Address address) {
        return AddressResponse.builder()
                .addressId(address.getAddressId())
                .addressName(address.getAddressName())
                .recipient(address.getRecipient())
                .phone(address.getPhone())
                .roadAddress(address.getRoadAddress())
                .numberAddress(address.getNumberAddress())
                .detailAddress(address.getDetailAddress())
                .zipCode(address.getZipCode())
                .isDefault(address.getIsDefault())
                .build();
    }
}
