package com.ssafy.naeda.domain.card.dto.response;

import com.ssafy.naeda.domain.card.entity.CreditCard;
import com.ssafy.naeda.domain.card.entity.DebitCard;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class CardRegisterResponse {

    private Long cardId;
    private String cardNo;
    private String cvc;
    private String cardUniqueNo;
    private String cardIssuerCode;
    private String cardIssuerName;
    private String cardName;
    private String cardExpiryDate;
    private String cardType;
    private String withdrawalAccountNo;
    private String withdrawalDate;
    private Long paymentMethodId;

    public static CardRegisterResponse fromCreditCard(CreditCard card, String withdrawalAccountNo, String withdrawalDate, Long paymentMethodId) {
        return builder()
                .cardId(card.getCreditCardId())
                .cardNo(card.getCardNo())
                .cvc(card.getCvc())
                .cardUniqueNo(card.getCardUniqueNo())
                .cardIssuerCode(card.getCardIssuerCode())
                .cardIssuerName(card.getCardIssuerName())
                .cardName(card.getCardName())
                .cardExpiryDate(card.getCardExpiryDate())
                .cardType("CREDIT")
                .withdrawalAccountNo(withdrawalAccountNo)
                .withdrawalDate(withdrawalDate)
                .paymentMethodId(paymentMethodId)
                .build();
    }

    public static CardRegisterResponse fromDebitCard(DebitCard card, String withdrawalAccountNo, String withdrawalDate, Long paymentMethodId) {
        return builder()
                .cardId(card.getDebitCardId())
                .cardNo(card.getCardNo())
                .cvc(card.getCvc())
                .cardUniqueNo(card.getCardUniqueNo())
                .cardIssuerCode(card.getCardIssuerCode())
                .cardIssuerName(card.getCardIssuerName())
                .cardName(card.getCardName())
                .cardExpiryDate(card.getCardExpiryDate())
                .cardType("DEBIT")
                .withdrawalAccountNo(withdrawalAccountNo)
                .withdrawalDate(withdrawalDate)
                .paymentMethodId(paymentMethodId)
                .build();
    }

}
