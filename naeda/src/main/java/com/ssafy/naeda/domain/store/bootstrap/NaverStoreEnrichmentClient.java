package com.ssafy.naeda.domain.store.bootstrap;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.naeda.domain.store.entity.Store;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
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

    public boolean isConfigured() {
        return hasText(clientId) && hasText(clientSecret);
    }

    public Optional<StoreEnrichmentData> enrich(Store store) {
        try {
            // 1단계: 네이버 지도 크롤링 시도 (실패해도 계속 진행)
            StoreEnrichmentData mapData;
            try {
                mapData = findFromNaverMap(store).orElse(StoreEnrichmentData.empty());
            } catch (Exception e) {
                log.debug("[StoreEnrichment] 네이버 지도 크롤링 실패, API 검색으로 대체: storeId={}", store.getStoreId());
                mapData = StoreEnrichmentData.empty();
            }

            // 2단계: 네이버 오픈 API 검색 (크롤링 실패해도 여기서 이미지/설명 가져옴)
            JsonNode bestLocalItem = null;
            try {
                bestLocalItem = isConfigured() ? findBestLocalItem(store).orElse(null) : null;
            } catch (Exception e) {
                log.debug("[StoreEnrichment] 네이버 로컬 검색 실패: storeId={}", store.getStoreId());
            }

            String description = firstNonBlank(
                    mapData.description(),
                    extractDescription(bestLocalItem),
                    fallbackDescription(store)
            );

            // 3단계: 이미지 URL (크롤링 결과 없으면 이미지 검색 API로 대체)
            String imageUrl = mapData.imageUrl();
            if (!hasText(imageUrl)) {
                try {
                    imageUrl = isConfigured() ? findImageUrl(store).orElse(null) : null;
                } catch (Exception e) {
                    log.debug("[StoreEnrichment] 네이버 이미지 검색 실패: storeId={}", store.getStoreId());
                }
            }

            return Optional.of(new StoreEnrichmentData(
                    trimToNull(imageUrl),
                    trimToNull(description),
                    mapData.rating()
            ));
        } catch (Exception e) {
            log.warn("[StoreEnrichment] 네이버 보강 실패 storeId={}, name={}, error={}",
                    store.getStoreId(), store.getStoreName(), e.getMessage());
            return Optional.of(new StoreEnrichmentData(
                    null,
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

            NaverMapPlaceCandidate candidate = place.get();
            String imageUrl = sanitizeImageUrl(candidate.imageUrl());
            String description = cleanDescription(candidate.description());
            Double rating = candidate.rating();

            String photoHtml = fetchPlacePhotoPage(candidate.placeId());
            imageUrl = firstNonBlank(extractPlaceImageUrl(photoHtml), imageUrl);
            description = firstNonBlank(extractPlaceDescription(photoHtml), description);
            rating = firstNonNull(extractPlaceRating(photoHtml), rating);

            if (!hasText(imageUrl) || !hasText(description) || rating == null) {
                String homeHtml = fetchPlaceHomePage(candidate.placeId());
                imageUrl = firstNonBlank(extractPlaceImageUrl(homeHtml), imageUrl);
                description = firstNonBlank(extractPlaceDescription(homeHtml), description);
                rating = firstNonNull(extractPlaceRating(homeHtml), rating);
            }

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
        Optional<NaverMapPlaceCandidate> htmlCandidate = findMapPlaceCandidateByHtml(store);
        Optional<NaverMapPlaceCandidate> searchPageCandidate = findMapPlaceCandidateBySearchPage(store);
        return selectPreferredCandidate(apiCandidate, htmlCandidate, searchPageCandidate);
    }

    Optional<NaverMapPlaceCandidate> selectPreferredCandidate(
            Optional<NaverMapPlaceCandidate> apiCandidate,
            Optional<NaverMapPlaceCandidate> htmlCandidate,
            Optional<NaverMapPlaceCandidate> searchPageCandidate
    ) {
        if (apiCandidate.isPresent() && hasText(apiCandidate.get().imageUrl())) {
            return apiCandidate;
        }
        if (htmlCandidate.isPresent() && hasText(htmlCandidate.get().imageUrl())) {
            return htmlCandidate;
        }
        if (searchPageCandidate.isPresent()) {
            return searchPageCandidate;
        }
        if (apiCandidate.isPresent()) {
            return apiCandidate;
        }
        return htmlCandidate;
    }

    private Optional<NaverMapPlaceCandidate> findMapPlaceCandidateByApi(Store store) {
        NaverMapPlaceCandidate bestCandidate = null;
        double bestScore = Double.NEGATIVE_INFINITY;

        for (String query : buildQueries(store)) {
            try {
                URI uri = UriComponentsBuilder
                        .fromUriString("https://map.naver.com/p/api/search/allSearch")
                        .queryParam("query", query)
                        .queryParam("type", "all")
                        .build()
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
                    continue;
                }

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
                            trimToNull(item.path("telephone").asText(null)),
                            firstNonBlank(
                                    sanitizeImageUrl(item.path("thumbnail").asText(null)),
                                    sanitizeImageUrl(item.path("thumbnailUrl").asText(null)),
                                    sanitizeImageUrl(item.path("thumUrl").asText(null)),
                                    sanitizeImageUrl(item.path("imageUrl").asText(null))
                            ),
                            firstNonBlank(
                                    cleanDescription(item.path("microReview").asText(null)),
                                    cleanDescription(item.path("description").asText(null)),
                                    cleanDescription(item.path("introduction").asText(null))
                            ),
                            firstNonNull(
                                    nullableDouble(item.path("totalRating")),
                                    nullableDouble(item.path("visitorReviewScore")),
                                    nullableDouble(item.path("starScore")),
                                    nullableDouble(item.path("rating"))
                            )
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
            } catch (Exception ignored) {
            }
        }

        return Optional.ofNullable(bestCandidate);
    }

    private Optional<NaverMapPlaceCandidate> findMapPlaceCandidateByHtml(Store store) {
        for (String query : buildQueries(store)) {
            try {
                String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8).replace("+", "%20");
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
                    continue;
                }

                return Optional.of(new NaverMapPlaceCandidate(
                        placeId,
                        store.getStoreName(),
                        store.getRoadAddress(),
                        store.getNumberAddress(),
                        store.getCategoryName(),
                        store.getPhone(),
                        sanitizeImageUrl(firstNonBlank(
                                extractByRegex(html, "\\\"imageUrl\\\"\\s*:\\s*\\\"([^\\\"]+)\\\""),
                                extractByRegex(html, "\\\"thumbnail\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"")
                        )),
                        firstNonBlank(
                                cleanDescription(extractByRegex(html, "\\\"introduction\\\"\\s*:\\s*\\\"([^\\\"]{5,400})\\\"")),
                                cleanDescription(extractByRegex(html, "\\\"description\\\"\\s*:\\s*\\\"([^\\\"]{5,400})\\\""))
                        ),
                        firstNonNull(
                                extractDouble(html, "\\\"totalRating\\\"\\s*:\\s*([0-9.]+)"),
                                extractDouble(html, "\\\"visitorReviewScore\\\"\\s*:\\s*([0-9.]+)"),
                                extractDouble(html, "\\\"rating\\\"\\s*:\\s*([0-9.]+)")
                        )
                ));
            } catch (Exception ignored) {
            }
        }
        return Optional.empty();
    }

    private Optional<NaverMapPlaceCandidate> findMapPlaceCandidateBySearchPage(Store store) {
        for (String query : buildQueries(store)) {
            try {
                URI uri = UriComponentsBuilder
                        .fromUriString("https://search.naver.com/search.naver")
                        .queryParam("query", query)
                        .build()
                        .toUri();

                String html = crawlClient().get()
                        .uri(uri)
                        .retrieve()
                        .body(String.class);

                SearchNaverPlaceCandidate candidate = extractSearchNaverPlaceCandidate(html);
                if (candidate == null || !hasText(candidate.placeId())) {
                    continue;
                }

                return Optional.of(new NaverMapPlaceCandidate(
                        candidate.placeId(),
                        firstNonBlank(candidate.name(), store.getStoreName()),
                        firstNonBlank(candidate.roadAddress(), store.getRoadAddress()),
                        store.getNumberAddress(),
                        firstNonBlank(candidate.category(), store.getCategoryName()),
                        store.getPhone(),
                        sanitizeImageUrl(candidate.imageUrl()),
                        cleanDescription(candidate.description()),
                        null
                ));
            } catch (Exception ignored) {
            }
        }
        return Optional.empty();
    }

    private String fetchPlacePhotoPage(String placeId) {
        List<String> urls = List.of(
                "https://pcmap.place.naver.com/restaurant/" + placeId + "/photo",
                "https://pcmap.place.naver.com/place/" + placeId + "/photo",
                "https://m.place.naver.com/restaurant/" + placeId + "/photo",
                "https://m.place.naver.com/place/" + placeId + "/photo"
        );

        return fetchFirstAvailablePage(urls);
    }

    private String fetchPlaceHomePage(String placeId) {
        List<String> urls = List.of(
                "https://pcmap.place.naver.com/restaurant/" + placeId + "/home",
                "https://pcmap.place.naver.com/place/" + placeId + "/home",
                "https://m.place.naver.com/restaurant/" + placeId + "/home",
                "https://m.place.naver.com/place/" + placeId + "/home",
                "https://map.naver.com/p/entry/place/" + placeId
        );

        return fetchFirstAvailablePage(urls);
    }

    private String fetchFirstAvailablePage(List<String> urls) {
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

    String extractPlaceImageUrl(String html) {
        if (!hasText(html)) {
            return null;
        }

        Document document = Jsoup.parse(html);
        return firstNonBlank(
                sanitizeImageUrl(metaContent(document, "property", "og:image")),
                sanitizeImageUrl(metaContent(document, "name", "twitter:image")),
                sanitizeImageUrl(extractByRegex(html, "\\\"imageUrl\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"")),
                sanitizeImageUrl(extractByRegex(html, "\\\"thumbnail\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"")),
                sanitizeImageUrl(extractByRegex(html, "\\\"photoUrl\\\"\\s*:\\s*\\\"([^\\\"]+)\\\""))
        );
    }

    SearchNaverPlaceCandidate extractSearchNaverPlaceCandidate(String html) {
        if (!hasText(html)) {
            return null;
        }

        Document document = Jsoup.parse(html);
        Element root = document.selectFirst("#place-main-section-root");
        if (root == null) {
            root = document.selectFirst(".place_pcnx_detail");
        }
        if (root == null) {
            root = document.body();
        }
        if (root == null) {
            return null;
        }

        Element entryAnchor = root.selectFirst("a[href*=map.naver.com/p/entry/place/]");
        if (entryAnchor == null) {
            return null;
        }

        String placeId = extractByRegex(entryAnchor.attr("href"), "/entry/place/([0-9]+)");
        if (!hasText(placeId)) {
            return null;
        }

        Element titleAnchor = root.selectFirst("#_title a[href*=map.naver.com/p/entry/place/]");
        if (titleAnchor == null) {
            titleAnchor = entryAnchor;
        }

        String name = null;
        String category = null;
        if (titleAnchor != null) {
            var spans = titleAnchor.select("span");
            if (!spans.isEmpty()) {
                name = trimToNull(spans.get(0).text());
                if (spans.size() > 1) {
                    category = trimToNull(spans.get(1).text());
                }
            }
            if (!hasText(name)) {
                name = trimToNull(titleAnchor.text());
            }
        }

        Element shareAnchor = root.selectFirst("[data-kakaotalk-image-url], [data-line-description], [data-kakaotalk-description]");
        String imageUrl = shareAnchor == null ? null : firstNonBlank(
                sanitizeImageUrl(shareAnchor.attr("data-kakaotalk-image-url")),
                sanitizeImageUrl(shareAnchor.attr("data-line-image-url"))
        );
        if (!hasText(imageUrl)) {
            Element image = root.selectFirst("img[src*=pstatic.net]");
            imageUrl = image == null ? null : sanitizeImageUrl(image.attr("src"));
        }

        String roadAddress = shareAnchor == null ? null : firstNonBlank(
                trimToNull(shareAnchor.attr("data-line-description")),
                trimToNull(shareAnchor.attr("data-kakaotalk-description"))
        );
        String description = trimToNull(root.select(".XtBbS").text());

        return new SearchNaverPlaceCandidate(placeId, name, roadAddress, category, imageUrl, description);
    }

    String extractPlaceDescription(String html) {
        if (!hasText(html)) {
            return null;
        }

        Document document = Jsoup.parse(html);
        return firstNonBlank(
                cleanDescription(metaContent(document, "property", "og:description")),
                cleanDescription(extractByRegex(html, "\\\"introduction\\\"\\s*:\\s*\\\"([^\\\"]{5,400})\\\"")),
                cleanDescription(extractByRegex(html, "\\\"description\\\"\\s*:\\s*\\\"([^\\\"]{5,400})\\\"")),
                cleanDescription(extractByRegex(html, "\\\"desc\\\"\\s*:\\s*\\\"([^\\\"]{5,400})\\\"")),
                cleanDescription(extractByRegex(html, "\\\"microReview\\\"\\s*:\\s*\\\"([^\\\"]{5,400})\\\"")),
                cleanDescription(extractByRegex(html, "\\\"summary\\\"\\s*:\\s*\\\"([^\\\"]{5,400})\\\"")),
                cleanDescription(extractByRegex(html, "\\\"briefDesc\\\"\\s*:\\s*\\\"([^\\\"]{5,400})\\\""))
        );
    }

    Double extractPlaceRating(String html) {
        if (!hasText(html)) {
            return null;
        }

        return firstNonNull(
                extractDouble(html, "\\\"totalRating\\\"\\s*:\\s*([0-9.]+)"),
                extractDouble(html, "\\\"visitorReviewScore\\\"\\s*:\\s*([0-9.]+)"),
                extractDouble(html, "\\\"starScore\\\"\\s*:\\s*([0-9.]+)"),
                extractDouble(html, "\\\"rating\\\"\\s*:\\s*([0-9.]+)")
        );
    }

    private Optional<JsonNode> findBestLocalItem(Store store) throws Exception {
        JsonNode bestItem = null;
        double bestScore = Double.NEGATIVE_INFINITY;

        for (String query : buildQueries(store)) {
            URI uri = UriComponentsBuilder
                    .fromUriString("https://openapi.naver.com/v1/search/local.json")
                    .queryParam("query", query)
                    .queryParam("display", 5)
                    .queryParam("start", 1)
                    .build()
                    .toUri();

            String response = apiClient().get()
                    .uri(uri)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(response);
            JsonNode items = root.path("items");
            if (!items.isArray() || items.isEmpty()) {
                continue;
            }

            Iterator<JsonNode> iterator = items.elements();
            while (iterator.hasNext()) {
                JsonNode item = iterator.next();
                double itemScore = score(item, store);
                if (itemScore > bestScore) {
                    bestScore = itemScore;
                    bestItem = item;
                }
            }
        }

        if (bestItem == null || bestScore < 1.0d) {
            return Optional.empty();
        }
        return Optional.of(bestItem);
    }

    private Optional<String> findImageUrl(Store store) throws Exception {
        for (String query : buildQueries(store)) {
            URI uri = UriComponentsBuilder
                    .fromUriString("https://openapi.naver.com/v1/search/image")
                    .queryParam("query", query)
                    .queryParam("display", 5)
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
                continue;
            }

            for (JsonNode item : items) {
                String imageUrl = firstNonBlank(
                        sanitizeImageUrl(item.path("thumbnail").asText(null)),
                        sanitizeImageUrl(item.path("link").asText(null))
                );
                if (hasText(imageUrl)) {
                    return Optional.of(imageUrl);
                }
            }
        }
        return Optional.empty();
    }

    private String extractDescription(JsonNode item) {
        if (item != null) {
            String description = cleanDescription(item.path("description").asText(null));
            if (description != null) {
                return description;
            }
        }
        return null;
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

    private List<String> buildQueries(Store store) {
        Set<String> queries = new LinkedHashSet<>();
        addQuery(queries, store.getStoreName());
        addQuery(queries, joinQuery(store.getStoreName(), "구미"));
        addQuery(queries, joinQuery(store.getStoreName(), shortenAddress(store.getRoadAddress())));
        addQuery(queries, joinQuery(store.getStoreName(), shortenAddress(store.getNumberAddress())));
        addQuery(queries, joinQuery(store.getStoreName(), store.getRoadAddress()));
        addQuery(queries, joinQuery(store.getStoreName(), store.getNumberAddress()));
        return List.copyOf(queries);
    }

    private void addQuery(Set<String> queries, String query) {
        String normalized = trimToNull(query);
        if (normalized != null) {
            queries.add(normalized);
        }
    }

    private String joinQuery(String left, String right) {
        String first = trimToNull(left);
        String second = trimToNull(right);
        if (first == null) {
            return second;
        }
        if (second == null) {
            return first;
        }
        return first + " " + second;
    }

    private String shortenAddress(String address) {
        String trimmed = trimToNull(address);
        if (trimmed == null) {
            return null;
        }
        String[] parts = trimmed.split("\\s+");
        if (parts.length <= 4) {
            return trimmed;
        }
        return String.join(" ", parts[0], parts[1], parts[2], parts[3]);
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

    private String sanitizeImageUrl(String value) {
        String cleaned = trimToNull(value);
        if (!hasText(cleaned)) {
            return null;
        }
        String normalized = cleanEscapedText(cleaned);
        if (!hasText(normalized) || !normalized.startsWith("http")) {
            return null;
        }

        String lower = normalized.toLowerCase();
        if (lower.contains("pcmap.place.naver.com")
                || lower.contains("map.naver.com/p/")
                || lower.contains("m.place.naver.com")) {
            return null;
        }
        if (lower.matches(".*\\.(jpg|jpeg|png|gif|webp|bmp|svg)(\\?.*)?$")) {
            return normalized;
        }
        if (lower.contains("phinf.pstatic.net")
                || lower.contains("ldb-phinf.pstatic.net")
                || lower.contains("search.pstatic.net")
                || lower.contains("blogfiles.pstatic.net")
                || lower.contains("postfiles.pstatic.net")) {
            return normalized;
        }
        return null;
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
        return decodeUnicodeEscapes(value)
                .replace("\\u003C", "<")
                .replace("\\u003E", ">")
                .replace("\\u002F", "/")
                .replace("\\u0026", "&")
                .replace("\\/", "/")
                .replace("\\n", " ")
                .replace("\\t", " ")
                .replace("\\\"", "\"")
                .replace("&quot;", "\"")
                .trim();
    }

    private String decodeUnicodeEscapes(String value) {
        Matcher matcher = Pattern.compile("\\\\u([0-9a-fA-F]{4})").matcher(value);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            char decoded = (char) Integer.parseInt(matcher.group(1), 16);
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(String.valueOf(decoded)));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private Double nullableDouble(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        if (node.isNumber()) {
            return node.asDouble();
        }
        if (node.isTextual()) {
            try {
                return Double.parseDouble(node.asText());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
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

    record NaverMapPlaceCandidate(
            String placeId,
            String name,
            String roadAddress,
            String address,
            String category,
            String phone,
            String imageUrl,
            String description,
            Double rating
    ) {
    }

    record SearchNaverPlaceCandidate(
            String placeId,
            String name,
            String roadAddress,
            String category,
            String imageUrl,
            String description
    ) {
    }
}
