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
    private String cvc;
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
                .cardNo(card.getCardNo())
                .cvc(card.getCvc())
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
                .cardNo(card.getCardNo())
                .cvc(card.getCvc())
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
}
