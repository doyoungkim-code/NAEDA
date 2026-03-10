package com.ssafy.naeda.domain.card.dto.response;

import com.ssafy.naeda.domain.card.entity.CreditCard;
import com.ssafy.naeda.domain.card.entity.DebitCard;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class CardResponse {

    private Long cardId;
    private String cardNo;
    private String cardUniqueNo;
    private String cardIssuerCode;
    private String cardIssuerName;
    private String cardName;
    private String cardExpiryDate;
    private Boolean isActive;
    private Long accountId;
    private String cardType;
    private Long creditLimit;
    private Integer billingDate;

    public static CardResponse fromCreditCard(CreditCard card) {
        return builder()
                .cardId(card.getCreditCardId())
                .cardNo(maskCardNo(card.getCardNo()))
                .cardUniqueNo(card.getCardUniqueNo())
                .cardIssuerCode(card.getCardIssuerCode())
                .cardIssuerName(card.getCardIssuerName())
                .cardName(card.getCardName())
                .cardExpiryDate(card.getCardExpiryDate())
                .isActive(card.getIsActive())
                .accountId(card.getAccountId())
                .cardType("CREDIT")
                .creditLimit(card.getCreditLimit())
                .billingDate(card.getBillingDate())
                .build();
    }

    public static CardResponse fromDebitCard(DebitCard card) {
        return builder()
                .cardId(card.getDebitCardId())
                .cardNo(maskCardNo(card.getCardNo()))
                .cardUniqueNo(card.getCardUniqueNo())
                .cardIssuerCode(card.getCardIssuerCode())
                .cardIssuerName(card.getCardIssuerName())
                .cardName(card.getCardName())
                .cardExpiryDate(card.getCardExpiryDate())
                .isActive(card.getIsActive())
                .accountId(card.getAccountId())
                .cardType("DEBIT")
                .build();
    }

    private static String maskCardNo(String cardNo) {
        if (cardNo == null || cardNo.length() < 8) return cardNo;
        return cardNo.substring(0, 4) + "****" + cardNo.substring(cardNo.length() - 4);
    }
}
