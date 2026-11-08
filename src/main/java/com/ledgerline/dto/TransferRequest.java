package com.ledgerline.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public class TransferRequest {

    @NotNull(message = "Source account ID is required")
    private Long sourceAccountId;

    @NotNull(message = "Destination account ID is required")
    private Long destinationAccountId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    @Digits(integer = 17, fraction = 2, message = "Amount must have at most 2 decimal places")
    private BigDecimal amount;

    @NotBlank(message = "Currency is required")
    @Size(min = 3, max = 3, message = "Currency must be a 3-letter ISO code")
    @Pattern(regexp = "[A-Z]{3}", message = "Currency must be uppercase 3-letter ISO code")
    private String currency;

    public TransferRequest() {}
    public TransferRequest(Long sourceAccountId, Long destinationAccountId, BigDecimal amount, String currency) {
        this.sourceAccountId = sourceAccountId;
        this.destinationAccountId = destinationAccountId;
        this.amount = amount;
        this.currency = currency;
    }

    public Long getSourceAccountId()      { return sourceAccountId; }
    public Long getDestinationAccountId() { return destinationAccountId; }
    public BigDecimal getAmount()         { return amount; }
    public String getCurrency()           { return currency; }
    public void setSourceAccountId(Long id)      { this.sourceAccountId = id; }
    public void setDestinationAccountId(Long id) { this.destinationAccountId = id; }
    public void setAmount(BigDecimal amount)      { this.amount = amount; }
    public void setCurrency(String currency)      { this.currency = currency; }
}
