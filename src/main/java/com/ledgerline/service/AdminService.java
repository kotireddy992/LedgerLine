package com.ledgerline.service;

import com.ledgerline.dto.*;
import com.ledgerline.mapper.EntityMapper;
import com.ledgerline.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final EntityMapper mapper;

    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream().map(mapper::toUserResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> getAllAccounts() {
        return accountRepository.findAll().stream().map(mapper::toAccountResponse).toList();
    }

    @Transactional(readOnly = true)
    public Page<LedgerEntryResponse> getAuditLedger(int page, int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        return ledgerEntryRepository.findAll(pageable).map(mapper::toLedgerEntryResponse);
    }
}
