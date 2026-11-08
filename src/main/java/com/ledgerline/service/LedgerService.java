package com.ledgerline.service;

import com.ledgerline.entity.*;
import com.ledgerline.enums.EntryType;
import com.ledgerline.repository.LedgerEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Creates ledger entries. This service only ever INSERTs — never UPDATEs or DELETEs.
 * Called within the same @Transactional boundary as the balance update so both
 * commit or both roll back atomically.
 */
@Service
@RequiredArgsConstructor
public class LedgerService {

    private final LedgerEntryRepository ledgerEntryRepository;

    public LedgerEntry record(
            String transactionReference,
            Account account,
            EntryType entryType,
            BigDecimal amount,
            BigDecimal resultingBalance,
            User initiatedBy) {

        LedgerEntry entry = LedgerEntry.builder()
                .transactionReference(transactionReference)
                .account(account)
                .entryType(entryType)
                .amount(amount)
                .resultingBalance(resultingBalance)
                .initiatedBy(initiatedBy)
                .build();

        return ledgerEntryRepository.save(entry);
    }
}
