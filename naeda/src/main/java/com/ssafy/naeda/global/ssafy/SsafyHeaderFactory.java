package com.ssafy.naeda.global.ssafy;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * SSAFY 금융 API 공통 Header 생성 팩토리.
 *
 * 모든 SSAFY API 요청은 Body 안에 "Header" 키로 공통부를 포함해야 한다.
 * institutionTransactionUniqueNo: yyyyMMddHHmmss + 6자리 난수 (총 20자리)
 */
@Component
public class SsafyHeaderFactory {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HHmmss");
    private static final DateTimeFormatter UNIQUE_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private static final String INSTITUTION_CODE = "00100";
    private static final String FINTECH_APP_NO   = "001";

    @Value("${ssafy.api.key}")
    private String apiKey;

    /**
     * userKey 없이 호출하는 API용 (관리자/공통 조회 API 등)
     */
    public Map<String, Object> create(String apiName) {
        return buildHeader(apiName, null);
    }

    /**
     * userKey가 필요한 API용 (계좌 조회, 이체 등 사용자 본인 API)
     */
    public Map<String, Object> create(String apiName, String userKey) {
        return buildHeader(apiName, userKey);
    }

    private Map<String, Object> buildHeader(String apiName, String userKey) {
        LocalDateTime now = LocalDateTime.now();

        Map<String, Object> header = new HashMap<>();
        header.put("apiName",                        apiName);
        header.put("transmissionDate",               now.format(DATE_FMT));
        header.put("transmissionTime",               now.format(TIME_FMT));
        header.put("institutionCode",                INSTITUTION_CODE);
        header.put("fintechAppNo",                   FINTECH_APP_NO);
        header.put("apiServiceCode",                 apiName);
        header.put("institutionTransactionUniqueNo", generateUniqueNo(now));
        header.put("apiKey",                         apiKey);

        if (userKey != null) {
            header.put("userKey", userKey);
        }

        return header;
    }

    /**
     * yyyyMMddHHmmss(14) + 6자리 난수 = 20자리
     */
    String generateUniqueNo(LocalDateTime now) {
        int random = ThreadLocalRandom.current().nextInt(100000, 1000000);
        return now.format(UNIQUE_FMT) + random;
    }
}