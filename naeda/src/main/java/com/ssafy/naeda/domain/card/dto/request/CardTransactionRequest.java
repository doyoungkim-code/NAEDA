package com.ssafy.naeda.domain.card.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CardTransactionRequest {

    @NotBlank
    private String startDate;

    @NotBlank
    private String endDate;

}
