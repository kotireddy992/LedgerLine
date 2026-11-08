package com.ledgerline.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.*;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "LedgerLine API",
        version = "1.0.0",
        description = "Money Movement with a Ledger You Can Prove — Snovasys Fresher Hiring Challenge 2026",
        contact = @Contact(name = "LedgerLine", email = "admin@ledgerline.com")
    )
)
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
    description = "Provide the JWT token obtained from POST /api/auth/login"
)
public class OpenApiConfig {
}
