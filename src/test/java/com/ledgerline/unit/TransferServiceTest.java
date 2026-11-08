package com.ledgerline.unit;

import com.ledgerline.dto.TransferRequest;
import com.ledgerline.entity.*;
import com.ledgerline.enums.*;
import com.ledgerline.exception.*;
import com.ledgerline.mapper.EntityMapper;
import com.ledgerline.repository.TransferRepository;
import com.ledgerline.security.SecurityUtils;
import com.ledgerline.service.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransferServiceTest {

    @Mock AccountService accountService;
    @Mock LedgerService ledgerService;
    @Mock IdempotencyService idempotencyService;
    @Mock TransferRepository transferRepository;
    @Mock EntityMapper mapper;
    @Mock SecurityUtils securityUtils;

    @InjectMocks TransferService transferService;

    User user = User.builder().id(1L).email("a@test.com").role(Role.ACCOUNT_HOLDER).build();

    Account makeAccount(Long id, BigDecimal balance) {
        return Account.builder().id(id).balance(balance).currency("INR")
                .status(AccountStatus.ACTIVE).owner(user).version(0L).build();
    }

    @Test
    void transfer_sameAccount_throwsInvalidOperation() {
        TransferRequest req = new TransferRequest(1L, 1L, new BigDecimal("100.00"), "INR");

        assertThatThrownBy(() -> transferService.transfer(req, "key-1"))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("different");
    }

    @Test
    void transfer_insufficientFunds_throwsInsufficientFunds() {
        when(securityUtils.getCurrentUser()).thenReturn(user);
        when(idempotencyService.checkOrCreate(any(), any())).thenReturn(Optional.empty());

        Account source = makeAccount(1L, new BigDecimal("50.00"));
        Account dest   = makeAccount(2L, new BigDecimal("0.00"));

        when(accountService.findAccountOrThrow(1L)).thenReturn(source);
        when(accountService.findAccountOrThrow(2L)).thenReturn(dest);
        doNothing().when(accountService).assertOwnerOrAdmin(any());
        doNothing().when(accountService).assertAccountActive(any());
        when(accountService.findAccountWithLockOrThrow(1L)).thenReturn(source);
        when(accountService.findAccountWithLockOrThrow(2L)).thenReturn(dest);

        TransferRequest req = new TransferRequest(1L, 2L, new BigDecimal("100.00"), "INR");

        assertThatThrownBy(() -> transferService.transfer(req, "key-2"))
                .isInstanceOf(InsufficientFundsException.class);
    }

    @Test
    void transfer_currencyMismatch_throwsInvalidOperation() {
        when(securityUtils.getCurrentUser()).thenReturn(user);
        when(idempotencyService.checkOrCreate(any(), any())).thenReturn(Optional.empty());

        Account source = Account.builder().id(1L).balance(new BigDecimal("500.00"))
                .currency("INR").status(AccountStatus.ACTIVE).owner(user).version(0L).build();
        Account dest = Account.builder().id(2L).balance(BigDecimal.ZERO)
                .currency("USD").status(AccountStatus.ACTIVE).owner(user).version(0L).build();

        when(accountService.findAccountOrThrow(1L)).thenReturn(source);
        when(accountService.findAccountOrThrow(2L)).thenReturn(dest);
        doNothing().when(accountService).assertOwnerOrAdmin(any());

        TransferRequest req = new TransferRequest(1L, 2L, new BigDecimal("100.00"), "INR");

        assertThatThrownBy(() -> transferService.transfer(req, "key-3"))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("currency");
    }

    @Test
    void transfer_frozenAccount_throwsInvalidOperation() {
        when(securityUtils.getCurrentUser()).thenReturn(user);
        when(idempotencyService.checkOrCreate(any(), any())).thenReturn(Optional.empty());

        Account source = Account.builder().id(1L).balance(new BigDecimal("500.00"))
                .currency("INR").status(AccountStatus.FROZEN).owner(user).version(0L).build();
        Account dest = makeAccount(2L, BigDecimal.ZERO);

        when(accountService.findAccountOrThrow(1L)).thenReturn(source);
        when(accountService.findAccountOrThrow(2L)).thenReturn(dest);
        doNothing().when(accountService).assertOwnerOrAdmin(any());
        when(accountService.findAccountWithLockOrThrow(1L)).thenReturn(source);
        when(accountService.findAccountWithLockOrThrow(2L)).thenReturn(dest);
        doCallRealMethod().when(accountService).assertAccountActive(source);

        TransferRequest req = new TransferRequest(1L, 2L, new BigDecimal("100.00"), "INR");

        assertThatThrownBy(() -> transferService.transfer(req, "key-4"))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("FROZEN");
    }
}
