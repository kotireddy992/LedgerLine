package com.ledgerline.mapper;

import com.ledgerline.dto.*;
import com.ledgerline.entity.*;
import org.springframework.stereotype.Component;

@Component
public class EntityMapper {

    public AccountResponse toAccountResponse(Account account) {
        return AccountResponse.builder()
                .id(account.getId())
                .accountNumber(account.getAccountNumber())
                .balance(account.getBalance())
                .currency(account.getCurrency())
                .status(account.getStatus())
                .ownerId(account.getOwner().getId())
                .ownerName(account.getOwner().getName())
                .createdAt(account.getCreatedAt())
                .build();
    }

    public TransferResponse toTransferResponse(Transfer transfer) {
        return TransferResponse.builder()
                .id(transfer.getId())
                .sourceAccountId(transfer.getSourceAccount().getId())
                .destinationAccountId(transfer.getDestinationAccount().getId())
                .amount(transfer.getAmount())
                .currency(transfer.getCurrency())
                .status(transfer.getStatus())
                .idempotencyKey(transfer.getIdempotencyKey())
                .failureReason(transfer.getFailureReason())
                .createdAt(transfer.getCreatedAt())
                .build();
    }

    public LedgerEntryResponse toLedgerEntryResponse(LedgerEntry entry) {
        return LedgerEntryResponse.builder()
                .id(entry.getId())
                .transactionReference(entry.getTransactionReference())
                .entryType(entry.getEntryType())
                .amount(entry.getAmount())
                .resultingBalance(entry.getResultingBalance())
                .initiatedByEmail(entry.getInitiatedBy().getEmail())
                .createdAt(entry.getCreatedAt())
                .build();
    }

    public UserResponse toUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
