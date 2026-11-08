package com.ledgerline.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ledgerline.dto.*;
import com.ledgerline.enums.Role;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import static org.assertj.core.api.Assertions.assertThat;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuthIntegrationTest extends BaseIntegrationTest {

    @Autowired TestRestTemplate restTemplate;
    @Autowired ObjectMapper objectMapper;

    static String registeredEmail = "authtest_" + System.currentTimeMillis() + "@test.com";

    @Test @Order(1)
    void register_success() {
        RegisterRequest req = new RegisterRequest("Auth User", registeredEmail, "Password1", Role.ACCOUNT_HOLDER);
        ResponseEntity<UserResponse> resp = restTemplate.postForEntity("/api/auth/register", req, UserResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().getEmail()).isEqualTo(registeredEmail);
        assertThat(resp.getBody().getRole()).isEqualTo(Role.ACCOUNT_HOLDER);
    }

    @Test @Order(2)
    void register_duplicateEmail_returns409() {
        RegisterRequest req = new RegisterRequest("Auth User", registeredEmail, "Password1", Role.ACCOUNT_HOLDER);
        ResponseEntity<ErrorResponse> resp = restTemplate.postForEntity("/api/auth/register", req, ErrorResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test @Order(3)
    void login_success_returnsToken() {
        LoginRequest req = new LoginRequest(registeredEmail, "Password1");
        ResponseEntity<AuthResponse> resp = restTemplate.postForEntity("/api/auth/login", req, AuthResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().getToken()).isNotBlank();
        assertThat(resp.getBody().getRole()).isEqualTo("ACCOUNT_HOLDER");
    }

    @Test @Order(4)
    void login_wrongPassword_returns401() {
        LoginRequest req = new LoginRequest(registeredEmail, "WrongPassword1");
        ResponseEntity<ErrorResponse> resp = restTemplate.postForEntity("/api/auth/login", req, ErrorResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        // Must not reveal whether email or password was wrong.
        assertThat(resp.getBody().getMessage()).doesNotContain("password");
        assertThat(resp.getBody().getMessage()).doesNotContain("email");
    }

    @Test @Order(5)
    void protectedEndpoint_withoutToken_returns401() {
        ResponseEntity<String> resp = restTemplate.getForEntity("/api/accounts", String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test @Order(6)
    void register_invalidEmail_returns400() {
        RegisterRequest req = new RegisterRequest("Bad User", "not-an-email", "Password1", Role.ACCOUNT_HOLDER);
        ResponseEntity<ErrorResponse> resp = restTemplate.postForEntity("/api/auth/register", req, ErrorResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test @Order(7)
    void register_shortPassword_returns400() {
        RegisterRequest req = new RegisterRequest("Bad User", "short@test.com", "abc", Role.ACCOUNT_HOLDER);
        ResponseEntity<ErrorResponse> resp = restTemplate.postForEntity("/api/auth/register", req, ErrorResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
