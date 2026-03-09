package com.ssafy.naeda.domain.card.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CardRegisterRequest {

    @NotBlank
    private String cardUniqueNo;

    @NotBlank
    private String withdrawalAccountNo;

    @NotBlank
    private String withdrawalDate;
}
