package com.ssafy.naeda.domain.pay.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PayRequestCreateRequest {
    @NotNull
    private Long storeId;

    @NotNull
    @Positive
    private Long amount;
}
