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
     * POST /api/cards?userNo=1
     * 카드 등록. userKey는 서버에서 DB 조회하여 사용.
     */
    @PostMapping
    public ResponseEntity<CardRegisterResponse> cardRegister(
            @RequestParam Long userNo,
            @RequestBody @Valid CardRegisterRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(cardService.registerCard(userNo, request));
    }
}
