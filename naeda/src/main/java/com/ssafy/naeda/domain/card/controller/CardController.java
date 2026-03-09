package com.ssafy.naeda.domain.card.controller;

import com.ssafy.naeda.domain.card.dto.request.CardRegisterRequest;
import com.ssafy.naeda.domain.card.dto.response.CardRegisterResponse;
import com.ssafy.naeda.domain.card.service.CardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cards")
@RequiredArgsConstructor
public class CardController {

    private final CardService cardService;

    /**
     * 카드 등록
     * POST /api/cards
     */
    @PostMapping
    public ResponseEntity<CardRegisterResponse> cardRegister (
            @RequestParam Long userNo,
            @RequestParam String userKey,
            @RequestBody @Valid CardRegisterRequest request
    ) {
        CardRegisterResponse response = cardService.registerCard(userNo, userKey, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
