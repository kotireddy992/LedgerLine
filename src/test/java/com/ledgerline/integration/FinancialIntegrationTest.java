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
class FinancialIntegrationTest extends BaseIntegrationTest {

    @Autowired TestRestTemplate restTemplate;

    static String token;
    static Long accountId;
    static final String EMAIL = "financial_" + System.currentTimeMillis() + "@test.com";

    @BeforeAll
    static void setup(@Autowired TestRestTemplate rt) {
        token = TestHelper.registerAndLogin(rt, EMAIL, "Password1", Role.ACCOUNT_HOLDER);

        CreateAccountRequest req = new CreateAccountRequest("INR");
        ResponseEntity<AccountResponse> resp = rt.exchange(
                "/api/accounts", HttpMethod.POST,
                new HttpEntity<>(req, TestHelper.authHeaders(token)), AccountResponse.class);
        accountId = resp.getBody().getId();
    }

    @Test @Order(1)
    void deposit_success_balanceIncreases() {
        MoneyRequest req = new MoneyRequest(new BigDecimal("500.00"));
        ResponseEntity<AccountResponse> resp = restTemplate.exchange(
                "/api/accounts/" + accountId + "/deposit", HttpMethod.POST,
                new HttpEntity<>(req, TestHelper.authHeadersWithIdempotency(token, UUID.randomUUID().toString())),
                AccountResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().getBalance()).isEqualByComparingTo("500.00");
    }

    @Test @Order(2)
    void deposit_zeroAmount_returns400() {
        MoneyRequest req = new MoneyRequest(BigDecimal.ZERO);
        ResponseEntity<ErrorResponse> resp = restTemplate.exchange(
                "/api/accounts/" + accountId + "/deposit", HttpMethod.POST,
                new HttpEntity<>(req, TestHelper.authHeadersWithIdempotency(token, UUID.randomUUID().toString())),
                ErrorResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test @Order(3)
    void deposit_negativeAmount_returns400() {
        MoneyRequest req = new MoneyRequest(new BigDecimal("-100.00"));
        ResponseEntity<ErrorResponse> resp = restTemplate.exchange(
                "/api/accounts/" + accountId + "/deposit", HttpMethod.POST,
                new HttpEntity<>(req, TestHelper.authHeadersWithIdempotency(token, UUID.randomUUID().toString())),
                ErrorResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test @Order(4)
    void withdraw_success_balanceDecreases() {
        // Balance is 500 from Order(1). Withdraw 200.
        MoneyRequest req = new MoneyRequest(new BigDecimal("200.00"));
        ResponseEntity<AccountResponse> resp = restTemplate.exchange(
                "/api/accounts/" + accountId + "/withdraw", HttpMethod.POST,
                new HttpEntity<>(req, TestHelper.authHeadersWithIdempotency(token, UUID.randomUUID().toString())),
                AccountResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().getBalance()).isEqualByComparingTo("300.00");
    }

    @Test @Order(5)
    void withdraw_insufficientFunds_returns400() {
        // Balance is 300. Try to withdraw 1000.
        MoneyRequest req = new MoneyRequest(new BigDecimal("1000.00"));
        ResponseEntity<ErrorResponse> resp = restTemplate.exchange(
                "/api/accounts/" + accountId + "/withdraw", HttpMethod.POST,
                new HttpEntity<>(req, TestHelper.authHeadersWithIdempotency(token, UUID.randomUUID().toString())),
                ErrorResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody().getMessage()).containsIgnoringCase("insufficient");
    }

    @Test @Order(6)
    void withdraw_insufficientFunds_balanceUnchanged() {
        // Verify balance is still 300 after the failed withdrawal.
        ResponseEntity<BigDecimal> resp = restTemplate.exchange(
                "/api/accounts/" + accountId + "/balance", HttpMethod.GET,
                new HttpEntity<>(TestHelper.authHeaders(token)), BigDecimal.class);

        assertThat(resp.getBody()).isEqualByComparingTo("300.00");
    }

    @Test @Order(7)
    void deposit_idempotency_sameKeyTwice_moneyMovesOnce() {
        String idempotencyKey = UUID.randomUUID().toString();
        MoneyRequest req = new MoneyRequest(new BigDecimal("100.00"));

        // First request
        ResponseEntity<AccountResponse> resp1 = restTemplate.exchange(
                "/api/accounts/" + accountId + "/deposit", HttpMethod.POST,
                new HttpEntity<>(req, TestHelper.authHeadersWithIdempotency(token, idempotencyKey)),
                AccountResponse.class);

        // Second request with same key
        ResponseEntity<AccountResponse> resp2 = restTemplate.exchange(
                "/api/accounts/" + accountId + "/deposit", HttpMethod.POST,
                new HttpEntity<>(req, TestHelper.authHeadersWithIdempotency(token, idempotencyKey)),
                AccountResponse.class);

        assertThat(resp1.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp2.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Balance should be 400 (300 + 100), not 500 (300 + 100 + 100).
        ResponseEntity<BigDecimal> balResp = restTemplate.exchange(
                "/api/accounts/" + accountId + "/balance", HttpMethod.GET,
                new HttpEntity<>(TestHelper.authHeaders(token)), BigDecimal.class);
        assertThat(balResp.getBody()).isEqualByComparingTo("400.00");
    }

    @Test @Order(8)
    void deposit_unauthorizedAccount_returns403() {
        // Register a different user and try to deposit into accountId owned by EMAIL.
        String otherToken = TestHelper.registerAndLogin(restTemplate,
                "other_" + System.currentTimeMillis() + "@test.com", "Password1", Role.ACCOUNT_HOLDER);

        MoneyRequest req = new MoneyRequest(new BigDecimal("100.00"));
        ResponseEntity<ErrorResponse> resp = restTemplate.exchange(
                "/api/accounts/" + accountId + "/deposit", HttpMethod.POST,
                new HttpEntity<>(req, TestHelper.authHeadersWithIdempotency(otherToken, UUID.randomUUID().toString())),
                ErrorResponse.class);

        assertThat(resp.getStatusCode()).isIn(HttpStatus.FORBIDDEN, HttpStatus.UNAUTHORIZED);
    }
}
