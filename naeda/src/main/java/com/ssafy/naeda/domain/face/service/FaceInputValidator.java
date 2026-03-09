package com.ssafy.naeda.domain.face.service;

import com.ssafy.naeda.domain.face.exception.FaceErrorCode;
import com.ssafy.naeda.domain.face.exception.FaceException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

@Component
public class FaceInputValidator {

    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    private final long maxImageBytes;

    public FaceInputValidator(@Value("${face.max-image-bytes:3145728}") long maxImageBytes) {
        this.maxImageBytes = maxImageBytes;
    }

    public void validateImage(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new FaceException(FaceErrorCode.EMPTY_IMAGE);
        }
        if (image.getSize() > maxImageBytes) {
            throw new FaceException(FaceErrorCode.IMAGE_TOO_LARGE);
        }

        String contentType = image.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase())) {
            throw new FaceException(FaceErrorCode.UNSUPPORTED_IMAGE_TYPE);
        }
    }
}
