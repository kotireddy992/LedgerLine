package com.ledgerline.controller;

import com.ledgerline.dto.*;
import com.ledgerline.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
@Tag(name = "Audit", description = "Read-only audit endpoints for AUDITOR and ADMIN")
@SecurityRequirement(name = "bearerAuth")
public class AuditController {

    private final AdminService adminService;
    private final StatementService statementService;

    /** GET /api/audit/transactions — Role: AUDITOR, ADMIN */
    @GetMapping("/transactions")
    @Operation(summary = "Get all ledger entries (paginated)")
    public ResponseEntity<Page<LedgerEntryResponse>> getAllTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(adminService.getAuditLedger(page, size));
    }

    /**
     * GET /api/audit/accounts/{id}/statement — Role: AUDITOR, ADMIN
     * Auditor can view any account statement without being the owner.
     */
    @GetMapping("/accounts/{id}/statement")
    @Operation(summary = "Get account statement (auditor access)")
    public ResponseEntity<Page<LedgerEntryResponse>> getAccountStatement(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        // Auditor bypasses ownership check — handled by SecurityConfig role restriction.
        // StatementService.assertOwnerOrAdmin allows ADMIN; for AUDITOR we call directly.
        return ResponseEntity.ok(statementService.getStatement(id, page, size));
    }
}
