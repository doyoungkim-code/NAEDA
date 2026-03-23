package com.ssafy.naeda.domain.store.bootstrap;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NaverStoreEnrichmentClientTest {

    private final NaverStoreEnrichmentClient client = new NaverStoreEnrichmentClient(new ObjectMapper());

    @Test
    @DisplayName("photo 페이지 HTML에서 첫 대표 이미지를 추출한다")
    void extractPlaceImageUrl_prefersOgImage() {
        String html = """
                <html>
                  <head>
                    <meta property="og:image" content="https://search.pstatic.net/common/?autoRotate=true&amp;type=f640_380&amp;src=https%3A%2F%2Fldb-phinf.pstatic.net%2Fimage.jpg" />
                  </head>
                </html>
                """;

        String imageUrl = client.extractPlaceImageUrl(html);

        assertThat(imageUrl)
                .isEqualTo("https://search.pstatic.net/common/?autoRotate=true&type=f640_380&src=https%3A%2F%2Fldb-phinf.pstatic.net%2Fimage.jpg");
    }

    @Test
    @DisplayName("place HTML이 없으면 대표 이미지도 비어 있다")
    void extractPlaceImageUrl_returnsNullWhenHtmlMissing() {
        assertThat(client.extractPlaceImageUrl(null)).isNull();
        assertThat(client.extractPlaceImageUrl("")).isNull();
    }

    @Test
    @DisplayName("네이버 검색 결과 HTML에서 첫 place 후보를 추출한다")
    void extractSearchNaverPlaceCandidate_extractsFirstPlaceCandidate() {
        String html = """
                <html>
                  <body>
                    <div id="place-main-section-root">
                      <section class="place_pcnx_detail">
                        <div id="_title">
                          <a href="https://map.naver.com/p/entry/place/1782635585?placePath=%2Fhome">
                            <span>김치찜은 못참지 인동점</span>
                            <span>한식</span>
                          </a>
                        </div>
                        <div class="XtBbS">오감만족 세트로 입맛 사로잡는 맛집</div>
                        <a
                          data-line-description="경북 구미시 인동중앙로3길 29 영무메트로 107,108호"
                          data-kakaotalk-image-url="https://search.pstatic.net/common/?autoRotate=true&amp;quality=100&amp;type=f640_380&amp;src=https%3A%2F%2Fldb-phinf.pstatic.net%2Fkimchi.jpg">
                        </a>
                      </section>
                    </div>
                  </body>
                </html>
                """;

        NaverStoreEnrichmentClient.SearchNaverPlaceCandidate candidate = client.extractSearchNaverPlaceCandidate(html);

        assertThat(candidate).isNotNull();
        assertThat(candidate.placeId()).isEqualTo("1782635585");
        assertThat(candidate.name()).isEqualTo("김치찜은 못참지 인동점");
        assertThat(candidate.category()).isEqualTo("한식");
        assertThat(candidate.roadAddress()).isEqualTo("경북 구미시 인동중앙로3길 29 영무메트로 107,108호");
        assertThat(candidate.imageUrl())
                .isEqualTo("https://search.pstatic.net/common/?autoRotate=true&quality=100&type=f640_380&src=https%3A%2F%2Fldb-phinf.pstatic.net%2Fkimchi.jpg");
        assertThat(candidate.description()).isEqualTo("오감만족 세트로 입맛 사로잡는 맛집");
    }

    @Test
    @DisplayName("기존 후보에 이미지가 없으면 검색 결과 후보를 우선 사용한다")
    void selectPreferredCandidate_prefersSearchCandidateWhenEarlierCandidatesMissImage() {
        NaverStoreEnrichmentClient.NaverMapPlaceCandidate apiCandidate =
                new NaverStoreEnrichmentClient.NaverMapPlaceCandidate(
                        "100",
                        "API 후보",
                        null,
                        null,
                        "한식",
                        null,
                        null,
                        null,
                        null
                );
        NaverStoreEnrichmentClient.NaverMapPlaceCandidate htmlCandidate =
                new NaverStoreEnrichmentClient.NaverMapPlaceCandidate(
                        "100",
                        "HTML 후보",
                        null,
                        null,
                        "한식",
                        null,
                        null,
                        null,
                        null
                );
        NaverStoreEnrichmentClient.NaverMapPlaceCandidate searchCandidate =
                new NaverStoreEnrichmentClient.NaverMapPlaceCandidate(
                        "1782635585",
                        "김치찜은 못참지 인동점",
                        "경북 구미시 인동중앙로3길 29 영무메트로 107,108호",
                        null,
                        "한식",
                        null,
                        "https://search.pstatic.net/common/?autoRotate=true&quality=100&type=f640_380&src=https%3A%2F%2Fldb-phinf.pstatic.net%2Fkimchi.jpg",
                        "오감만족 세트로 입맛 사로잡는 맛집",
                        null
                );

        Optional<NaverStoreEnrichmentClient.NaverMapPlaceCandidate> selected = client.selectPreferredCandidate(
                Optional.of(apiCandidate),
                Optional.of(htmlCandidate),
                Optional.of(searchCandidate)
        );

        assertThat(selected).contains(searchCandidate);
    }
}
