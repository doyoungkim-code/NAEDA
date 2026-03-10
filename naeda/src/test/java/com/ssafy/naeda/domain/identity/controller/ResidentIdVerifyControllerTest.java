package com.ssafy.naeda.domain.identity.controller;

import com.ssafy.naeda.domain.identity.dto.request.ResidentIdConfirmRequest;
import com.ssafy.naeda.domain.identity.dto.response.ResidentIdExtractResponse;
import com.ssafy.naeda.domain.identity.dto.response.ResidentIdVerifyResponse;
import com.ssafy.naeda.domain.identity.service.ResidentIdVerifyService;
import java.security.Principal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ResidentIdVerifyControllerTest {

    @InjectMocks
    private ResidentIdVerifyController residentIdVerifyController;

    @Mock
    private ResidentIdVerifyService residentIdVerifyService;

    @Mock
    private Principal principal;

    @Test
    @DisplayName("OCR 추출 결과를 반환한다")
    void extract_returnsServiceResponse() {
        given(residentIdVerifyService.extract(any())).willReturn(
                ResidentIdExtractResponse.builder()
                        .documentType("RESIDENT_ID")
                        .documentMatched(true)
                        .name("홍길동")
                        .residentFront6("900101")
                        .residentBackFirst1("1")
                        .provider("mock")
                        .confidence(0.95d)
                        .build()
        );

        ResponseEntity<ResidentIdExtractResponse> response = residentIdVerifyController.extract(
                new MockMultipartFile("image", "card.jpg", "image/jpeg", new byte[]{1, 2, 3})
        );

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getDocumentType()).isEqualTo("RESIDENT_ID");
        assertThat(response.getBody().getName()).isEqualTo("홍길동");
    }

    @Test
    @DisplayName("사용자 확인 결과를 검증 응답으로 반환한다")
    void confirm_returnsServiceResponse() {
        given(principal.getName()).willReturn("user-1");
        given(residentIdVerifyService.confirm(any(), any())).willReturn(
                ResidentIdVerifyResponse.builder()
                        .verified(true)
                        .nameMatched(true)
                        .residentNoMatched(true)
                        .nextAction("CONTINUE_FACEPAY_REGISTRATION")
                        .build()
        );

        ResidentIdConfirmRequest request = new ResidentIdConfirmRequest();
        ResponseEntity<ResidentIdVerifyResponse> response = residentIdVerifyController.confirm(request, principal);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isVerified()).isTrue();
    }
}
