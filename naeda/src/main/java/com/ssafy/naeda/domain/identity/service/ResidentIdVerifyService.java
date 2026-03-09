package com.ssafy.naeda.domain.identity.service;

import com.ssafy.naeda.domain.face.service.FaceInputValidator;
import com.ssafy.naeda.domain.identity.client.ResidentIdOcrClient;
import com.ssafy.naeda.domain.identity.client.dto.ResidentIdOcrResponse;
import com.ssafy.naeda.domain.identity.dto.request.ResidentIdConfirmRequest;
import com.ssafy.naeda.domain.identity.dto.response.ResidentIdExtractResponse;
import com.ssafy.naeda.domain.identity.dto.response.ResidentIdVerifyResponse;
import com.ssafy.naeda.domain.user.entity.User;
import com.ssafy.naeda.domain.user.repository.UserRepository;
import com.ssafy.naeda.global.exception.NotFoundException;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class ResidentIdVerifyService {

    private static final Pattern WHITESPACE = Pattern.compile("\\s+");
    private static final Pattern NON_DIGIT = Pattern.compile("\\D");

    private final UserRepository userRepository;
    private final ResidentIdOcrClient residentIdOcrClient;
    private final FaceInputValidator faceInputValidator;

    public ResidentIdExtractResponse extract(MultipartFile image) {
        faceInputValidator.validateImage(image);
        ResidentIdOcrResponse ocrResponse = residentIdOcrClient.extractResidentId(image);

        return ResidentIdExtractResponse.builder()
                .documentType(ocrResponse.getDocumentType())
                .documentMatched(ocrResponse.isDocumentMatched())
                .name(ocrResponse.getName())
                .residentFront6(ocrResponse.getResidentFront6())
                .residentBackFirst1(ocrResponse.getResidentBackFirst1())
                .provider(ocrResponse.getProvider())
                .confidence(ocrResponse.getConfidence())
                .build();
    }

    public ResidentIdVerifyResponse confirm(String userId, ResidentIdConfirmRequest request) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));

        String normalizedName = normalizeName(user.getUsername());
        String normalizedResidentNo = normalizeDigits(user.getResidentNo());
        String extractedName = normalizeName(request.getName());
        String extractedResidentNo = normalizeDigits(request.getResidentFront6() + request.getResidentBackFirst1());

        boolean nameMatched = normalizedName.equals(extractedName);
        boolean residentNoMatched = normalizedResidentNo.equals(extractedResidentNo);
        boolean verified = nameMatched && residentNoMatched;

        return ResidentIdVerifyResponse.builder()
                .verified(verified)
                .nameMatched(nameMatched)
                .residentNoMatched(residentNoMatched)
                .nextAction(verified ? "CONTINUE_FACEPAY_REGISTRATION" : "RETRY_CONFIRM")
                .build();
    }

    private String normalizeName(String value) {
        return WHITESPACE.matcher(value == null ? "" : value).replaceAll("");
    }

    private String normalizeDigits(String value) {
        return NON_DIGIT.matcher(value == null ? "" : value).replaceAll("");
    }
}
