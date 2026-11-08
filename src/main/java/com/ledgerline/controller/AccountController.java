package com.ledgerline.controller;

import com.ledgerline.dto.*;
import com.ledgerline.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
@Tag(name = "Accounts", description = "Account management, deposits, withdrawals, statements")
@SecurityRequirement(name = "bearerAuth")
public class AccountController {

    private final AccountService accountService;
    private final DepositWithdrawalService depositWithdrawalService;
    private final StatementService statementService;

    /**
     * POST /api/accounts
     * Role: ACCOUNT_HOLDER, ADMIN
     * Changes money: NO
     */
    @PostMapping
    @Operation(summary = "Create a new account for the authenticated user")
    public ResponseEntity<AccountResponse> createAccount(@Valid @RequestBody CreateAccountRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(accountService.createAccount(request));
    }

    /**
     * GET /api/accounts
     * Role: ACCOUNT_HOLDER (own accounts), ADMIN (all)
     * Changes money: NO
     */
    @GetMapping
    @Operation(summary = "Get accounts for the authenticated user")
    public ResponseEntity<List<AccountResponse>> getMyAccounts() {
        return ResponseEntity.ok(accountService.getMyAccounts());
    }

    /**
     * GET /api/accounts/{id}
     * Role: owner or ADMIN
     * Changes money: NO
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get account by ID")
    public ResponseEntity<AccountResponse> getAccount(@PathVariable Long id) {
        return ResponseEntity.ok(accountService.getAccount(id));
    }

    /**
     * GET /api/accounts/{id}/balance
     * Role: owner or ADMIN
     * Changes money: NO
     */
    @GetMapping("/{id}/balance")
    @Operation(summary = "Get current balance of an account")
    public ResponseEntity<BigDecimal> getBalance(@PathVariable Long id) {
        return ResponseEntity.ok(accountService.getBalance(id));
    }

    /**
     * POST /api/accounts/{id}/deposit
     * Role: owner or ADMIN
     * Changes money: YES
     * Idempotency-Key header: required
     */
    @PostMapping("/{id}/deposit")
    @Operation(summary = "Deposit money into an account")
    public ResponseEntity<AccountResponse> deposit(
            @PathVariable Long id,
            @Valid @RequestBody MoneyRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            idempotencyKey = UUID.randomUUID().toString();
        }
        return ResponseEntity.ok(depositWithdrawalService.deposit(id, request, idempotencyKey));
    }

    /**
     * POST /api/accounts/{id}/withdraw
     * Role: owner or ADMIN
     * Changes money: YES
     * Idempotency-Key header: required
     */
    @PostMapping("/{id}/withdraw")
    @Operation(summary = "Withdraw money from an account")
    public ResponseEntity<AccountResponse> withdraw(
            @PathVariable Long id,
            @Valid @RequestBody MoneyRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            idempotencyKey = UUID.randomUUID().toString();
        }
        return ResponseEntity.ok(depositWithdrawalService.withdraw(id, request, idempotencyKey));
    }

    /**
     * GET /api/accounts/{id}/statement
     * Role: owner, ADMIN, AUDITOR
     * Changes money: NO
     */
    @GetMapping("/{id}/statement")
    @Operation(summary = "Get paginated transaction statement for an account")
    public ResponseEntity<Page<LedgerEntryResponse>> getStatement(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(statementService.getStatement(id, page, size));
    }
}
