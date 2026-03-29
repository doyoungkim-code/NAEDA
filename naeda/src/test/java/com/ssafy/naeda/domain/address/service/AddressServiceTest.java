package com.ssafy.naeda.domain.address.service;

import com.ssafy.naeda.domain.address.dto.request.AddressRequest;
import com.ssafy.naeda.domain.address.dto.response.AddressResponse;
import com.ssafy.naeda.domain.address.entity.Address;
import com.ssafy.naeda.domain.address.repository.AddressRepository;
import com.ssafy.naeda.global.exception.BadRequestException;
import com.ssafy.naeda.global.exception.NotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class AddressServiceTest {

    @InjectMocks
    private AddressService addressService;

    @Mock
    private AddressRepository addressRepository;

    private static final Long USER_NO = 1L;
    private static final Long ADDRESS_ID = 10L;

    private Address address(Long id, Long userNo, boolean isDefault) {
        return Address.builder()
                .addressId(id)
                .userNo(userNo)
                .addressName("집")
                .recipient("홍길동")
                .phone("01012345678")
                .roadAddress("서울시 강남구 테헤란로 1")
                .numberAddress("역삼동 123-4")
                .detailAddress("101호")
                .zipCode("06234")
                .isDefault(isDefault)
                .build();
    }

    private AddressRequest request(boolean isDefault) {
        return AddressRequest.builder()
                .addressName("회사")
                .recipient("김철수")
                .phone("01098765432")
                .roadAddress("서울시 서초구 서초대로 2")
                .numberAddress("서초동 456-7")
                .detailAddress("202호")
                .zipCode("06500")
                .isDefault(isDefault)
                .build();
    }

    // ── getAddresses ──────────────────────────────────────────────────────

    @Test
    @DisplayName("주소 목록 조회")
    void getAddresses() {
        given(addressRepository.findByUserNo(USER_NO))
                .willReturn(List.of(address(1L, USER_NO, true), address(2L, USER_NO, false)));

        List<AddressResponse> result = addressService.getAddresses(USER_NO);

        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("주소 목록 조회 - 빈 목록")
    void getAddresses_empty() {
        given(addressRepository.findByUserNo(USER_NO)).willReturn(List.of());

        List<AddressResponse> result = addressService.getAddresses(USER_NO);

        assertThat(result).isEmpty();
    }

    // ── getAddress ────────────────────────────────────────────────────────

    @Test
    @DisplayName("주소 단건 조회")
    void getAddress() {
        given(addressRepository.findById(ADDRESS_ID))
                .willReturn(Optional.of(address(ADDRESS_ID, USER_NO, false)));

        AddressResponse result = addressService.getAddress(USER_NO, ADDRESS_ID);

        assertThat(result.getAddressId()).isEqualTo(ADDRESS_ID);
        assertThat(result.getRecipient()).isEqualTo("홍길동");
    }

    @Test
    @DisplayName("주소 단건 조회 - 존재하지 않는 주소")
    void getAddress_notFound() {
        given(addressRepository.findById(ADDRESS_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> addressService.getAddress(USER_NO, ADDRESS_ID))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("주소 단건 조회 - 본인 주소가 아닌 경우")
    void getAddress_notOwner() {
        given(addressRepository.findById(ADDRESS_ID))
                .willReturn(Optional.of(address(ADDRESS_ID, 999L, false)));

        assertThatThrownBy(() -> addressService.getAddress(USER_NO, ADDRESS_ID))
                .isInstanceOf(BadRequestException.class);
    }

    // ── createAddress ─────────────────────────────────────────────────────

    @Test
    @DisplayName("주소 생성")
    void createAddress() {
        AddressRequest req = request(false);
        Address saved = address(ADDRESS_ID, USER_NO, false);

        given(addressRepository.save(any(Address.class))).willReturn(saved);

        AddressResponse result = addressService.createAddress(USER_NO, req);

        assertThat(result.getAddressId()).isEqualTo(ADDRESS_ID);
    }

    @Test
    @DisplayName("주소 생성 - 기본 배송지 설정 시 기존 기본 배송지 해제")
    void createAddress_withDefault() {
        AddressRequest req = request(true);
        Address existing = address(1L, USER_NO, true);
        Address saved = address(ADDRESS_ID, USER_NO, true);

        given(addressRepository.findByUserNoAndIsDefaultTrue(USER_NO))
                .willReturn(Optional.of(existing));
        given(addressRepository.save(any(Address.class))).willReturn(saved);

        addressService.createAddress(USER_NO, req);

        assertThat(existing.getIsDefault()).isFalse();
    }

    // ── updateAddress ─────────────────────────────────────────────────────

    @Test
    @DisplayName("주소 수정")
    void updateAddress() {
        Address existing = address(ADDRESS_ID, USER_NO, false);
        AddressRequest req = request(false);

        given(addressRepository.findById(ADDRESS_ID)).willReturn(Optional.of(existing));

        AddressResponse result = addressService.updateAddress(USER_NO, ADDRESS_ID, req);

        assertThat(result.getRecipient()).isEqualTo("김철수");
        assertThat(result.getRoadAddress()).isEqualTo("서울시 서초구 서초대로 2");
    }

    @Test
    @DisplayName("주소 수정 - 기본 배송지로 변경 시 기존 기본 배송지 해제")
    void updateAddress_setDefault() {
        Address existing = address(ADDRESS_ID, USER_NO, false);
        Address oldDefault = address(1L, USER_NO, true);
        AddressRequest req = request(true);

        given(addressRepository.findById(ADDRESS_ID)).willReturn(Optional.of(existing));
        given(addressRepository.findByUserNoAndIsDefaultTrue(USER_NO))
                .willReturn(Optional.of(oldDefault));

        addressService.updateAddress(USER_NO, ADDRESS_ID, req);

        assertThat(oldDefault.getIsDefault()).isFalse();
        assertThat(existing.getIsDefault()).isTrue();
    }

    // ── deleteAddress ─────────────────────────────────────────────────────

    @Test
    @DisplayName("주소 삭제")
    void deleteAddress() {
        Address existing = address(ADDRESS_ID, USER_NO, false);
        given(addressRepository.findById(ADDRESS_ID)).willReturn(Optional.of(existing));

        addressService.deleteAddress(USER_NO, ADDRESS_ID);

        then(addressRepository).should().delete(existing);
    }

    @Test
    @DisplayName("주소 삭제 - 본인 주소가 아닌 경우")
    void deleteAddress_notOwner() {
        given(addressRepository.findById(ADDRESS_ID))
                .willReturn(Optional.of(address(ADDRESS_ID, 999L, false)));

        assertThatThrownBy(() -> addressService.deleteAddress(USER_NO, ADDRESS_ID))
                .isInstanceOf(BadRequestException.class);
    }

    // ── setDefaultAddress ─────────────────────────────────────────────────

    @Test
    @DisplayName("기본 배송지 설정")
    void setDefaultAddress() {
        Address target = address(ADDRESS_ID, USER_NO, false);
        Address oldDefault = address(1L, USER_NO, true);

        given(addressRepository.findById(ADDRESS_ID)).willReturn(Optional.of(target));
        given(addressRepository.findByUserNoAndIsDefaultTrue(USER_NO))
                .willReturn(Optional.of(oldDefault));

        AddressResponse result = addressService.setDefaultAddress(USER_NO, ADDRESS_ID);

        assertThat(result.getIsDefault()).isTrue();
        assertThat(oldDefault.getIsDefault()).isFalse();
    }

    @Test
    @DisplayName("기본 배송지 설정 - 기존 기본 배송지 없는 경우")
    void setDefaultAddress_noPrevious() {
        Address target = address(ADDRESS_ID, USER_NO, false);

        given(addressRepository.findById(ADDRESS_ID)).willReturn(Optional.of(target));
        given(addressRepository.findByUserNoAndIsDefaultTrue(USER_NO))
                .willReturn(Optional.empty());

        AddressResponse result = addressService.setDefaultAddress(USER_NO, ADDRESS_ID);

        assertThat(result.getIsDefault()).isTrue();
    }
}