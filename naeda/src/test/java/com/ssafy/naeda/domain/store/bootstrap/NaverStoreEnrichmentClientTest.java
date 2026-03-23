package com.ssafy.naeda.domain.store.bootstrap;

import com.fasterxml.jackson.databind.ObjectMapper;
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
}
