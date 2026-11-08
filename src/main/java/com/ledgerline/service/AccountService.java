package com.ledgerline.service;

import com.ledgerline.dto.*;
import com.ledgerline.entity.*;
import com.ledgerline.enums.*;
import com.ledgerline.exception.*;
import com.ledgerline.mapper.EntityMapper;
import com.ledgerline.repository.AccountRepository;
import com.ledgerline.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final AccountNumberGenerator accountNumberGenerator;
    private final EntityMapper mapper;
    private final SecurityUtils securityUtils;

    @Transactional
    public AccountResponse createAccount(CreateAccountRequest request) {
        User currentUser = securityUtils.getCurrentUser();

        Account account = Account.builder()
                .accountNumber(accountNumberGenerator.generate())
                .balance(BigDecimal.ZERO)
                .currency(request.getCurrency())
                .status(AccountStatus.ACTIVE)
                .owner(currentUser)
                .version(0L)
                .build();

        return mapper.toAccountResponse(accountRepository.save(account));
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> getMyAccounts() {
        User currentUser = securityUtils.getCurrentUser();
        return accountRepository.findByOwnerId(currentUser.getId())
                .stream().map(mapper::toAccountResponse).toList();
    }

    @Transactional(readOnly = true)
    public AccountResponse getAccount(Long accountId) {
        Account account = findAccountOrThrow(accountId);
        assertOwnerOrAdmin(account);
        return mapper.toAccountResponse(account);
    }

    @Transactional(readOnly = true)
    public BigDecimal getBalance(Long accountId) {
        Account account = findAccountOrThrow(accountId);
        assertOwnerOrAdmin(account);
        return account.getBalance();
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    public Account findAccountOrThrow(Long id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + id));
    }

    public Account findAccountWithLockOrThrow(Long id) {
        return accountRepository.findByIdWithLock(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + id));
    }

    public void assertAccountActive(Account account) {
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new InvalidOperationException(
                    "Account " + account.getAccountNumber() + " is " + account.getStatus());
        }
    }

    public void assertOwnerOrAdmin(Account account) {
        User currentUser = securityUtils.getCurrentUser();
        boolean isOwner = account.getOwner().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRole() == Role.ADMIN;
        if (!isOwner && !isAdmin) {
            throw new AccessDeniedException("You do not have access to this account");
        }
    }

    // Admin-only: list all accounts
    @Transactional(readOnly = true)
    public List<AccountResponse> getAllAccounts() {
        return accountRepository.findAll().stream().map(mapper::toAccountResponse).toList();
    }
}
