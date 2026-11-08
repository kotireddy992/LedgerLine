package com.ledgerline.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public class MoneyRequest {

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    @Digits(integer = 17, fraction = 2, message = "Amount must have at most 2 decimal places")
    private BigDecimal amount;

    public MoneyRequest() {}
    public MoneyRequest(BigDecimal amount) { this.amount = amount; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
}
