package com.ssafy.naeda.domain.identity.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.naeda.domain.face.client.dto.AiErrorResponse;
import com.ssafy.naeda.domain.identity.client.dto.ResidentIdOcrResponse;
import com.ssafy.naeda.global.exception.BadRequestException;
import com.ssafy.naeda.global.exception.SsafyApiException;
import java.net.SocketTimeoutException;
import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Component
@RequiredArgsConstructor
public class ResidentIdOcrClient {

    private final RestClient aiRestClient;
    private final ObjectMapper objectMapper;

    public ResidentIdOcrResponse extractResidentId(MultipartFile image) {
        byte[] imageBytes = null;
        try {
            imageBytes = image.getBytes();
            ByteArrayResource imageResource = new ByteArrayResource(imageBytes) {
                @Override
                public String getFilename() {
                    return "id-card.jpg";
                }
            };

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("image", imageResource);

            ResidentIdOcrResponse response = aiRestClient.post()
                    .uri("/internal/v1/ocr/id-card/extract")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .onStatus(status -> status.isError(), (req, res) -> {
                        try {
                            byte[] bodyBytes = res.getBody().readAllBytes();
                            AiErrorResponse aiError = objectMapper.readValue(bodyBytes, AiErrorResponse.class);
                            throw mapAiError(aiError.getCode(), aiError.getMessage());
                        } catch (RuntimeException e) {
                            throw e;
                        } catch (Exception e) {
                            throw new SsafyApiException("OCR_UNAVAILABLE", "신분증 OCR 서비스에 접근할 수 없습니다.");
                        }
                    })
                    .body(ResidentIdOcrResponse.class);

            if (response == null || response.getName() == null || response.getResidentFront6() == null
                    || response.getResidentBackFirst1() == null) {
                throw new BadRequestException("신분증 OCR 결과가 올바르지 않습니다.");
            }
            if (!response.isDocumentMatched()) {
                throw new BadRequestException("주민등록증 또는 운전면허증을 인식하지 못했습니다.");
            }
            if (!isSupportedDocumentType(response.getDocumentType())) {
                throw new BadRequestException("지원하지 않는 신분증 종류입니다.");
            }
            return response;
        } catch (BadRequestException | SsafyApiException e) {
            throw e;
        } catch (ResourceAccessException e) {
            if (e.getCause() instanceof SocketTimeoutException) {
                throw new SsafyApiException("OCR_TIMEOUT", "주민등록증 OCR 응답이 지연되고 있습니다.");
            }
            throw new SsafyApiException("OCR_UNAVAILABLE", "신분증 OCR 서비스에 접근할 수 없습니다.");
        } catch (Exception e) {
            log.error("신분증 OCR 호출 중 예외 발생", e);
            throw new SsafyApiException("OCR_UNAVAILABLE", "신분증 OCR 서비스에 접근할 수 없습니다.");
        } finally {
            if (imageBytes != null) {
                Arrays.fill(imageBytes, (byte) 0);
            }
        }
    }

    private RuntimeException mapAiError(String code, String message) {
        return switch (code) {
            case "OCR_EXTRACTION_FAILED" -> new BadRequestException("신분증 정보를 인식하지 못했습니다.");
            case "EMPTY_IMAGE", "INVALID_IMAGE", "UNSUPPORTED_IMAGE_TYPE", "IMAGE_TOO_LARGE" ->
                    new BadRequestException(message != null ? message : "신분증 이미지가 올바르지 않습니다.");
            case "OCR_UNAVAILABLE", "AI_UNAVAILABLE", "AI_TIMEOUT" ->
                    new SsafyApiException(code, message != null ? message : "신분증 OCR 서비스를 사용할 수 없습니다.");
            default -> new SsafyApiException("OCR_UNAVAILABLE", "신분증 OCR 서비스를 사용할 수 없습니다.");
        };
    }

    private boolean isSupportedDocumentType(String documentType) {
        return "RESIDENT_ID".equals(documentType) || "DRIVER_LICENSE".equals(documentType);
    }
}
