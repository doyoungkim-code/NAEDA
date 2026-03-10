package com.ssafy.naeda.domain.point.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PointOrderCreateRequest {

    @NotNull
    private Long productId;

    private String roadAddress;

    private String numberAddress;
}
