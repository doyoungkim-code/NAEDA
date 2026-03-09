package com.ssafy.naeda.domain.user.service;

import com.ssafy.naeda.domain.user.dto.request.LoginRequest;
import com.ssafy.naeda.domain.user.dto.request.RefreshTokenRequest;
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
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import java.util.Map;
import java.util.concurrent.TimeUnit;


@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RestTemplate restTemplate;
    private final RedisTemplate<String,String> redisTemplate;

    @Value("${ssafy.api.base-url}")
    private String ssafyBaseUrl;

    @Value("${ssafy.api.key}")
    private String ssafyApiKey;

    @Value("${jwt.refresh-token-expiry}")
    private long refreshTokenExpiry;

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

        // 5. RefreshToken을 Redis에 저장 (key : "refresh:{userId}", TTL:7일)
        redisTemplate.opsForValue().set(
                "refresh:" + saved.getUserId(),
                refreshToken,
                refreshTokenExpiry,
                TimeUnit.MILLISECONDS
        );

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

        //RefreshToken을 Redis에 저장(기존 토큰을 덮어쓰기 = 이전 세션 무효화)
        redisTemplate.opsForValue().set(
                "refresh:" + user.getUserId(),
                refreshToken,
                refreshTokenExpiry,
                TimeUnit.MILLISECONDS
        );
//        key: "refresh:hong123@ssafy.co.kr" — userId별로 1개
//        value: refreshToken 문자열
//        timeout: 604800000 (7일, application.yaml의 refresh-token-expiry와 동일)
//        unit: TimeUnit.MILLISECONDS
//        재로그인하면 기존 토큰이 덮어씌워짐 → 이전 기기의 refreshToken은 자동으로 무효화. 이게 "단일 기기 로그인" 정책.

        return LoginResponse.of(user,accessToken,refreshToken);

    }

    public LoginResponse refresh(RefreshTokenRequest request){
        String refreshToken = request.getRefreshToken();

        // 1) RefreshToken JWT 자체 유효성 검증 (서명, 만료)
        if(!jwtTokenProvider.validateToken(refreshToken)){
            throw new AuthenticationFailedException("만료되었거나 유효하지 않은 리프레시 토큰입니다.");
        }

        // 2) 토큰에서 userId 추출
        String userId = jwtTokenProvider.getUserId(refreshToken);

        // 3) Redis에 저장된 RefreshToken과 비교
        String savedToken = redisTemplate.opsForValue().get("refresh:" +userId);
        if(savedToken == null || !savedToken.equals(refreshToken)){
            throw new AuthenticationFailedException("유효하지 않은 리프레시 토큰입니다.");
        }

        // 4) DB에서 사용자 조회
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new AuthenticationFailedException("사용자를 찾을 수 없습니다."));

        // 5) 새 토큰 발급
        String newAccessToken = jwtTokenProvider.createAccessToken(user.getUserId(), user.getUserKey());
        String newRefreshToken = jwtTokenProvider.createRefreshToken(user.getUserId(), user.getUserKey());

        // 6) Redis에 새 refreshToken 저장 (기존 토큰 교체)
        redisTemplate.opsForValue().set(
                "refresh:" + user.getUserId(),
                newRefreshToken,
                refreshTokenExpiry,
                TimeUnit.MILLISECONDS
        );

        return LoginResponse.of(user, newAccessToken, newRefreshToken);

    }

    public void logout(RefreshTokenRequest request, String accessToken){
        String refreshToken = request.getRefreshToken();

        // 1) RefreshToken JWT 유효성 검증
        if(!jwtTokenProvider.validateToken(refreshToken)){
            throw new AuthenticationFailedException("유효하지 않은 리프레시 토큰입니다.");
        }

        // 2)토큰에서 userId 추출
        String userId = jwtTokenProvider.getUserId(refreshToken);

        // 3) Redis에서 RefreshToken 삭제
        redisTemplate.delete("refresh:" +userId);

        // 4. AccessToken 블랙리스트 등록 (남은 만료시간만큼 TTL 설정)
        long remainTime = jwtTokenProvider.getRemainingTime(accessToken);
        if(remainTime > 0){
            redisTemplate.opsForValue().set(
                    "blacklist:" + accessToken,
                    "logout",
                    remainTime,
                    TimeUnit.MILLISECONDS
            );
        }

        log.info("[AuthService] 로그아웃 완료: userId = {}", userId);
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
