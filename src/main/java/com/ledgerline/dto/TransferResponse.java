package com.ledgerline.dto;

import com.ledgerline.enums.TransferStatus;
import java.math.BigDecimal;
import java.time.Instant;

public class TransferResponse {
    private Long id;
    private Long sourceAccountId;
    private Long destinationAccountId;
    private BigDecimal amount;
    private String currency;
    private TransferStatus status;
    private String idempotencyKey;
    private String failureReason;
    private Instant createdAt;

    public TransferResponse() {}
    private TransferResponse(Builder b) {
        this.id = b.id; this.sourceAccountId = b.sourceAccountId;
        this.destinationAccountId = b.destinationAccountId; this.amount = b.amount;
        this.currency = b.currency; this.status = b.status; this.idempotencyKey = b.idempotencyKey;
        this.failureReason = b.failureReason; this.createdAt = b.createdAt;
    }

    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private Long id, sourceAccountId, destinationAccountId;
        private BigDecimal amount;
        private String currency, idempotencyKey, failureReason;
        private TransferStatus status;
        private Instant createdAt;
        public Builder id(Long id)                         { this.id = id; return this; }
        public Builder sourceAccountId(Long id)            { this.sourceAccountId = id; return this; }
        public Builder destinationAccountId(Long id)       { this.destinationAccountId = id; return this; }
        public Builder amount(BigDecimal a)                { this.amount = a; return this; }
        public Builder currency(String c)                  { this.currency = c; return this; }
        public Builder status(TransferStatus s)            { this.status = s; return this; }
        public Builder idempotencyKey(String k)            { this.idempotencyKey = k; return this; }
        public Builder failureReason(String r)             { this.failureReason = r; return this; }
        public Builder createdAt(Instant t)                { this.createdAt = t; return this; }
        public TransferResponse build()                    { return new TransferResponse(this); }
    }

    public Long getId()                    { return id; }
    public Long getSourceAccountId()       { return sourceAccountId; }
    public Long getDestinationAccountId()  { return destinationAccountId; }
    public BigDecimal getAmount()          { return amount; }
    public String getCurrency()            { return currency; }
    public TransferStatus getStatus()      { return status; }
    public String getIdempotencyKey()      { return idempotencyKey; }
    public String getFailureReason()       { return failureReason; }
    public Instant getCreatedAt()          { return createdAt; }
}
