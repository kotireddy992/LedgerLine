package com.ledgerline.dto;

import jakarta.validation.constraints.*;

public class CreateAccountRequest {

    @NotBlank(message = "Currency is required")
    @Size(min = 3, max = 3, message = "Currency must be a 3-letter ISO code (e.g. INR)")
    @Pattern(regexp = "[A-Z]{3}", message = "Currency must be uppercase 3-letter ISO code")
    private String currency;

    public CreateAccountRequest() {}
    public CreateAccountRequest(String currency) { this.currency = currency; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
}
