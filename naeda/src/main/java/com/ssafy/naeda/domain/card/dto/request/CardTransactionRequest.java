package com.ssafy.naeda.domain.card.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CardTransactionRequest {

    @NotBlank
    @Pattern(regexp = "\\d{8}", message = "날짜는 yyyyMMdd 형식이어야 합니다.")
    private String startDate;

    @NotBlank
    @Pattern(regexp = "\\d{8}", message = "날짜는 yyyyMMdd 형식이어야 합니다.")
    private String endDate;

}
