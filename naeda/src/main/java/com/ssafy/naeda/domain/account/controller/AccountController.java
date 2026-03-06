package com.ssafy.naeda.domain.account.controller;

import com.ssafy.naeda.domain.account.dto.response.AccountResponse;
import com.ssafy.naeda.domain.account.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    /**
     * GET /api/accounts?userNo=1
     */
    @GetMapping
    public ResponseEntity<List<AccountResponse>> getAccounts(
            @RequestParam Long userNo
    ) {
        return ResponseEntity.ok(accountService.getAccounts(userNo));
    }

    /**
     * GET /api/accounts/{accountNo}?userNo=1
     */
    @GetMapping("/{accountNo}")
    public ResponseEntity<AccountResponse> getAccount(
            @RequestParam Long userNo,
            @PathVariable String accountNo
    ) {
        return ResponseEntity.ok(accountService.getAccount(userNo, accountNo));
    }
}
