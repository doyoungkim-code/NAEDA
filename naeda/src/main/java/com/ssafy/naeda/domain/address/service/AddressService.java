package com.ssafy.naeda.domain.address.service;

import com.ssafy.naeda.domain.address.dto.request.AddressRequest;
import com.ssafy.naeda.domain.address.dto.response.AddressResponse;
import com.ssafy.naeda.domain.address.entity.Address;
import com.ssafy.naeda.domain.address.repository.AddressRepository;
import com.ssafy.naeda.global.exception.BadRequestException;
import com.ssafy.naeda.global.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AddressService {

    private final AddressRepository addressRepository;

    public List<AddressResponse> getAddresses(Long userNo) {
        return addressRepository.findByUserNo(userNo).stream()
                .map(AddressResponse::from)
                .toList();
    }

    public AddressResponse getAddress(Long userNo, Long addressId) {
        Address address = findAddressAndValidateOwner(userNo, addressId);
        return AddressResponse.from(address);
    }

    @Transactional
    public AddressResponse createAddress(Long userNo, AddressRequest request) {
        if (Boolean.TRUE.equals(request.getIsDefault())) {
            clearDefaultAddress(userNo);
        }

        Address address = Address.builder()
                .userNo(userNo)
                .addressName(request.getAddressName())
                .recipient(request.getRecipient())
                .phone(request.getPhone())
                .roadAddress(request.getRoadAddress())
                .numberAddress(request.getNumberAddress())
                .detailAddress(request.getDetailAddress())
                .zipCode(request.getZipCode())
                .isDefault(request.getIsDefault())
                .build();

        Address saved = addressRepository.save(address);
        return AddressResponse.from(saved);
    }

    @Transactional
    public AddressResponse updateAddress(Long userNo, Long addressId, AddressRequest request) {
        Address address = findAddressAndValidateOwner(userNo, addressId);

        if (Boolean.TRUE.equals(request.getIsDefault()) && !Boolean.TRUE.equals(address.getIsDefault())) {
            clearDefaultAddress(userNo);
        }

        address.update(
                request.getAddressName(),
                request.getRecipient(),
                request.getPhone(),
                request.getRoadAddress(),
                request.getNumberAddress(),
                request.getDetailAddress(),
                request.getZipCode()
        );
        address.updateDefault(request.getIsDefault());

        return AddressResponse.from(address);
    }

    @Transactional
    public void deleteAddress(Long userNo, Long addressId) {
        Address address = findAddressAndValidateOwner(userNo, addressId);
        addressRepository.delete(address);
    }

    @Transactional
    public AddressResponse setDefaultAddress(Long userNo, Long addressId) {
        Address address = findAddressAndValidateOwner(userNo, addressId);
        clearDefaultAddress(userNo);
        address.updateDefault(true);
        return AddressResponse.from(address);
    }

    private Address findAddressAndValidateOwner(Long userNo, Long addressId) {
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new NotFoundException("주소를 찾을 수 없습니다. addressId: " + addressId));

        if (!address.getUserNo().equals(userNo)) {
            throw new BadRequestException("본인의 주소만 접근할 수 있습니다.");
        }

        return address;
    }

    private void clearDefaultAddress(Long userNo) {
        addressRepository.findByUserNoAndIsDefaultTrue(userNo)
                .ifPresent(addr -> addr.updateDefault(false));
    }
}