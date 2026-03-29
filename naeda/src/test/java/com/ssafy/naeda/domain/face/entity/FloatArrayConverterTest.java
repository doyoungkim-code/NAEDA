package com.ssafy.naeda.domain.face.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class FloatArrayConverterTest {

    private final FloatArrayConverter converter = new FloatArrayConverter();
    private final EmbeddingEncryptor encryptor = mock(EmbeddingEncryptor.class);

    FloatArrayConverterTest() {
        ReflectionTestUtils.setField(converter, "encryptor", encryptor);
    }

    @Test
    @DisplayName("환경키가 없으면 float[]를 JSON 문자열로 저장한다")
    void convertToDatabaseColumn_withoutKey_returnsJson() {
        float[] input = new float[]{0.12f, -0.03f, 0.9f};
        given(encryptor.encrypt("[0.12,-0.03,0.9]")).willReturn("[0.12,-0.03,0.9]");

        String dbValue = converter.convertToDatabaseColumn(input);

        assertThat(dbValue).isEqualTo("[0.12,-0.03,0.9]");
    }

    @Test
    @DisplayName("레거시 JSON 문자열은 float[]로 복원된다")
    void convertToEntityAttribute_legacyJson_returnsFloatArray() {
        float[] result = converter.convertToEntityAttribute("[0.12,-0.03,0.9]");

        assertThat(result).containsExactly(0.12f, -0.03f, 0.9f);
    }

    @Test
    @DisplayName("빈 JSON 배열은 빈 float[]로 변환된다")
    void convertToEntityAttribute_emptyJson_returnsEmptyArray() {
        float[] result = converter.convertToEntityAttribute("[]");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("암호화 데이터인데 키가 없으면 복호화 예외를 던진다")
    void convertToEntityAttribute_encryptedWithoutKey_throws() {
        assertThatThrownBy(() -> converter.convertToEntityAttribute("ENCv1:aaa:bbb"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("FACE_EMBEDDING_AES_KEY is required");
    }
}
