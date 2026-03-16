package com.ssafy.naeda.domain.store.bootstrap;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.naeda.domain.store.entity.Store;
import java.net.URI;
import java.time.Duration;
import java.util.Iterator;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Component
@RequiredArgsConstructor
public class NaverStoreEnrichmentClient {

    private static final Pattern[] RATING_PATTERNS = new Pattern[]{
            Pattern.compile("\"averageRating\"\\s*:\\s*\"?([0-9]+(?:\\.[0-9]+)?)\"?"),
            Pattern.compile("\"rating\"\\s*:\\s*\"?([0-9]+(?:\\.[0-9]+)?)\"?"),
            Pattern.compile("\"visitorReviewScore\"\\s*:\\s*\"?([0-9]+(?:\\.[0-9]+)?)\"?"),
            Pattern.compile("평점[^0-9]*([0-9]+(?:\\.[0-9]+)?)")
    };

    private final ObjectMapper objectMapper;

    @Value("${store.enrichment.naver.client-id:}")
    private String clientId;

    @Value("${store.enrichment.naver.client-secret:}")
    private String clientSecret;

    @Value("${store.enrichment.timeout-seconds:3}")
    private int timeoutSeconds;

    @Value("${store.enrichment.user-agent:Mozilla/5.0}")
    private String userAgent;

    public boolean isConfigured() {
        return hasText(clientId) && hasText(clientSecret);
    }

    public Optional<StoreEnrichmentData> enrich(Store store) {
        if (!isConfigured()) {
            return Optional.empty();
        }

        try {
            JsonNode bestLocalItem = findBestLocalItem(store).orElse(null);
            String description = extractDescription(bestLocalItem).orElse(null);
            String imageUrl = findImageUrl(store).orElse(null);
            Double rating = extractRating(bestLocalItem).orElse(null);

            if (!hasText(imageUrl) && !hasText(description) && rating == null) {
                return Optional.empty();
            }
            return Optional.of(new StoreEnrichmentData(
                    hasText(imageUrl) ? imageUrl : null,
                    hasText(description) ? description : null,
                    rating
            ));
        } catch (Exception e) {
            log.warn("[StoreEnrichment] 네이버 보강 실패 storeId={}, name={}, error={}",
                    store.getStoreId(), store.getStoreName(), e.getMessage());
            return Optional.empty();
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

    private Optional<String> extractDescription(JsonNode item) {
        if (item == null) {
            return Optional.empty();
        }
        String description = trimToNull(text(item.path("description").asText(null)));
        if (description != null) {
            return Optional.of(description);
        }
        return Optional.ofNullable(trimToNull(text(item.path("category").asText(null))));
    }

    private Optional<Double> extractRating(JsonNode item) {
        if (item == null) {
            return Optional.empty();
        }
        String link = trimToNull(item.path("link").asText(null));
        if (!hasText(link)) {
            return Optional.empty();
        }

        try {
            Document document = Jsoup.connect(link)
                    .userAgent(userAgent)
                    .timeout(Math.toIntExact(Duration.ofSeconds(timeoutSeconds).toMillis()))
                    .get();
            String html = document.html();
            for (Pattern pattern : RATING_PATTERNS) {
                Matcher matcher = pattern.matcher(html);
                if (matcher.find()) {
                    double rating = Double.parseDouble(matcher.group(1));
                    if (rating >= 0.0d && rating <= 5.0d) {
                        return Optional.of(rating);
                    }
                }
            }
        } catch (Exception e) {
            log.debug("[StoreEnrichment] 평점 추출 실패 link={}, error={}", link, e.getMessage());
        }
        return Optional.empty();
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
