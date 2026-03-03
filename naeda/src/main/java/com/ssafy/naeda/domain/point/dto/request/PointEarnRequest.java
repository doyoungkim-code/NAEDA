package com.ssafy.naeda.domain.point.dto.request;

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
public class PointEarnRequest {

    @NotNull
    @Positive
    private Long amount;

    @Size(max = 255)
    private String description;

    private Long paymentId;
}
