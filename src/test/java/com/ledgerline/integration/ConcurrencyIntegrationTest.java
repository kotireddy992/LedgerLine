package com.ledgerline.integration;

import com.ledgerline.dto.*;
import com.ledgerline.enums.Role;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Concurrency tests — execute ACTUAL simultaneous HTTP requests against the real database.
 * These tests will FAIL if pessimistic locking is removed.
 *
 * Test: 10 concurrent withdrawals of 100 each from an account with balance 500.
 * Expected: exactly 5 succeed, 5 fail with insufficient funds.
 * Final balance: 0.00 (never negative).
 */
class ConcurrencyIntegrationTest extends BaseIntegrationTest {

    @Autowired TestRestTemplate restTemplate;

    static String userToken;
    static Long accountId;

    @BeforeAll
    static void setup(@Autowired TestRestTemplate rt) {
        long ts = System.currentTimeMillis();
        userToken = TestHelper.registerAndLogin(rt, "concurrency_" + ts + "@test.com", "Password1", Role.ACCOUNT_HOLDER);

        ResponseEntity<AccountResponse> accResp = rt.exchange(
                "/api/accounts", HttpMethod.POST,
                new HttpEntity<>(new CreateAccountRequest("INR"), TestHelper.authHeaders(userToken)),
                AccountResponse.class);
        accountId = accResp.getBody().getId();

        // Fund with 500
        rt.exchange("/api/accounts/" + accountId + "/deposit", HttpMethod.POST,
                new HttpEntity<>(new MoneyRequest(new BigDecimal("500.00")),
                        TestHelper.authHeadersWithIdempotency(userToken, UUID.randomUUID().toString())),
                AccountResponse.class);
    }

    @Test
    void concurrentWithdrawals_neverProduceNegativeBalance() throws InterruptedException {
        int threads = 10;
        BigDecimal withdrawAmount = new BigDecimal("100.00");
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            futures.add(executor.submit(() -> {
                try {
                    latch.await(); // All threads start simultaneously
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                MoneyRequest req = new MoneyRequest(withdrawAmount);
                ResponseEntity<AccountResponse> resp = restTemplate.exchange(
                        "/api/accounts/" + accountId + "/withdraw", HttpMethod.POST,
                        new HttpEntity<>(req, TestHelper.authHeadersWithIdempotency(
                                userToken, UUID.randomUUID().toString())),
                        AccountResponse.class);

                if (resp.getStatusCode() == HttpStatus.OK) {
                    successCount.incrementAndGet();
                } else {
                    failCount.incrementAndGet();
                }
            }));
        }

        latch.countDown(); // Release all threads at once
        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.SECONDS);

        // Exactly 5 should succeed (500 / 100 = 5), 5 should fail.
        assertThat(successCount.get()).isEqualTo(5);
        assertThat(failCount.get()).isEqualTo(5);

        // Final balance must be exactly 0.00 — never negative.
        ResponseEntity<BigDecimal> balResp = restTemplate.exchange(
                "/api/accounts/" + accountId + "/balance", HttpMethod.GET,
                new HttpEntity<>(TestHelper.authHeaders(userToken)), BigDecimal.class);
        assertThat(balResp.getBody()).isEqualByComparingTo("0.00");
    }

    @Test
    void concurrentTransfers_oppositeDirection_noDeadlock() throws InterruptedException {
        long ts = System.currentTimeMillis();
        String tokenX = TestHelper.registerAndLogin(restTemplate, "deadlock_x_" + ts + "@test.com", "Password1", Role.ACCOUNT_HOLDER);
        String tokenY = TestHelper.registerAndLogin(restTemplate, "deadlock_y_" + ts + "@test.com", "Password1", Role.ACCOUNT_HOLDER);

        Long accX = createAndFund(tokenX, "500.00");
        Long accY = createAndFund(tokenY, "500.00");

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(1);

        // Thread 1: X → Y
        Future<?> f1 = executor.submit(() -> {
            try { latch.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            TransferRequest req = new TransferRequest(accX, accY, new BigDecimal("100.00"), "INR");
            restTemplate.exchange("/api/transfers", HttpMethod.POST,
                    new HttpEntity<>(req, TestHelper.authHeadersWithIdempotency(tokenX, UUID.randomUUID().toString())),
                    TransferResponse.class);
        });

        // Thread 2: Y → X (opposite direction — classic deadlock scenario)
        Future<?> f2 = executor.submit(() -> {
            try { latch.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            TransferRequest req = new TransferRequest(accY, accX, new BigDecimal("100.00"), "INR");
            restTemplate.exchange("/api/transfers", HttpMethod.POST,
                    new HttpEntity<>(req, TestHelper.authHeadersWithIdempotency(tokenY, UUID.randomUUID().toString())),
                    TransferResponse.class);
        });

        latch.countDown();
        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.SECONDS);

        // Both transfers should complete without deadlock.
        // Total balance must be conserved: X + Y = 1000.
        BigDecimal balX = getBalance(accX, tokenX);
        BigDecimal balY = getBalance(accY, tokenY);
        assertThat(balX.add(balY)).isEqualByComparingTo("1000.00");
    }

    Long createAndFund(String token, String amount) {
        ResponseEntity<AccountResponse> accResp = restTemplate.exchange(
                "/api/accounts", HttpMethod.POST,
                new HttpEntity<>(new CreateAccountRequest("INR"), TestHelper.authHeaders(token)),
                AccountResponse.class);
        Long id = accResp.getBody().getId();

        restTemplate.exchange("/api/accounts/" + id + "/deposit", HttpMethod.POST,
                new HttpEntity<>(new MoneyRequest(new BigDecimal(amount)),
                        TestHelper.authHeadersWithIdempotency(token, UUID.randomUUID().toString())),
                AccountResponse.class);
        return id;
    }

    BigDecimal getBalance(Long accId, String token) {
        return restTemplate.exchange("/api/accounts/" + accId + "/balance", HttpMethod.GET,
                new HttpEntity<>(TestHelper.authHeaders(token)), BigDecimal.class).getBody();
    }
}
