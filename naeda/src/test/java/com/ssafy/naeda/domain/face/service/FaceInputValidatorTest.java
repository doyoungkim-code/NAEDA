package com.ssafy.naeda.domain.face.service;

import com.ssafy.naeda.domain.face.exception.FaceException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FaceInputValidatorTest {

    private final FaceInputValidator validator = new FaceInputValidator(4);

    @Test
    @DisplayName("지원하지 않는 MIME 타입이면 예외를 던진다")
    void validateImage_rejectsUnsupportedContentType() {
        MockMultipartFile image = new MockMultipartFile("image", "face.gif", "image/gif", new byte[]{1, 2, 3});

        assertThatThrownBy(() -> validator.validateImage(image))
                .isInstanceOf(FaceException.class)
                .hasMessage("지원하지 않는 이미지 MIME 타입입니다.");
    }

    @Test
    @DisplayName("최대 크기를 초과하면 예외를 던진다")
    void validateImage_rejectsTooLargeImage() {
        MockMultipartFile image = new MockMultipartFile("image", "face.jpg", "image/jpeg", new byte[]{1, 2, 3, 4, 5});

        assertThatThrownBy(() -> validator.validateImage(image))
                .isInstanceOf(FaceException.class)
                .hasMessage("이미지 크기가 허용된 최대 크기를 초과했습니다.");
    }
}
