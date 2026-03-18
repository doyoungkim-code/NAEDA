package com.ssafy.naeda.domain.store.bootstrap;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.naeda.domain.store.entity.Store;
import java.net.URI;
import java.util.Iterator;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Component
@RequiredArgsConstructor
public class NaverStoreEnrichmentClient {

    private final ObjectMapper objectMapper;

    @Value("${store.enrichment.naver.client-id:}")
    private String clientId;

    @Value("${store.enrichment.naver.client-secret:}")
    private String clientSecret;

    @Value("${store.enrichment.timeout-seconds:3}")
    private int timeoutSeconds;

    @Value("${store.enrichment.user-agent:Mozilla/5.0}")
    private String userAgent;

    @Value("${store.enrichment.defaults.image.restaurant:/images/store/default-restaurant.svg}")
    private String defaultRestaurantImageUrl;

    @Value("${store.enrichment.defaults.image.bakery:/images/store/default-bakery.svg}")
    private String defaultBakeryImageUrl;

    @Value("${store.enrichment.defaults.image.store:/images/store/default-store.svg}")
    private String defaultStoreImageUrl;

    public boolean isConfigured() {
        return hasText(clientId) && hasText(clientSecret);
    }

    public Optional<StoreEnrichmentData> enrich(Store store) {
        try {
            JsonNode bestLocalItem = isConfigured() ? findBestLocalItem(store).orElse(null) : null;
            String description = extractDescription(bestLocalItem, store);
            String imageUrl = isConfigured()
                    ? findImageUrl(store).orElseGet(() -> defaultImageUrl(store))
                    : defaultImageUrl(store);

            return Optional.of(new StoreEnrichmentData(
                    hasText(imageUrl) ? imageUrl : null,
                    hasText(description) ? description : null,
                    null
            ));
        } catch (Exception e) {
            log.warn("[StoreEnrichment] 네이버 보강 실패 storeId={}, name={}, error={}",
                    store.getStoreId(), store.getStoreName(), e.getMessage());
            return Optional.of(new StoreEnrichmentData(
                    defaultImageUrl(store),
                    fallbackDescription(store),
                    null
            ));
        }
    }

    private Optional<JsonNode> findBestLocalItem(Store store) throws Exception {
        URI uri = UriComponentsBuilder
                .fromUriString("https://openapi.naver.com/v1/search/local.json")
                .queryParam("query", buildQuery(store))
                .queryParam("display", 5)
                .queryParam("start", 1)
                .build(true)
                .toUri();

        String response = restClient().get()
                .uri(uri)
                .retrieve()
                .body(String.class);

        JsonNode root = objectMapper.readTree(response);
        JsonNode items = root.path("items");
        if (!items.isArray() || items.isEmpty()) {
            return Optional.empty();
        }

        JsonNode bestItem = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        Iterator<JsonNode> iterator = items.elements();
        while (iterator.hasNext()) {
            JsonNode item = iterator.next();
            double itemScore = score(item, store);
            if (itemScore > bestScore) {
                bestScore = itemScore;
                bestItem = item;
            }
        }

        if (bestItem == null || bestScore < 1.0d) {
            return Optional.empty();
        }
        return Optional.of(bestItem);
    }

    private Optional<String> findImageUrl(Store store) throws Exception {
        URI uri = UriComponentsBuilder
                .fromUriString("https://openapi.naver.com/v1/search/image")
                .queryParam("query", buildQuery(store))
                .queryParam("display", 1)
                .queryParam("start", 1)
                .queryParam("sort", "sim")
                .build(true)
                .toUri();

        String response = restClient().get()
                .uri(uri)
                .retrieve()
                .body(String.class);

        JsonNode root = objectMapper.readTree(response);
        JsonNode items = root.path("items");
        if (!items.isArray() || items.isEmpty()) {
            return Optional.empty();
        }

        JsonNode first = items.get(0);
        String thumbnail = trimToNull(first.path("thumbnail").asText(null));
        if (thumbnail != null) {
            return Optional.of(thumbnail);
        }
        return Optional.ofNullable(trimToNull(first.path("link").asText(null)));
    }

    private String extractDescription(JsonNode item, Store store) {
        if (item != null) {
            String description = trimToNull(text(item.path("description").asText(null)));
            if (description != null) {
                return description;
            }

            String category = trimToNull(text(item.path("category").asText(null)));
            if (category != null) {
                return category + " 매장입니다.";
            }
        }

        return fallbackDescription(store);
    }

    private double score(JsonNode item, Store store) {
        String title = normalize(text(item.path("title").asText("")));
        String storeName = normalize(store.getStoreName());
        String roadAddress = normalize(store.getRoadAddress());
        String itemRoadAddress = normalize(item.path("roadAddress").asText(""));
        String itemAddress = normalize(item.path("address").asText(""));
        String phone = normalizeDigits(store.getPhone());
        String itemPhone = normalizeDigits(item.path("telephone").asText(""));
        String category = normalize(text(item.path("category").asText("")));

        double score = 0.0d;
        if (hasText(title) && (title.contains(storeName) || storeName.contains(title))) {
            score += 1.5d;
        }
        if (hasText(roadAddress) && (itemRoadAddress.contains(roadAddress) || roadAddress.contains(itemRoadAddress))) {
            score += 1.2d;
        }
        if (hasText(roadAddress) && (itemAddress.contains(roadAddress) || roadAddress.contains(itemAddress))) {
            score += 0.7d;
        }
        if (hasText(phone) && hasText(itemPhone) && (phone.endsWith(itemPhone) || itemPhone.endsWith(phone))) {
            score += 0.5d;
        }
        if (hasText(store.getCategoryName()) && category.contains(normalize(store.getCategoryName()))) {
            score += 0.2d;
        }
        return score;
    }

    private RestClient restClient() {
        return RestClient.builder()
                .defaultHeader("X-Naver-Client-Id", clientId)
                .defaultHeader("X-Naver-Client-Secret", clientSecret)
                .build();
    }

    private String buildQuery(Store store) {
        StringBuilder query = new StringBuilder(store.getStoreName());
        if (hasText(store.getRoadAddress())) {
            query.append(' ').append(store.getRoadAddress());
        } else if (hasText(store.getNumberAddress())) {
            query.append(' ').append(store.getNumberAddress());
        }
        return query.toString();
    }

    private String defaultImageUrl(Store store) {
        if ("PUBLIC_BAKERY".equals(store.getCategoryId())) {
            return defaultBakeryImageUrl;
        }
        if ("PUBLIC_RESTAURANT".equals(store.getCategoryId())) {
            return defaultRestaurantImageUrl;
        }
        return defaultStoreImageUrl;
    }

    private String fallbackDescription(Store store) {
        String categoryName = trimToNull(store.getCategoryName());
        String storeName = trimToNull(store.getStoreName());

        if (categoryName != null && storeName != null) {
            return storeName + "는 구미 지역 " + categoryName + " 매장입니다.";
        }
        if (categoryName != null) {
            return "구미 지역 " + categoryName + " 매장입니다.";
        }
        if ("PUBLIC_BAKERY".equals(store.getCategoryId())) {
            return "구미 지역 제과점 매장입니다.";
        }
        if ("PUBLIC_RESTAURANT".equals(store.getCategoryId())) {
            return "구미 지역 음식점 매장입니다.";
        }
        return "구미 지역 매장입니다.";
    }

    private String text(String value) {
        return value == null ? null : Jsoup.parse(value).text();
    }

    private String normalize(String value) {
        return hasText(value) ? value.replaceAll("[^가-힣0-9A-Za-z]", "").toLowerCase() : "";
    }

    private String normalizeDigits(String value) {
        return hasText(value) ? value.replaceAll("\\D", "") : "";
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
