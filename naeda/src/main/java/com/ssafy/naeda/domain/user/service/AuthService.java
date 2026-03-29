package com.ssafy.naeda.domain.user.service;

import com.ssafy.naeda.domain.account.entity.Account;
import com.ssafy.naeda.domain.account.repository.AccountRepository;
import com.ssafy.naeda.domain.address.repository.AddressRepository;
import com.ssafy.naeda.domain.card.repository.CreditCardRepository;
import com.ssafy.naeda.domain.card.repository.DebitCardRepository;
import com.ssafy.naeda.domain.face.repository.FaceEmbeddingRepository;
import com.ssafy.naeda.domain.fds.repository.FdsLogRepository;
import com.ssafy.naeda.domain.notification.repository.NotificationRepository;
import com.ssafy.naeda.domain.notificationsetting.repository.NotificationSettingRepository;
import com.ssafy.naeda.domain.pay.entity.MethodType;
import com.ssafy.naeda.domain.pay.entity.PayMethod;
import com.ssafy.naeda.domain.pay.repository.PayLimitRepository;
import com.ssafy.naeda.domain.pay.repository.PayMethodRepository;
import com.ssafy.naeda.domain.pay.repository.PayTransactionRepository;
import com.ssafy.naeda.domain.pay.service.PayLimitService;
import com.ssafy.naeda.domain.point.repository.PointHistoryRepository;
import com.ssafy.naeda.domain.point.repository.PointOrderRepository;
import com.ssafy.naeda.domain.point.repository.PointWalletRepository;
import com.ssafy.naeda.domain.report.repository.ConsumptionReportRepository;
import com.ssafy.naeda.domain.transaction.repository.TransactionLogRepository;
import com.ssafy.naeda.global.ssafy.SsafyApiClient;
import com.ssafy.naeda.global.ssafy.SsafyHeaderFactory;
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
import com.ssafy.naeda.domain.user.dto.response.PasswordResetCodeResponse;
import com.ssafy.naeda.domain.user.dto.response.PasswordResetVerifyResponse;
import com.ssafy.naeda.global.exception.BadRequestException;
import com.ssafy.naeda.global.exception.NotFoundException;

import java.security.SecureRandom;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
    private final PayLimitService paymentLimitService;
    private final AccountRepository accountRepository;
    private final PayMethodRepository payMethodRepository;
    private final SsafyApiClient ssafyApiClient;
    private final SsafyHeaderFactory ssafyHeaderFactory;
    private final FdsLogRepository fdsLogRepository;
    private final PayTransactionRepository payTransactionRepository;
    private final PayLimitRepository payLimitRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationSettingRepository notificationSettingRepository;
    private final AddressRepository addressRepository;
    private final CreditCardRepository creditCardRepository;
    private final DebitCardRepository debitCardRepository;
    private final FaceEmbeddingRepository faceEmbeddingRepository;
    private final PointWalletRepository pointWalletRepository;
    private final PointOrderRepository pointOrderRepository;
    private final PointHistoryRepository pointHistoryRepository;
    private final ConsumptionReportRepository consumptionReportRepository;
    private final TransactionLogRepository transactionLogRepository;

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

    public boolean isUserIdDuplicate(String userId) {
        return userRepository.findByUserId(userId).isPresent();
    }

    public boolean isPhoneDuplicate(String phone) {
        return userRepository.findByPhone(phone).isPresent();
    }

    @Transactional
    public SignupResponse signup(SignupRequest request) {
        // 1. 아이디 중복 검증
        if (userRepository.findByUserId(request.getUserId()).isPresent()) {
            throw new DuplicateException("이미 존재하는 아이디입니다.");
        }

        // 1-1. 전화번호 중복 검증
        if (userRepository.findByPhone(request.getPhone()).isPresent()) {
            throw new DuplicateException("이미 등록된 전화번호입니다.");
        }

        // 2. 자체 회원가입 (userKey 없이 먼저 저장)
        User user = User.builder()
                .userId(request.getUserId())
                .password(passwordEncoder.encode(request.getPassword()))
                .username(request.getUsername())
                .residentNo(request.getResidentNo())
                .phone(request.getPhone())
                .institutionCode(request.getInstitutionCode())
                .pinPassword(passwordEncoder.encode(request.getPin()))
                .build();

        User saved = userRepository.save(user);

        // 3. SSAFY 금융망 회원가입 → userKey 발급 → DB 업데이트
        String userKey = registerSsafyMember(saved.getUserId());
        saved.updateUserKey(userKey);

        // 4. 결제 한도 기본값 자동 생성
        paymentLimitService.createDefaultLimit(saved.getUserNo());

        // 5. SSAFY 수시입출금 계좌 자동 생성 + Account/PayMethod 저장
        createDefaultAccount(saved.getUserNo(), saved.getUserKey());

        // 6. JWT 토큰 발급 (userKey를 claim에 포함)
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

    /**
     * SSAFY 수시입출금 계좌를 생성하고, Account + PayMethod(ACCOUNT)를 자동 등록한다.
     * 회원가입 직후 호출되며, 실패해도 회원가입 자체는 롤백하지 않는다.
     */
    @SuppressWarnings("unchecked")
    private void createDefaultAccount(Long userNo, String userKey) {
        try {
            // 1. SSAFY 계좌 생성 API 호출
            Map<String, Object> header = ssafyHeaderFactory.create("createDemandDepositAccount", userKey);
            Map<String, Object> body = ssafyApiClient.buildBody(header,
                    "accountTypeUniqueNo", "001-1-4e9b6cd7a26a45"  // SSAFY 수시입출금 상품 고유번호
            );

            Map<String, Object> response = ssafyApiClient.post(
                    "/edu/demandDeposit/createDemandDepositAccount", body);

            Map<String, Object> rec = (Map<String, Object>) response.get("REC");
            if (rec == null) {
                log.warn("[AuthService] 계좌 생성 응답에 REC이 없습니다. userNo={}", userNo);
                return;
            }

            String accountNo = (String) rec.get("accountNo");
            String bankCode = (String) rec.getOrDefault("bankCode", "001");
            String bankName = (String) rec.getOrDefault("bankName", "한국은행");
            String accountName = (String) rec.getOrDefault("accountName", "수시입출금");

            // 2. Account 엔티티 저장
            Account account = accountRepository.save(Account.builder()
                    .userNo(userNo)
                    .bankCode(bankCode)
                    .bankName(bankName)
                    .accountNo(accountNo)
                    .accountName(accountName)
                    .build());

            // 3. PayMethod(ACCOUNT 타입) 자동 생성
            PayMethod payMethod = PayMethod.builder()
                    .userNo(userNo)
                    .methodType(MethodType.ACCOUNT)
                    .accountId(account.getAccountId())
                    .build();
            payMethodRepository.save(payMethod);

            log.info("[AuthService] 기본 계좌 + 결제수단 생성 완료: userNo={}, accountNo={}", userNo, accountNo);

            // 4. 초기 잔액 입금 (1,000,000원)
            depositInitialBalance(accountNo, userKey);

        } catch (Exception e) {
            log.error("[AuthService] 기본 계좌 생성 실패 (회원가입은 정상 처리됨): userNo={}", userNo, e);
        }
    }

    /**
     * SSAFY 계좌에 초기 잔액을 입금한다.
     */
    @SuppressWarnings("unchecked")
    private void depositInitialBalance(String accountNo, String userKey) {
        try {
            Map<String, Object> header = ssafyHeaderFactory.create("updateDemandDepositAccountDeposit", userKey);
            Map<String, Object> body = ssafyApiClient.buildBody(header,
                    "accountNo", accountNo,
                    "transactionBalance", "1000000",
                    "transactionSummary", "회원가입 초기 입금"
            );

            ssafyApiClient.post("/edu/demandDeposit/updateDemandDepositAccountDeposit", body);
            log.info("[AuthService] 초기 잔액 입금 완료: accountNo={}, amount=1,000,000", accountNo);

        } catch (Exception e) {
            log.error("[AuthService] 초기 잔액 입금 실패 (계좌는 정상 생성됨): accountNo={}", accountNo, e);
        }
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
        // 5. FCM 토큰 제거 (로그아웃 후 푸시 알림 차단)
        User user = userRepository.findByUserId(userId)
                .orElse(null);
        if (user != null) {
            user.clearFcmToken();
            userRepository.save(user);
        }


        log.info("[AuthService] 로그아웃 완료: userId = {}", userId);
    }


    public String getUserIdFromToken(String token) {
        if (!jwtTokenProvider.validateToken(token)) {
            throw new AuthenticationFailedException("유효하지 않은 토큰입니다.");
        }
        return jwtTokenProvider.getUserId(token);
    }

    @Transactional
    public void withdraw(String userId, String accessToken) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new AuthenticationFailedException("사용자를 찾을 수 없습니다."));

        Long userNo = user.getUserNo();

        // FK 의존 순서에 따라 자식 테이블부터 삭제
        // 1. FDS 로그 (pay_transaction 참조)
        fdsLogRepository.deleteByUserNo(userNo);

        // 2. 결제 트랜잭션
        payTransactionRepository.deleteByUserNo(userNo);

        // 3. 결제수단, 결제한도
        payMethodRepository.deleteByUserNo(userNo);
        payLimitRepository.deleteByUserNo(userNo);

        // 4. 포인트 (history → wallet 순서)
        pointWalletRepository.findByUserNo(userNo).ifPresent(wallet ->
                pointHistoryRepository.deleteByWalletId(wallet.getWalletId())
        );
        pointOrderRepository.deleteByUserNo(userNo);
        pointWalletRepository.deleteByUserNo(userNo);

        // 5. 거래내역 (account 참조)
        List<Account> accounts = accountRepository.findByUserNo(userNo);
        for (Account account : accounts) {
            transactionLogRepository.deleteByAccountId(account.getAccountId());
        }

        // 6. 카드 (account 참조 가능)
        creditCardRepository.deleteByUserNo(userNo);
        debitCardRepository.deleteByUserNo(userNo);

        // 7. 계좌
        accountRepository.deleteByUserNo(userNo);

        // 8. 주소, 알림, 알림설정, 소비리포트
        addressRepository.deleteByUserNo(userNo);
        notificationRepository.deleteByUserNo(userNo);
        notificationSettingRepository.deleteByUserNo(userNo);
        consumptionReportRepository.deleteByUserNo(userNo);

        // 9. 얼굴 임베딩 (userId 기반)
        faceEmbeddingRepository.deleteByUserId(userId);

        // 10. 사용자 삭제
        userRepository.delete(user);

        // 11. Redis 정리 (refreshToken 삭제 + accessToken 블랙리스트)
        redisTemplate.delete("refresh:" + userId);
        long remainTime = jwtTokenProvider.getRemainingTime(accessToken);
        if (remainTime > 0) {
            redisTemplate.opsForValue().set(
                    "blacklist:" + accessToken,
                    "withdrawn",
                    remainTime,
                    TimeUnit.MILLISECONDS
            );
        }

        log.info("[AuthService] 회원탈퇴 완료: userId={}, userNo={}", userId, userNo);
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

    private static final int RESET_CODE_TTL_SECONDS = 300; // 5분
    private static final int RESET_TOKEN_TTL_SECONDS = 600; // 10분
    private static final String RESET_CODE_PREFIX = "pwd-reset:";
    private static final String RESET_VERIFIED_PREFIX = "pwd-reset-verified:";

    public PasswordResetCodeResponse requestPasswordReset(String phone) {
        userRepository.findByPhone(phone)
                .orElseThrow(() -> new NotFoundException("등록되지 않은 휴대폰번호입니다."));

        String code = String.format("%06d", new SecureRandom().nextInt(1_000_000));

        redisTemplate.opsForValue().set(
                RESET_CODE_PREFIX + phone,
                code,
                RESET_CODE_TTL_SECONDS,
                TimeUnit.SECONDS
        );

        log.info("[AuthService] 비밀번호 재설정 코드 발급: phone={}", phone);
        return PasswordResetCodeResponse.of(code, RESET_CODE_TTL_SECONDS);
    }

    public PasswordResetVerifyResponse verifyPasswordResetCode(String phone, String code) {
        String savedCode = redisTemplate.opsForValue().get(RESET_CODE_PREFIX + phone);

        if (savedCode == null) {
            throw new BadRequestException("인증 코드가 만료되었거나 존재하지 않습니다.");
        }
        if (!savedCode.equals(code)) {
            throw new BadRequestException("인증 코드가 일치하지 않습니다.");
        }

        redisTemplate.delete(RESET_CODE_PREFIX + phone);

        String token = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(
                RESET_VERIFIED_PREFIX + token,
                phone,
                RESET_TOKEN_TTL_SECONDS,
                TimeUnit.SECONDS
        );

        log.info("[AuthService] 비밀번호 재설정 코드 검증 성공: phone={}", phone);
        return PasswordResetVerifyResponse.of(token);
    }

    @Transactional
    public void confirmPasswordReset(String token, String newPassword) {
        String phone = redisTemplate.opsForValue().get(RESET_VERIFIED_PREFIX + token);

        if (phone == null) {
            throw new BadRequestException("유효하지 않거나 만료된 토큰입니다.");
        }

        redisTemplate.delete(RESET_VERIFIED_PREFIX + token);

        User user = userRepository.findByPhone(phone)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));

        user.updatePassword(passwordEncoder.encode(newPassword));

        log.info("[AuthService] 비밀번호 재설정 완료: phone={}", phone);
    }
}
