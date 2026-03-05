package com.ssafy.naeda.global.ssafy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SsafyHeaderFactoryTest {

    private SsafyHeaderFactory factory;

    @BeforeEach
    void setUp() {
        factory = new SsafyHeaderFactory();
        ReflectionTestUtils.setField(factory, "apiKey", "test-api-key-1234");
    }

    @Test
    @DisplayName("userKey 없이 생성하면 공통 필드가 모두 포함된다")
    void create_withoutUserKey_containsRequiredFields() {
        Map<String, Object> header = factory.create("inquireDemandDepositAccountList");

        assertThat(header)
                .containsKey("apiName")
                .containsKey("transmissionDate")
                .containsKey("transmissionTime")
                .containsKey("institutionCode")
                .containsKey("fintechAppNo")
                .containsKey("apiServiceCode")
                .containsKey("institutionTransactionUniqueNo")
                .containsKey("apiKey")
                .doesNotContainKey("userKey");   // userKey 없어야 함
    }

    @Test
    @DisplayName("userKey와 함께 생성하면 userKey 필드가 포함된다")
    void create_withUserKey_containsUserKey() {
        String userKey = "cf1d49ba-663b-495d-9227-fc2643aa7c5e";

        Map<String, Object> header = factory.create("inquireDemandDepositAccount", userKey);

        assertThat(header)
                .containsEntry("userKey", userKey)
                .containsEntry("apiName", "inquireDemandDepositAccount")
                .containsEntry("apiServiceCode", "inquireDemandDepositAccount");
    }

    @Test
    @DisplayName("apiName과 apiServiceCode는 항상 동일한 값이다")
    void create_apiNameAndServiceCodeAreEqual() {
        String apiName = "updateDemandDepositAccountTransfer";
        Map<String, Object> header = factory.create(apiName, "some-user-key");

        assertThat(header.get("apiName")).isEqualTo(header.get("apiServiceCode"));
    }

    @Test
    @DisplayName("institutionTransactionUniqueNo는 20자리이다")
    void generateUniqueNo_is20Digits() {
        String uniqueNo = factory.generateUniqueNo(LocalDateTime.of(2026, 3, 5, 14, 30, 0));

        assertThat(uniqueNo).hasSize(20);
        assertThat(uniqueNo).startsWith("20260305143000");
    }

    @Test
    @DisplayName("두 번 호출하면 institutionTransactionUniqueNo가 다르다")
    void generateUniqueNo_isDifferentEachCall() {
        LocalDateTime now = LocalDateTime.now();

        // 같은 시각 기준으로 생성해도 난수 부분이 다를 가능성이 매우 높음
        // 극히 드물게 같을 수 있어 100번 반복으로 최소 1번은 달라야 함을 검증
        long distinctCount = java.util.stream.IntStream.range(0, 100)
                .mapToObj(i -> factory.generateUniqueNo(now))
                .distinct()
                .count();

        assertThat(distinctCount).isGreaterThan(1);
    }

    @Test
    @DisplayName("institutionCode는 항상 00100이다")
    void create_institutionCodeIsFixed() {
        Map<String, Object> header = factory.create("someApi");

        assertThat(header.get("institutionCode")).isEqualTo("00100");
        assertThat(header.get("fintechAppNo")).isEqualTo("001");
    }
}