package com.ledgerline.controller;

import com.ledgerline.dto.*;
import com.ledgerline.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "Admin-only operations")
@SecurityRequirement(name = "bearerAuth")
public class AdminController {

    private final AdminService adminService;

    /** GET /api/admin/users — Role: ADMIN */
    @GetMapping("/users")
    @Operation(summary = "List all users")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(adminService.getAllUsers());
    }

    /** GET /api/admin/accounts — Role: ADMIN */
    @GetMapping("/accounts")
    @Operation(summary = "List all accounts")
    public ResponseEntity<List<AccountResponse>> getAllAccounts() {
        return ResponseEntity.ok(adminService.getAllAccounts());
    }

    /** GET /api/admin/audit — Role: ADMIN */
    @GetMapping("/audit")
    @Operation(summary = "Get full audit ledger (paginated)")
    public ResponseEntity<Page<LedgerEntryResponse>> getAudit(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(adminService.getAuditLedger(page, size));
    }
}
