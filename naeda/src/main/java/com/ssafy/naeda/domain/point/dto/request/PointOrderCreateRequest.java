package com.ssafy.naeda.domain.point.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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

    @Size(max = 200, message = "도로명 주소는 200자 이하이어야 합니다.")
    private String roadAddress;

    @Size(max = 200, message = "지번 주소는 200자 이하이어야 합니다.")
    private String numberAddress;
}
