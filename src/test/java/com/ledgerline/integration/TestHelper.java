package com.ledgerline.integration;

import com.ledgerline.dto.*;
import com.ledgerline.enums.Role;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

public class TestHelper {

    public static String registerAndLogin(TestRestTemplate restTemplate, String email, String password, Role role) {
        RegisterRequest reg = new RegisterRequest("Test User " + email, email, password, role);
        restTemplate.postForEntity("/api/auth/register", reg, UserResponse.class);

        LoginRequest login = new LoginRequest(email, password);
        ResponseEntity<AuthResponse> resp = restTemplate.postForEntity("/api/auth/login", login, AuthResponse.class);
        return resp.getBody().getToken();
    }

    public static HttpHeaders authHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    public static HttpHeaders authHeadersWithIdempotency(String token, String idempotencyKey) {
        HttpHeaders headers = authHeaders(token);
        headers.set("Idempotency-Key", idempotencyKey);
        return headers;
    }
}
