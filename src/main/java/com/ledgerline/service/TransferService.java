package com.ledgerline.service;

import com.ledgerline.dto.*;
import com.ledgerline.entity.*;
import com.ledgerline.enums.*;
import com.ledgerline.exception.*;
import com.ledgerline.mapper.EntityMapper;
import com.ledgerline.repository.*;
import com.ledgerline.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;
import java.util.UUID;

/**
 * Handles money transfers between accounts.
 *
 * ── Deadlock prevention ───────────────────────────────────────────────────────
 * When locking two accounts, we always lock the one with the LOWER id first.
 * This deterministic ordering prevents the classic deadlock scenario where:
 *   Thread A locks account 1, waits for account 2.
 *   Thread B locks account 2, waits for account 1.
 * With ordered locking, both threads attempt to lock account 1 first.
 * One succeeds; the other blocks. No circular wait → no deadlock.
 *
 * ── Financial invariant ───────────────────────────────────────────────────────
 * For every completed internal transfer:
 *   source.balance decreases by X
 *   destination.balance increases by X
 *   Net change to total system balance = 0
 *   Ledger: one DEBIT entry (source, amount X) + one CREDIT entry (dest, amount X)
 *   DEBIT sum - CREDIT sum = 0  (verified by LedgerEntryRepository.calculateNetForReference)
 *
 * ── Atomicity ─────────────────────────────────────────────────────────────────
 * All of the following happen in one @Transactional boundary:
 *   1. Source balance deducted
 *   2. Destination balance credited
 *   3. DEBIT ledger entry created
 *   4. CREDIT ledger entry created
 *   5. Transfer record saved as COMPLETED
 * If any step throws, the entire transaction rolls back. No partial updates.
 */
@Service
@RequiredArgsConstructor
public class TransferService {

    private final AccountService accountService;
    private final LedgerService ledgerService;
    private final IdempotencyService idempotencyService;
    private final TransferRepository transferRepository;
    private final EntityMapper mapper;
    private final SecurityUtils securityUtils;

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TransferResponse transfer(TransferRequest request, String idempotencyKey) {
        User currentUser = securityUtils.getCurrentUser();

        // Validate same-account transfer before any DB work.
        if (request.getSourceAccountId().equals(request.getDestinationAccountId())) {
            throw new InvalidOperationException("Source and destination accounts must be different");
        }

        String requestJson = request.getSourceAccountId() + ":" +
                request.getDestinationAccountId() + ":" +
                request.getAmount().toPlainString() + ":" +
                request.getCurrency();

        Optional<IdempotencyService.StoredResponse> cached =
                idempotencyService.checkOrCreate(idempotencyKey, requestJson);
        if (cached.isPresent()) {
            // Replay — find and return the original transfer record.
            return transferRepository.findByIdempotencyKey(idempotencyKey)
                    .map(mapper::toTransferResponse)
                    .orElseThrow(() -> new ResourceNotFoundException("Transfer record not found for idempotency key"));
        }

        Transfer transfer = null;
        try {
            // ── Step 1: Validate accounts exist (no lock yet) ─────────────────
            Account sourceUnlocked = accountService.findAccountOrThrow(request.getSourceAccountId());
            Account destUnlocked   = accountService.findAccountOrThrow(request.getDestinationAccountId());

            // ── Step 2: Authorization — only the source account owner can initiate ──
            accountService.assertOwnerOrAdmin(sourceUnlocked);

            // ── Step 3: Currency validation ───────────────────────────────────
            if (!sourceUnlocked.getCurrency().equals(request.getCurrency())) {
                throw new InvalidOperationException(
                        "Transfer currency " + request.getCurrency() +
                        " does not match source account currency " + sourceUnlocked.getCurrency());
            }
            if (!sourceUnlocked.getCurrency().equals(destUnlocked.getCurrency())) {
                throw new InvalidOperationException(
                        "Cross-currency transfers are not supported. " +
                        "Source: " + sourceUnlocked.getCurrency() +
                        ", Destination: " + destUnlocked.getCurrency());
            }

            // ── Step 4: Acquire locks in deterministic order (lower id first) ─
            Long firstLockId  = Math.min(request.getSourceAccountId(), request.getDestinationAccountId());
            Long secondLockId = Math.max(request.getSourceAccountId(), request.getDestinationAccountId());

            Account firstLocked  = accountService.findAccountWithLockOrThrow(firstLockId);
            Account secondLocked = accountService.findAccountWithLockOrThrow(secondLockId);

            // Re-assign to source/dest after locking.
            Account source = firstLockId.equals(request.getSourceAccountId()) ? firstLocked : secondLocked;
            Account dest   = firstLockId.equals(request.getDestinationAccountId()) ? firstLocked : secondLocked;

            // ── Step 5: Validate account statuses ─────────────────────────────
            accountService.assertAccountActive(source);
            accountService.assertAccountActive(dest);

            // ── Step 6: Check sufficient funds (after lock — no TOCTOU race) ──
            BigDecimal amount = request.getAmount().setScale(2, RoundingMode.HALF_EVEN);
            if (source.getBalance().compareTo(amount) < 0) {
                throw new InsufficientFundsException(
                        "Insufficient funds. Available: " + source.getBalance() + ", Requested: " + amount);
            }

            // ── Step 7: Debit source ───────────────────────────────────────────
            BigDecimal sourceNewBalance = source.getBalance().subtract(amount);
            source.setBalance(sourceNewBalance);

            // ── Step 8: Credit destination ────────────────────────────────────
            BigDecimal destNewBalance = dest.getBalance().add(amount);
            dest.setBalance(destNewBalance);

            // ── Step 9: Double-entry ledger ───────────────────────────────────
            String txRef = "TXF-" + UUID.randomUUID();
            ledgerService.record(txRef, source, EntryType.DEBIT,  amount, sourceNewBalance, currentUser);
            ledgerService.record(txRef, dest,   EntryType.CREDIT, amount, destNewBalance,   currentUser);

            // ── Step 10: Save transfer record ─────────────────────────────────
            transfer = transferRepository.save(Transfer.builder()
                    .sourceAccount(source)
                    .destinationAccount(dest)
                    .amount(amount)
                    .currency(request.getCurrency())
                    .status(TransferStatus.COMPLETED)
                    .idempotencyKey(idempotencyKey)
                    .initiatedBy(currentUser)
                    .build());

            // ── Step 11: Mark idempotency record complete ─────────────────────
            TransferResponse response = mapper.toTransferResponse(transfer);
            idempotencyService.markCompleted(idempotencyKey, response, 201);
            return response;

        } catch (Exception e) {
            idempotencyService.markFailed(idempotencyKey);
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public TransferResponse getTransfer(Long transferId) {
        Transfer transfer = transferRepository.findById(transferId)
                .orElseThrow(() -> new ResourceNotFoundException("Transfer not found: " + transferId));

        // Only the source account owner or admin can view a transfer.
        accountService.assertOwnerOrAdmin(transfer.getSourceAccount());
        return mapper.toTransferResponse(transfer);
    }
}
