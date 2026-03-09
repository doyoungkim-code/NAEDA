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

    /**
     * SSAFY 카드 상품 타입 코드 (API 24 카드 상품 조회에서 확인).
     * "1" = 신용카드, "2" = 체크카드
     */
    @NotBlank
    private String cardTypeCode;
}
