package com.ssafy.naeda.domain.user.service;

import com.ssafy.naeda.domain.user.dto.request.SignupRequest;
import com.ssafy.naeda.domain.user.dto.response.SignupResponse;
import com.ssafy.naeda.domain.user.entity.User;
import com.ssafy.naeda.domain.user.repository.UserRepository;
import com.ssafy.naeda.global.security.JwtTokenProvider;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @InjectMocks
    private AuthService authService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private com.ssafy.naeda.domain.payment.service.PaymentLimitService paymentLimitService;

    @Test
    @DisplayName("회원가입 시 PIN을 해시하여 저장한다")
    void signup_savesEncodedPin() {
        SignupRequest request = SignupRequest.builder()
                .userId("user-1@ssafy.co.kr")
                .password("password123!")
                .username("홍길동")
                .residentNo("9001011")
                .phone("01012345678")
                .institutionCode("001")
                .pin("123456")
                .build();

        ReflectionTestUtils.setField(authService, "ssafyBaseUrl", "https://ssafy.example.com");
        ReflectionTestUtils.setField(authService, "ssafyApiKey", "api-key");
        ReflectionTestUtils.setField(authService, "refreshTokenExpiry", 604800000L);

        given(userRepository.findByUserId(request.getUserId())).willReturn(Optional.empty());
        given(passwordEncoder.encode("password123!")).willReturn("encoded-password");
        given(passwordEncoder.encode("123456")).willReturn("encoded-pin");
        given(userRepository.save(any(User.class))).willAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "userNo", 1L);
            return saved;
        });
        given(restTemplate.exchange(
                eq("https://ssafy.example.com/member"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(Map.class))
        ).willReturn(ResponseEntity.ok(Map.of("userKey", "user-key-1")));
        given(jwtTokenProvider.createAccessToken(request.getUserId(), "user-key-1")).willReturn("access-token");
        given(jwtTokenProvider.createRefreshToken(request.getUserId(), "user-key-1")).willReturn("refresh-token");
        given(redisTemplate.opsForValue()).willReturn(valueOperations);

        SignupResponse response = authService.signup(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        org.mockito.Mockito.verify(userRepository).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getPinPassword()).isEqualTo("encoded-pin");
        assertThat(response.getUserNo()).isEqualTo(1L);
        assertThat(response.getUserKey()).isEqualTo("user-key-1");
        assertThat(response.isFaceRegistered()).isFalse();
        assertThat(response.isSecondaryAuthEnabled()).isFalse();
    }
}
