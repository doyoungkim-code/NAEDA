package com.ssafy.naeda.domain.identity.service;

import com.ssafy.naeda.domain.face.service.FaceInputValidator;
import com.ssafy.naeda.domain.identity.client.ResidentIdOcrClient;
import com.ssafy.naeda.domain.identity.client.dto.ResidentIdOcrResponse;
import com.ssafy.naeda.domain.identity.dto.request.ResidentIdConfirmRequest;
import com.ssafy.naeda.domain.identity.dto.response.ResidentIdExtractResponse;
import com.ssafy.naeda.domain.identity.dto.response.ResidentIdVerifyResponse;
import com.ssafy.naeda.domain.user.entity.User;
import com.ssafy.naeda.domain.user.repository.UserRepository;
import java.lang.reflect.Field;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ResidentIdVerifyServiceTest {

    @InjectMocks
    private ResidentIdVerifyService residentIdVerifyService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ResidentIdOcrClient residentIdOcrClient;

    @Mock
    private FaceInputValidator faceInputValidator;

    @Test
    @DisplayName("OCR 추출 결과를 그대로 확인 화면에 전달한다")
    void extract_success() throws Exception {
        ResidentIdOcrResponse response = new ResidentIdOcrResponse();
        setField(response, "name", "홍길동");
        setField(response, "residentFront6", "900101");
        setField(response, "residentBackFirst1", "1");
        setField(response, "provider", "mock");
        given(residentIdOcrClient.extractResidentId(any())).willReturn(response);

        ResidentIdExtractResponse result = residentIdVerifyService.extract(
                new MockMultipartFile("image", "card.jpg", "image/jpeg", new byte[]{1, 2, 3})
        );

        assertThat(result.getName()).isEqualTo("홍길동");
        assertThat(result.getResidentFront6()).isEqualTo("900101");
        assertThat(result.getResidentBackFirst1()).isEqualTo("1");
        assertThat(result.getProvider()).isEqualTo("mock");
    }

    @Test
    @DisplayName("이름과 주민번호 7자리가 모두 일치하면 확인 성공")
    void confirm_success() {
        given(userRepository.findByUserId("user-1")).willReturn(Optional.of(
                User.builder()
                        .userId("user-1")
                        .username("홍길동")
                        .residentNo("9001011")
                        .password("pw")
                        .phone("01012345678")
                        .institutionCode("001")
                        .build()
        ));

        ResidentIdConfirmRequest request = request("홍길동", "900101", "1");

        ResidentIdVerifyResponse result = residentIdVerifyService.confirm("user-1", request);

        assertThat(result.isVerified()).isTrue();
        assertThat(result.isNameMatched()).isTrue();
        assertThat(result.isResidentNoMatched()).isTrue();
        assertThat(result.getNextAction()).isEqualTo("CONTINUE_FACEPAY_REGISTRATION");
    }

    @Test
    @DisplayName("주민번호 7자리가 불일치하면 재입력 응답")
    void confirm_residentNoMismatch() {
        given(userRepository.findByUserId("user-1")).willReturn(Optional.of(
                User.builder()
                        .userId("user-1")
                        .username("홍길동")
                        .residentNo("9001011")
                        .password("pw")
                        .phone("01012345678")
                        .institutionCode("001")
                        .build()
        ));

        ResidentIdConfirmRequest request = request("홍길동", "900101", "2");

        ResidentIdVerifyResponse result = residentIdVerifyService.confirm("user-1", request);

        assertThat(result.isVerified()).isFalse();
        assertThat(result.isNameMatched()).isTrue();
        assertThat(result.isResidentNoMatched()).isFalse();
        assertThat(result.getNextAction()).isEqualTo("RETRY_CONFIRM");
    }

    private static ResidentIdConfirmRequest request(String name, String front6, String back1) {
        ResidentIdConfirmRequest request = new ResidentIdConfirmRequest();
        try {
            setField(request, "name", name);
            setField(request, "residentFront6", front6);
            setField(request, "residentBackFirst1", back1);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return request;
    }

    private static void setField(Object target, String name, String value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
