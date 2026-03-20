package com.ssafy.naeda.domain.rba.controller;

import com.ssafy.naeda.domain.rba.dto.request.PhoneVerifyRequest;
import com.ssafy.naeda.domain.rba.dto.response.PhoneVerifyResponse;
import com.ssafy.naeda.domain.rba.service.PhoneVerificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class PhoneVerificationControllerTest {

    @Mock
    private PhoneVerificationService phoneVerificationService;

    @InjectMocks
    private PhoneVerificationController phoneVerificationController;

    @Test
    @DisplayName("전화번호 가운데 4자리 검증 성공 시 verified=true를 반환한다")
    void verify_returnsSuccess() {
        PhoneVerifyRequest request = PhoneVerifyRequest.builder()
                .userNo(18L)
                .middleDigits("1234")
                .build();
        given(phoneVerificationService.verify(18L, "1234")).willReturn(true);

        ResponseEntity<PhoneVerifyResponse> response = phoneVerificationController.verify(request);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isVerified()).isTrue();
        assertThat(response.getBody().getMessage()).isEqualTo("전화번호 확인이 완료되었습니다.");
        then(phoneVerificationService).should().verify(18L, "1234");
    }

    @Test
    @DisplayName("전화번호 가운데 4자리 검증 실패 시 verified=false를 반환한다")
    void verify_returnsFailure() {
        PhoneVerifyRequest request = PhoneVerifyRequest.builder()
                .userNo(18L)
                .middleDigits("9999")
                .build();
        given(phoneVerificationService.verify(18L, "9999")).willReturn(false);

        ResponseEntity<PhoneVerifyResponse> response = phoneVerificationController.verify(request);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isVerified()).isFalse();
        assertThat(response.getBody().getMessage()).isEqualTo("전화번호 가운데 4자리가 일치하지 않습니다.");
        then(phoneVerificationService).should().verify(18L, "9999");
    }
}
