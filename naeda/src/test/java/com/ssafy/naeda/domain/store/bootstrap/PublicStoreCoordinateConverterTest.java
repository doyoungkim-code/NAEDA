package com.ssafy.naeda.domain.store.bootstrap;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PublicStoreCoordinateConverterTest {

    private final PublicStoreCoordinateConverter converter = new PublicStoreCoordinateConverter();

    @Test
    @DisplayName("검증된 매장 좌표 기준으로 전체 공공 매장 좌표에 동일 보정값을 적용한다")
    void convert_withCalibratedOffset() {
        PublicStoreCoordinateConverter.LatLng latLng = converter.convert(327649.227535413, 290716.785210593);

        assertThat(latLng).isNotNull();
        assertThat(latLng.latitude()).isEqualTo(36.108534);
        assertThat(latLng.longitude()).isEqualTo(128.418517);
    }
}
