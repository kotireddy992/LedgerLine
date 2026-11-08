package com.ledgerline.service;

import com.ledgerline.dto.LedgerEntryResponse;
import com.ledgerline.entity.*;
import com.ledgerline.enums.Role;
import com.ledgerline.exception.UnauthorizedException;
import com.ledgerline.mapper.EntityMapper;
import com.ledgerline.repository.LedgerEntryRepository;
import com.ledgerline.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StatementService {

    private final LedgerEntryRepository ledgerEntryRepository;
    private final AccountService accountService;
    private final SecurityUtils securityUtils;
    private final EntityMapper mapper;

    @Transactional(readOnly = true)
    public Page<LedgerEntryResponse> getStatement(Long accountId, int page, int size) {
        Account account = accountService.findAccountOrThrow(accountId);
        User currentUser = securityUtils.getCurrentUser();
        boolean isOwner   = account.getOwner().getId().equals(currentUser.getId());
        boolean isAdmin   = currentUser.getRole() == Role.ADMIN;
        boolean isAuditor = currentUser.getRole() == Role.AUDITOR;
        if (!isOwner && !isAdmin && !isAuditor) {
            throw new UnauthorizedException("You do not have access to this account statement");
        }

        Pageable pageable = PageRequest.of(page, Math.min(size, 100)); // cap at 100 per page
        return ledgerEntryRepository
                .findByAccountIdOrderByCreatedAtDesc(accountId, pageable)
                .map(mapper::toLedgerEntryResponse);
    }
}
