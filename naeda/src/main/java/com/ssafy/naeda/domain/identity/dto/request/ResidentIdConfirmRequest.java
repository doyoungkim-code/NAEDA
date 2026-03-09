package com.ssafy.naeda.domain.identity.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "주민등록증 OCR 확인 요청")
public class ResidentIdConfirmRequest {

    @NotBlank
    @Size(max = 50)
    @Schema(description = "사용자가 확인 또는 수정한 이름", example = "홍길동")
    private String name;

    @NotBlank
    @Pattern(regexp = "^\\d{6}$")
    @Schema(description = "사용자가 확인 또는 수정한 주민등록번호 앞 6자리", example = "900101")
    private String residentFront6;

    @NotBlank
    @Pattern(regexp = "^\\d$")
    @Schema(description = "사용자가 확인 또는 수정한 주민등록번호 뒤 첫 1자리", example = "1")
    private String residentBackFirst1;
}
