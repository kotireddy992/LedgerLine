package com.ledgerline.controller;

import com.ledgerline.dto.*;
import com.ledgerline.service.TransferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/transfers")
@RequiredArgsConstructor
@Tag(name = "Transfers", description = "Money transfers between accounts")
@SecurityRequirement(name = "bearerAuth")
public class TransferController {

    private final TransferService transferService;

    /**
     * POST /api/transfers
     * Role: ACCOUNT_HOLDER (source account owner), ADMIN
     * Changes money: YES
     * Idempotency-Key header: required
     */
    @PostMapping
    @Operation(summary = "Transfer money between two accounts")
    public ResponseEntity<TransferResponse> transfer(
            @Valid @RequestBody TransferRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            idempotencyKey = UUID.randomUUID().toString();
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(transferService.transfer(request, idempotencyKey));
    }

    /**
     * GET /api/transfers/{id}
     * Role: source account owner or ADMIN
     * Changes money: NO
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get transfer by ID")
    public ResponseEntity<TransferResponse> getTransfer(@PathVariable Long id) {
        return ResponseEntity.ok(transferService.getTransfer(id));
    }
}
