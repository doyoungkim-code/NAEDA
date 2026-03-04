package com.ssafy.naeda.domain.point.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "포인트 사용 요청")
public class PointUseRequest {

    @NotNull
    @Positive
    @Schema(description = "사용할 포인트", example = "500", requiredMode = Schema.RequiredMode.REQUIRED, minimum = "1")
    private Long amount;

    @Size(max = 255)
    @Schema(description = "사용 사유", example = "쿠폰 교환")
    private String description;
}
