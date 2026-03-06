package com.ssafy.naeda.domain.user.service;

import com.ssafy.naeda.domain.user.dto.request.LoginRequest;
import com.ssafy.naeda.domain.user.dto.request.SignupRequest;
import com.ssafy.naeda.domain.user.dto.response.LoginResponse;
import com.ssafy.naeda.domain.user.dto.response.SignupResponse;
import com.ssafy.naeda.domain.user.entity.User;
import com.ssafy.naeda.domain.user.repository.UserRepository;
import com.ssafy.naeda.global.exception.AuthenticationFailedException;
import com.ssafy.naeda.global.exception.DuplicateException;
import com.ssafy.naeda.global.exception.SsafyApiException;
import com.ssafy.naeda.global.security.JwtTokenProvider;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RestTemplate restTemplate;

    @Value("${ssafy.api.base-url}")
    private String ssafyBaseUrl;

    @Value("${ssafy.api.key}")
    private String ssafyApiKey;

    @PostConstruct
    void checkApiKey() {
        if (ssafyApiKey == null || ssafyApiKey.isBlank()) {
            log.warn("★★★ [AuthService] SSAFY API KEY가 비어 있습니다! .env 파일을 확인하세요. ★★★");
        } else {
            log.info("★★★ [AuthService] SSAFY API KEY 로드 완료 (길이={}) ★★★", ssafyApiKey.length());
        }
    }

    @Transactional
    public SignupResponse signup(SignupRequest request) {
        // 1. 아이디 중복 검증
        if (userRepository.findByUserId(request.getUserId()).isPresent()) {
            throw new DuplicateException("이미 존재하는 아이디입니다.");
        }

        // 2. 자체 회원가입 (userKey 없이 먼저 저장)
        User user = User.builder()
                .userId(request.getUserId())
                .password(passwordEncoder.encode(request.getPassword()))
                .username(request.getUsername())
                .residentNo(request.getResidentNo())
                .phone(request.getPhone())
                .institutionCode(request.getInstitutionCode())
                .build();

        User saved = userRepository.save(user);

        // 3. SSAFY 금융망 회원가입 → userKey 발급 → DB 업데이트
        String userKey = registerSsafyMember(saved.getUserId());
        saved.updateUserKey(userKey);

        // 4. JWT 토큰 발급 (userKey를 claim에 포함)
        String accessToken = jwtTokenProvider.createAccessToken(saved.getUserId(), userKey);
        String refreshToken = jwtTokenProvider.createRefreshToken(saved.getUserId(), userKey);

        return SignupResponse.of(saved, accessToken, refreshToken);
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request){
        User user = userRepository.findByUserId(request.getUserId())
                .orElseThrow(() -> new AuthenticationFailedException(
                        "아이디 또는 비밀번호가 일치하지 않습니다."
                ));
        //비밀번호 검증
        if(!passwordEncoder.matches(request.getPassword(), user.getPassword())){
            throw new AuthenticationFailedException("아이디 또는 비밀번호가 일치하지 않습니다.");
        }

        //SSAFY 금융망 사용자 검증
        verifySsafyMember(user.getUserId());

        String accessToken = jwtTokenProvider.createAccessToken(user.getUserId(),user.getUserKey());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getUserId(), user.getUserKey());

        return LoginResponse.of(user,accessToken,refreshToken);

    }



    /**
     * SSAFY 금융망 사용자 계정 생성 API 호출.
     * 다른 SSAFY API와 달리 Header 래퍼 없이 apiKey + userId만 전송.
     * 성공 시 userKey를 반환한다.
     */
    @SuppressWarnings("unchecked")
    private String registerSsafyMember(String userId) {
        String url = ssafyBaseUrl + "/member";

        Map<String, Object> body = Map.of(
                "apiKey", ssafyApiKey,
                "userId", userId
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        log.info("[AuthService] SSAFY 회원가입 요청: url={}, userId={}, apiKeyLength={}",
                url, userId, ssafyApiKey != null ? ssafyApiKey.length() : 0);

        try {
            ResponseEntity<Map> responseEntity = restTemplate.exchange(
                    url, HttpMethod.POST, request, Map.class
            );

            Map<String, Object> response = responseEntity.getBody();
            if (response == null || !response.containsKey("userKey")) {
                throw new SsafyApiException("SSAFY_SIGNUP_FAIL", "SSAFY 회원가입 응답에 userKey가 없습니다.");
            }

            String userKey = (String) response.get("userKey");
            log.info("[AuthService] SSAFY 회원가입 성공: userId={}, userKey={}", userId, userKey);
            return userKey;

        } catch (HttpClientErrorException e) {
            // SSAFY API가 4xx 응답을 준 경우 (E4001 등)
            log.error("[AuthService] SSAFY 회원가입 API 오류: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new SsafyApiException("SSAFY_API_ERROR",
                    "SSAFY 회원가입 실패 (" + e.getStatusCode() + "): " + e.getResponseBodyAsString());
        } catch (RestClientException e) {
            log.error("[AuthService] SSAFY 회원가입 네트워크 오류: userId={}", userId, e);
            throw new SsafyApiException("NETWORK_ERROR", "SSAFY 회원가입 API 호출 실패: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void verifySsafyMember(String userId){
        String url = ssafyBaseUrl + "/member/search";

        Map<String,Object> body = Map.of(
                "apiKey",ssafyApiKey,
                "userId",userId
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String,Object>> request = new HttpEntity<>(body,headers);

        log.info("[AuthService] SSAFY 회원 조회 요청: url={}, userId={}", url, userId);

        try {
            ResponseEntity<Map> responseEntity = restTemplate.exchange(
                    url, HttpMethod.POST, request, Map.class
            );

            Map<String, Object> response = responseEntity.getBody();
            if (response == null || !response.containsKey("userKey")) {
                throw new SsafyApiException("SSAFY_VERIFY_FAIL", "SSAFY 회원 조회 응답에 userKey가 없습니다.");
            }

            log.info("[AuthService] SSAFY 회원 조회 성공: userId={}", userId);
        }catch (HttpClientErrorException e) {
            log.error("[AuthService] SSAFY 회원 조회 API 오류: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new SsafyApiException("SSAFY_API_ERROR",
                    "SSAFY 회원 조회 실패 (" + e.getStatusCode() + "): " + e.getResponseBodyAsString());
        } catch (RestClientException e) {
            log.error("[AuthService] SSAFY 회원 조회 네트워크 오류: userId={}", userId, e);
            throw new SsafyApiException("NETWORK_ERROR", "SSAFY 회원 조회 API 호출 실패: " + e.getMessage());
        }

    }
}
