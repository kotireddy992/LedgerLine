package com.ledgerline.service;

import com.ledgerline.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
@RequiredArgsConstructor
public class AccountNumberGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();
    private final AccountRepository accountRepository;

    public String generate() {
        String number;
        do {
            // Format: LL + 10 digits, e.g. LL2024031500001
            number = "LL" + String.format("%010d", Math.abs(RANDOM.nextLong() % 10_000_000_000L));
        } while (accountRepository.existsByAccountNumber(number));
        return number;
    }
}
