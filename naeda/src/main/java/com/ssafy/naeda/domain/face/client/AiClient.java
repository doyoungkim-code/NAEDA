package com.ssafy.naeda.domain.face.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.naeda.domain.face.client.dto.AiEmbeddingResponse;
import com.ssafy.naeda.domain.face.client.dto.AiErrorResponse;
import com.ssafy.naeda.domain.face.client.dto.AiEmbeddingResult;
import com.ssafy.naeda.domain.face.exception.FaceErrorCode;
import com.ssafy.naeda.domain.face.exception.FaceException;
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

import java.net.SocketTimeoutException;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiClient {

    private final RestClient aiRestClient;
    private final ObjectMapper objectMapper;

    /**
     * AI 서버에 이미지를 보내서 임베딩(512개 float 배열)을 받아옴
     */
    public AiEmbeddingResult extractEmbedding(MultipartFile image) {
        try {
            byte[] imageBytes = image.getBytes();

            // MultipartFile -> ByteArrayResource (RestClient가 multipart로 전송할 수 있도록)
            ByteArrayResource imageResource = new ByteArrayResource(imageBytes) {
                @Override
                public String getFilename() {
                    String name = image.getOriginalFilename();
                    return (name != null && !name.isBlank()) ? name : "image.jpg";
                }
            };

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("image", imageResource);

            AiEmbeddingResponse response = aiRestClient.post()
                    .uri("/internal/v1/embeddings/extract")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .onStatus(status -> status.isError(), (req, res) -> {
                        try {
                            byte[] bodyBytes = res.getBody().readAllBytes();
                            AiErrorResponse aiError = objectMapper.readValue(bodyBytes, AiErrorResponse.class);
                            throw mapAiErrorCode(aiError.getCode());
                        } catch (FaceException e) {
                            throw e;
                        } catch (Exception e) {
                            throw new FaceException(FaceErrorCode.AI_UNAVAILABLE);
                        }
                    })
                    .body(AiEmbeddingResponse.class);

            if (response == null || response.getEmbedding() == null || response.getEmbedding().size() != 512) {
                log.error("AI 서버 응답이 유효하지 않습니다: {}", response);
                throw new FaceException(FaceErrorCode.AI_UNAVAILABLE);
            }

            return AiEmbeddingResult.builder()
                    .embedding(response.toFloatArray())
                    .qualityScore(response.getQualityScore())
                    .model(response.getModel())
                    .faceCount(response.getFaceCount())
                    .yaw(response.getYaw())
                    .pitch(response.getPitch())
                    .roll(response.getRoll())
                    .build();

        } catch (FaceException e) {
            throw e;
        } catch (ResourceAccessException e) {
            if (e.getCause() instanceof SocketTimeoutException) {
                log.warn("AI 서버 timeout");
                throw new FaceException(FaceErrorCode.AI_TIMEOUT);
            }
            log.warn("AI 서버 연결 실패: {}", e.getMessage());
            throw new FaceException(FaceErrorCode.AI_UNAVAILABLE);
        } catch (Exception e) {
            log.error("AI 서버 호출 중 예외 발생", e);
            throw new FaceException(FaceErrorCode.AI_UNAVAILABLE);
        }
    }

    /**
     * AI 에러 코드 -> FaceException 변환
     */
    private FaceException mapAiErrorCode(String code) {
        if (code == null) return new FaceException(FaceErrorCode.AI_UNAVAILABLE);
        return switch (code) {
            case "NO_FACE"         -> new FaceException(FaceErrorCode.NO_FACE);
            case "MULTIPLE_FACES"  -> new FaceException(FaceErrorCode.MULTIPLE_FACES);
            case "EMPTY_IMAGE"     -> new FaceException(FaceErrorCode.EMPTY_IMAGE);
            case "INVALID_IMAGE"   -> new FaceException(FaceErrorCode.INVALID_IMAGE);
            default                -> new FaceException(FaceErrorCode.AI_UNAVAILABLE);
        };
    }
}
