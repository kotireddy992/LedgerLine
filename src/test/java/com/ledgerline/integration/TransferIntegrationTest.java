package com.ledgerline.integration;

import com.ledgerline.dto.*;
import com.ledgerline.enums.Role;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TransferIntegrationTest extends BaseIntegrationTest {

    @Autowired TestRestTemplate restTemplate;

    static String userAToken, userBToken;
    static Long accountA, accountB;

    @BeforeAll
    static void setup(@Autowired TestRestTemplate rt) {
        long ts = System.currentTimeMillis();
        userAToken = TestHelper.registerAndLogin(rt, "transfer_a_" + ts + "@test.com", "Password1", Role.ACCOUNT_HOLDER);
        userBToken = TestHelper.registerAndLogin(rt, "transfer_b_" + ts + "@test.com", "Password1", Role.ACCOUNT_HOLDER);

        accountA = createAccount(rt, userAToken);
        accountB = createAccount(rt, userBToken);

        // Fund account A with 1000
        deposit(rt, userAToken, accountA, "1000.00");
    }

    static Long createAccount(TestRestTemplate rt, String token) {
        ResponseEntity<AccountResponse> resp = rt.exchange(
                "/api/accounts", HttpMethod.POST,
                new HttpEntity<>(new CreateAccountRequest("INR"), TestHelper.authHeaders(token)),
                AccountResponse.class);
        return resp.getBody().getId();
    }

    static void deposit(TestRestTemplate rt, String token, Long accId, String amount) {
        rt.exchange("/api/accounts/" + accId + "/deposit", HttpMethod.POST,
                new HttpEntity<>(new MoneyRequest(new BigDecimal(amount)),
                        TestHelper.authHeadersWithIdempotency(token, UUID.randomUUID().toString())),
                AccountResponse.class);
    }

    @Test @Order(1)
    void transfer_success_balancesUpdatedCorrectly() {
        TransferRequest req = new TransferRequest(accountA, accountB, new BigDecimal("300.00"), "INR");
        ResponseEntity<TransferResponse> resp = restTemplate.exchange(
                "/api/transfers", HttpMethod.POST,
                new HttpEntity<>(req, TestHelper.authHeadersWithIdempotency(userAToken, UUID.randomUUID().toString())),
                TransferResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resp.getBody().getStatus().name()).isEqualTo("COMPLETED");

        // Verify balances: A should have 700, B should have 300.
        BigDecimal balA = getBalance(accountA, userAToken);
        BigDecimal balB = getBalance(accountB, userBToken);
        assertThat(balA).isEqualByComparingTo("700.00");
        assertThat(balB).isEqualByComparingTo("300.00");

        // Financial invariant: total balance unchanged (1000 = 700 + 300).
        assertThat(balA.add(balB)).isEqualByComparingTo("1000.00");
    }

    @Test @Order(2)
    void transfer_sameAccount_returns400() {
        TransferRequest req = new TransferRequest(accountA, accountA, new BigDecimal("100.00"), "INR");
        ResponseEntity<ErrorResponse> resp = restTemplate.exchange(
                "/api/transfers", HttpMethod.POST,
                new HttpEntity<>(req, TestHelper.authHeadersWithIdempotency(userAToken, UUID.randomUUID().toString())),
                ErrorResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test @Order(3)
    void transfer_insufficientFunds_returns400_balancesUnchanged() {
        BigDecimal balABefore = getBalance(accountA, userAToken);

        TransferRequest req = new TransferRequest(accountA, accountB, new BigDecimal("9999.00"), "INR");
        ResponseEntity<ErrorResponse> resp = restTemplate.exchange(
                "/api/transfers", HttpMethod.POST,
                new HttpEntity<>(req, TestHelper.authHeadersWithIdempotency(userAToken, UUID.randomUUID().toString())),
                ErrorResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        // Balance must be unchanged after failed transfer.
        BigDecimal balAAfter = getBalance(accountA, userAToken);
        assertThat(balAAfter).isEqualByComparingTo(balABefore);
    }

    @Test @Order(4)
    void transfer_unauthorizedUser_cannotTransferOthersAccount() {
        // User B tries to transfer from User A's account.
        TransferRequest req = new TransferRequest(accountA, accountB, new BigDecimal("100.00"), "INR");
        ResponseEntity<ErrorResponse> resp = restTemplate.exchange(
                "/api/transfers", HttpMethod.POST,
                new HttpEntity<>(req, TestHelper.authHeadersWithIdempotency(userBToken, UUID.randomUUID().toString())),
                ErrorResponse.class);

        assertThat(resp.getStatusCode()).isIn(HttpStatus.FORBIDDEN, HttpStatus.UNAUTHORIZED);
    }

    @Test @Order(5)
    void transfer_idempotency_sameKeyTwice_moneyMovesOnce() {
        BigDecimal balABefore = getBalance(accountA, userAToken);
        BigDecimal balBBefore = getBalance(accountB, userBToken);

        String idempotencyKey = UUID.randomUUID().toString();
        TransferRequest req = new TransferRequest(accountA, accountB, new BigDecimal("50.00"), "INR");

        // First request
        ResponseEntity<TransferResponse> resp1 = restTemplate.exchange(
                "/api/transfers", HttpMethod.POST,
                new HttpEntity<>(req, TestHelper.authHeadersWithIdempotency(userAToken, idempotencyKey)),
                TransferResponse.class);

        // Second request with same key — must return same result, not move money again.
        ResponseEntity<TransferResponse> resp2 = restTemplate.exchange(
                "/api/transfers", HttpMethod.POST,
                new HttpEntity<>(req, TestHelper.authHeadersWithIdempotency(userAToken, idempotencyKey)),
                TransferResponse.class);

        assertThat(resp1.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resp2.getStatusCode()).isIn(HttpStatus.CREATED, HttpStatus.OK);

        // Money must have moved exactly once: A loses 50, B gains 50.
        BigDecimal balAAfter = getBalance(accountA, userAToken);
        BigDecimal balBAfter = getBalance(accountB, userBToken);
        assertThat(balAAfter).isEqualByComparingTo(balABefore.subtract(new BigDecimal("50.00")));
        assertThat(balBAfter).isEqualByComparingTo(balBBefore.add(new BigDecimal("50.00")));
    }

    @Test @Order(6)
    void transfer_idempotency_sameKeyDifferentPayload_returns409() {
        String idempotencyKey = UUID.randomUUID().toString();

        TransferRequest req1 = new TransferRequest(accountA, accountB, new BigDecimal("10.00"), "INR");
        restTemplate.exchange("/api/transfers", HttpMethod.POST,
                new HttpEntity<>(req1, TestHelper.authHeadersWithIdempotency(userAToken, idempotencyKey)),
                TransferResponse.class);

        // Same key, different amount.
        TransferRequest req2 = new TransferRequest(accountA, accountB, new BigDecimal("99.00"), "INR");
        ResponseEntity<ErrorResponse> resp2 = restTemplate.exchange(
                "/api/transfers", HttpMethod.POST,
                new HttpEntity<>(req2, TestHelper.authHeadersWithIdempotency(userAToken, idempotencyKey)),
                ErrorResponse.class);

        assertThat(resp2.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    BigDecimal getBalance(Long accId, String token) {
        ResponseEntity<BigDecimal> resp = restTemplate.exchange(
                "/api/accounts/" + accId + "/balance", HttpMethod.GET,
                new HttpEntity<>(TestHelper.authHeaders(token)), BigDecimal.class);
        return resp.getBody();
    }
}
