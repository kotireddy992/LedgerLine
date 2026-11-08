package com.ledgerline.service;

import com.ledgerline.dto.*;
import com.ledgerline.entity.*;
import com.ledgerline.enums.EntryType;
import com.ledgerline.exception.InsufficientFundsException;
import com.ledgerline.mapper.EntityMapper;
import com.ledgerline.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DepositWithdrawalService {

    private final AccountService accountService;
    private final LedgerService ledgerService;
    private final IdempotencyService idempotencyService;
    private final EntityMapper mapper;
    private final SecurityUtils securityUtils;

    /**
     * Deposits amount into the account.
     *
     * Transaction isolation: SERIALIZABLE is not needed here because we use
     * pessimistic row-level locking (SELECT FOR UPDATE) which serializes
     * concurrent access to the same account row regardless of isolation level.
     *
     * Rollback: if ledger INSERT fails after balance UPDATE, the @Transactional
     * annotation rolls back both operations together.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public AccountResponse deposit(Long accountId, MoneyRequest request, String idempotencyKey) {
        User currentUser = securityUtils.getCurrentUser();
        String requestJson = accountId + ":" + request.getAmount().toPlainString();

        Optional<IdempotencyService.StoredResponse> cached =
                idempotencyService.checkOrCreate(idempotencyKey, requestJson);
        if (cached.isPresent()) {
            // Replay — return the original account state (re-fetch for current balance).
            return mapper.toAccountResponse(accountService.findAccountOrThrow(accountId));
        }

        try {
            // Acquire pessimistic write lock on the account row.
            Account account = accountService.findAccountWithLockOrThrow(accountId);
            accountService.assertOwnerOrAdmin(account);
            accountService.assertAccountActive(account);

            BigDecimal amount = request.getAmount().setScale(2, RoundingMode.HALF_EVEN);
            BigDecimal newBalance = account.getBalance().add(amount);

            account.setBalance(newBalance);

            String txRef = "DEP-" + UUID.randomUUID();
            ledgerService.record(txRef, account, EntryType.CREDIT, amount, newBalance, currentUser);

            AccountResponse response = mapper.toAccountResponse(account);
            idempotencyService.markCompleted(idempotencyKey, response, 200);
            return response;

        } catch (Exception e) {
            idempotencyService.markFailed(idempotencyKey);
            throw e;
        }
    }

    /**
     * Withdraws amount from the account.
     *
     * Critical invariant: balance must never go negative.
     * The check is performed AFTER acquiring the pessimistic lock, so no
     * concurrent withdrawal can read the same balance and both succeed.
     *
     * Race condition analysis:
     *   Thread A: locks row, reads balance=100, deducts 80, writes 20, commits.
     *   Thread B: blocks on lock, then reads balance=20, deducts 80 → rejected.
     *   Result: correct. No negative balance possible.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public AccountResponse withdraw(Long accountId, MoneyRequest request, String idempotencyKey) {
        User currentUser = securityUtils.getCurrentUser();
        String requestJson = accountId + ":withdraw:" + request.getAmount().toPlainString();

        Optional<IdempotencyService.StoredResponse> cached =
                idempotencyService.checkOrCreate(idempotencyKey, requestJson);
        if (cached.isPresent()) {
            return mapper.toAccountResponse(accountService.findAccountOrThrow(accountId));
        }

        try {
            Account account = accountService.findAccountWithLockOrThrow(accountId);
            accountService.assertOwnerOrAdmin(account);
            accountService.assertAccountActive(account);

            BigDecimal amount = request.getAmount().setScale(2, RoundingMode.HALF_EVEN);

            // Server-side balance check — never trust client.
            if (account.getBalance().compareTo(amount) < 0) {
                throw new InsufficientFundsException(
                        "Insufficient funds. Available: " + account.getBalance() + ", Requested: " + amount);
            }

            BigDecimal newBalance = account.getBalance().subtract(amount);
            account.setBalance(newBalance);

            String txRef = "WDR-" + UUID.randomUUID();
            ledgerService.record(txRef, account, EntryType.DEBIT, amount, newBalance, currentUser);

            AccountResponse response = mapper.toAccountResponse(account);
            idempotencyService.markCompleted(idempotencyKey, response, 200);
            return response;

        } catch (Exception e) {
            idempotencyService.markFailed(idempotencyKey);
            throw e;
        }
    }
}
