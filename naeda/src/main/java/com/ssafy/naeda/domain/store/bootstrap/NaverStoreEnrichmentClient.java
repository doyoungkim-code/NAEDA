package com.ssafy.naeda.domain.store.bootstrap;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.naeda.domain.store.entity.Store;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.List;
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
            StoreEnrichmentData mapData = findFromNaverMap(store).orElse(StoreEnrichmentData.empty());
            JsonNode bestLocalItem = isConfigured() ? findBestLocalItem(store).orElse(null) : null;

            String description = firstNonBlank(
                    mapData.description(),
                    extractDescription(bestLocalItem, store),
                    fallbackDescription(store)
            );
            String imageUrl = firstNonBlank(
                    mapData.imageUrl(),
                    isConfigured() ? findImageUrl(store).orElse(null) : null,
                    defaultImageUrl(store)
            );

            return Optional.of(new StoreEnrichmentData(
                    trimToNull(imageUrl),
                    trimToNull(description),
                    mapData.rating()
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

    private Optional<StoreEnrichmentData> findFromNaverMap(Store store) {
        try {
            Optional<NaverMapPlaceCandidate> place = findMapPlaceCandidate(store);
            if (place.isEmpty()) {
                return Optional.empty();
            }

            String html = fetchPlacePage(place.get().placeId());
            if (!hasText(html)) {
                return Optional.empty();
            }

            Document document = Jsoup.parse(html);
            String imageUrl = sanitizeUrl(firstNonBlank(
                    metaContent(document, "property", "og:image"),
                    metaContent(document, "name", "twitter:image"),
                    extractByRegex(html, "\\\"imageUrl\\\"\\s*:\\s*\\\"([^\\\"]+)\\\""),
                    extractByRegex(html, "\\\"thumbnail\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"")
            ));
            String description = cleanDescription(firstNonBlank(
                    metaContent(document, "property", "og:description"),
                    extractByRegex(html, "\\\"introduction\\\"\\s*:\\s*\\\"([^\\\"]{5,400})\\\""),
                    extractByRegex(html, "\\\"description\\\"\\s*:\\s*\\\"([^\\\"]{5,400})\\\""),
                    extractByRegex(html, "\\\"desc\\\"\\s*:\\s*\\\"([^\\\"]{5,400})\\\"")
            ));
            Double rating = firstNonNull(
                    extractDouble(html, "\\\"totalRating\\\"\\s*:\\s*([0-9.]+)"),
                    extractDouble(html, "\\\"visitorReviewScore\\\"\\s*:\\s*([0-9.]+)"),
                    extractDouble(html, "\\\"rating\\\"\\s*:\\s*([0-9.]+)")
            );

            if (!hasText(imageUrl) && !hasText(description) && rating == null) {
                return Optional.empty();
            }
            return Optional.of(new StoreEnrichmentData(imageUrl, description, rating));
        } catch (Exception e) {
            log.debug("[StoreEnrichment] 네이버 지도 크롤링 실패 storeId={}, name={}, error={}",
                    store.getStoreId(), store.getStoreName(), e.getMessage());
            return Optional.empty();
        }
    }

    private Optional<NaverMapPlaceCandidate> findMapPlaceCandidate(Store store) {
        Optional<NaverMapPlaceCandidate> apiCandidate = findMapPlaceCandidateByApi(store);
        if (apiCandidate.isPresent()) {
            return apiCandidate;
        }
        return findMapPlaceCandidateByHtml(store);
    }

    private Optional<NaverMapPlaceCandidate> findMapPlaceCandidateByApi(Store store) {
        try {
            URI uri = UriComponentsBuilder
                    .fromUriString("https://map.naver.com/p/api/search/allSearch")
                    .queryParam("query", buildQuery(store))
                    .queryParam("type", "all")
                    .build(true)
                    .toUri();

            String response = crawlClient().get()
                    .uri(uri)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(response);
            JsonNode items = firstArray(
                    root.path("result").path("place").path("list"),
                    root.path("result").path("place").path("items"),
                    root.path("place").path("list"),
                    root.path("items")
            );
            if (items == null || !items.isArray() || items.isEmpty()) {
                return Optional.empty();
            }

            NaverMapPlaceCandidate bestCandidate = null;
            double bestScore = Double.NEGATIVE_INFINITY;
            for (JsonNode item : items) {
                NaverMapPlaceCandidate candidate = new NaverMapPlaceCandidate(
                        firstNonBlank(
                                trimToNull(item.path("id").asText(null)),
                                trimToNull(item.path("placeId").asText(null)),
                                trimToNull(item.path("seq").asText(null))
                        ),
                        firstNonBlank(
                                trimToNull(text(item.path("name").asText(null))),
                                trimToNull(text(item.path("title").asText(null)))
                        ),
                        firstNonBlank(
                                trimToNull(item.path("roadAddress").asText(null)),
                                trimToNull(item.path("roadAddr").asText(null))
                        ),
                        firstNonBlank(
                                trimToNull(item.path("address").asText(null)),
                                trimToNull(item.path("jibunAddress").asText(null))
                        ),
                        firstNonBlank(
                                trimToNull(text(item.path("category").asText(null))),
                                trimToNull(text(item.path("categoryName").asText(null)))
                        ),
                        trimToNull(item.path("telephone").asText(null))
                );
                if (!hasText(candidate.placeId())) {
                    continue;
                }
                double candidateScore = score(candidate, store);
                if (candidateScore > bestScore) {
                    bestScore = candidateScore;
                    bestCandidate = candidate;
                }
            }

            if (bestCandidate == null) {
                return Optional.empty();
            }
            return Optional.of(bestCandidate);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private Optional<NaverMapPlaceCandidate> findMapPlaceCandidateByHtml(Store store) {
        try {
            String encodedQuery = URLEncoder.encode(buildQuery(store), StandardCharsets.UTF_8).replace("+", "%20");
            URI uri = URI.create("https://map.naver.com/p/search/" + encodedQuery);
            String html = crawlClient().get()
                    .uri(uri)
                    .retrieve()
                    .body(String.class);

            String placeId = firstNonBlank(
                    extractByRegex(html, "/place/([0-9]+)"),
                    extractByRegex(html, "\\\"placeId\\\"\\s*:\\s*\\\"?([0-9]+)\\\"?")
            );
            if (!hasText(placeId)) {
                return Optional.empty();
            }

            return Optional.of(new NaverMapPlaceCandidate(
                    placeId,
                    store.getStoreName(),
                    store.getRoadAddress(),
                    store.getNumberAddress(),
                    store.getCategoryName(),
                    store.getPhone()
            ));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private String fetchPlacePage(String placeId) {
        List<String> urls = List.of(
                "https://pcmap.place.naver.com/restaurant/" + placeId + "/home",
                "https://pcmap.place.naver.com/place/" + placeId + "/home",
                "https://m.place.naver.com/restaurant/" + placeId + "/home",
                "https://m.place.naver.com/place/" + placeId + "/home",
                "https://map.naver.com/p/entry/place/" + placeId
        );

        for (String url : urls) {
            try {
                String html = crawlClient().get()
                        .uri(URI.create(url))
                        .retrieve()
                        .body(String.class);
                if (hasText(html)) {
                    return html;
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private Optional<JsonNode> findBestLocalItem(Store store) throws Exception {
        URI uri = UriComponentsBuilder
                .fromUriString("https://openapi.naver.com/v1/search/local.json")
                .queryParam("query", buildQuery(store))
                .queryParam("display", 5)
                .queryParam("start", 1)
                .build(true)
                .toUri();

        String response = apiClient().get()
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

        String response = apiClient().get()
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

    private double score(NaverMapPlaceCandidate candidate, Store store) {
        String title = normalize(candidate.name());
        String storeName = normalize(store.getStoreName());
        String roadAddress = normalize(store.getRoadAddress());
        String itemRoadAddress = normalize(candidate.roadAddress());
        String itemAddress = normalize(candidate.address());
        String phone = normalizeDigits(store.getPhone());
        String itemPhone = normalizeDigits(candidate.phone());
        String category = normalize(candidate.category());

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

    private RestClient apiClient() {
        return RestClient.builder()
                .defaultHeader("X-Naver-Client-Id", clientId)
                .defaultHeader("X-Naver-Client-Secret", clientSecret)
                .defaultHeader("User-Agent", userAgent)
                .build();
    }

    private RestClient crawlClient() {
        return RestClient.builder()
                .defaultHeader("User-Agent", userAgent)
                .defaultHeader("Referer", "https://map.naver.com/p/")
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

    private JsonNode firstArray(JsonNode... candidates) {
        for (JsonNode candidate : candidates) {
            if (candidate != null && candidate.isArray() && !candidate.isEmpty()) {
                return candidate;
            }
        }
        return null;
    }

    private String metaContent(Document document, String attrKey, String attrValue) {
        String content = trimToNull(document.selectFirst("meta[" + attrKey + "=" + attrValue + "]") != null
                ? document.selectFirst("meta[" + attrKey + "=" + attrValue + "]").attr("content")
                : null);
        return hasText(content) ? text(content) : null;
    }

    private String extractByRegex(String text, String regex) {
        if (!hasText(text)) {
            return null;
        }
        Matcher matcher = Pattern.compile(regex).matcher(text);
        if (!matcher.find() || matcher.groupCount() < 1) {
            return null;
        }
        return cleanEscapedText(matcher.group(1));
    }

    private Double extractDouble(String text, String regex) {
        String value = extractByRegex(text, regex);
        if (!hasText(value)) {
            return null;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Double firstNonNull(Double... values) {
        for (Double value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (hasText(value)) {
                return value;
            }
        }
        return null;
    }

    private String sanitizeUrl(String value) {
        String cleaned = trimToNull(value);
        if (!hasText(cleaned)) {
            return null;
        }
        return cleaned.startsWith("http") ? cleaned : null;
    }

    private String cleanDescription(String value) {
        String cleaned = trimToNull(text(cleanEscapedText(value)));
        if (!hasText(cleaned)) {
            return null;
        }
        if (cleaned.length() > 300) {
            return cleaned.substring(0, 300);
        }
        return cleaned;
    }

    private String cleanEscapedText(String value) {
        if (value == null) {
            return null;
        }
        return value
                .replace("\\u003C", "<")
                .replace("\\u003E", ">")
                .replace("\\u002F", "/")
                .replace("\\/", "/")
                .replace("\\n", " ")
                .replace("\\t", " ")
                .replace("\\\"", "\"")
                .replace("&quot;", "\"")
                .trim();
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

    private record NaverMapPlaceCandidate(
            String placeId,
            String name,
            String roadAddress,
            String address,
            String category,
            String phone
    ) {
    }
}
