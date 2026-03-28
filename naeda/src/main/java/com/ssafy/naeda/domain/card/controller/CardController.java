package com.ssafy.naeda.domain.card.controller;

import com.ssafy.naeda.domain.card.dto.request.CardRegisterRequest;
import com.ssafy.naeda.domain.card.dto.request.CardTransactionRequest;
import com.ssafy.naeda.domain.card.dto.response.CardProductResponse;
import com.ssafy.naeda.domain.card.dto.response.CardRegisterResponse;
import com.ssafy.naeda.domain.card.dto.response.CardResponse;
import com.ssafy.naeda.domain.card.dto.response.CardTransactionResponse;
import com.ssafy.naeda.domain.card.service.CardService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cards")
@RequiredArgsConstructor
@Validated
public class CardController {

    private final CardService cardService;

    /**
     * 발급 가능한 카드 상품 목록 조회 (SSAFY API 24)
     * GET /api/cards/products
     */
    @GetMapping("/products")
    public ResponseEntity<List<CardProductResponse>> getCardProducts(
            @RequestParam @Positive Long userNo
    ) {
        return ResponseEntity.ok(cardService.getCardProducts(userNo));
    }

    /**
     * 내 카드 목록 조회
     * GET /api/cards
     */
    @GetMapping
    public ResponseEntity<List<CardResponse>> getCards(
            @RequestParam @Positive Long userNo
    ) {
        return ResponseEntity.ok(cardService.getMyCards(userNo));
    }

    /**
     * 카드 등록
     * POST /api/cards
     */
    @PostMapping
    public ResponseEntity<CardRegisterResponse> cardRegister(
            @RequestParam @Positive Long userNo,
            @RequestBody @Valid CardRegisterRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(cardService.registerCard(userNo, request));
    }

    /**
     * 카드 삭제
     * DELETE /api/cards/{cardId}
     */
    @DeleteMapping("/{cardId}")
    public ResponseEntity<Void> deleteCard(
            @PathVariable Long cardId,
            @RequestParam @Positive Long userNo,
            @RequestParam String cardType
    ) {
        cardService.deleteCard(userNo, cardId, cardType);
        return ResponseEntity.noContent().build();
    }

    /**
     * 카드 결제 내역 조회
     * GET /api/cards/{cardId}/transactions
     */
    @GetMapping("/{cardId}/transactions")
    public ResponseEntity<List<CardTransactionResponse>> getCardTransactions (
            @PathVariable Long cardId,
            @RequestParam @Positive Long userNo,
            @RequestParam(defaultValue = "CREDIT") String cardType,
            @ModelAttribute @Valid CardTransactionRequest request
    ) {
        return ResponseEntity.ok(cardService.getCardTransactions(userNo, cardId, cardType, request));
    }
}

