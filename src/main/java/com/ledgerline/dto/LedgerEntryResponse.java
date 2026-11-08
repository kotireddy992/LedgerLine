package com.ledgerline.dto;

import com.ledgerline.enums.EntryType;
import java.math.BigDecimal;
import java.time.Instant;

public class LedgerEntryResponse {
    private Long id;
    private String transactionReference;
    private EntryType entryType;
    private BigDecimal amount;
    private BigDecimal resultingBalance;
    private String initiatedByEmail;
    private Instant createdAt;

    public LedgerEntryResponse() {}
    private LedgerEntryResponse(Builder b) {
        this.id = b.id; this.transactionReference = b.transactionReference;
        this.entryType = b.entryType; this.amount = b.amount;
        this.resultingBalance = b.resultingBalance; this.initiatedByEmail = b.initiatedByEmail;
        this.createdAt = b.createdAt;
    }

    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private Long id;
        private String transactionReference, initiatedByEmail;
        private EntryType entryType;
        private BigDecimal amount, resultingBalance;
        private Instant createdAt;
        public Builder id(Long id)                         { this.id = id; return this; }
        public Builder transactionReference(String ref)    { this.transactionReference = ref; return this; }
        public Builder entryType(EntryType t)              { this.entryType = t; return this; }
        public Builder amount(BigDecimal a)                { this.amount = a; return this; }
        public Builder resultingBalance(BigDecimal b)      { this.resultingBalance = b; return this; }
        public Builder initiatedByEmail(String e)          { this.initiatedByEmail = e; return this; }
        public Builder createdAt(Instant t)                { this.createdAt = t; return this; }
        public LedgerEntryResponse build()                 { return new LedgerEntryResponse(this); }
    }

    public Long getId()                     { return id; }
    public String getTransactionReference() { return transactionReference; }
    public EntryType getEntryType()         { return entryType; }
    public BigDecimal getAmount()           { return amount; }
    public BigDecimal getResultingBalance() { return resultingBalance; }
    public String getInitiatedByEmail()     { return initiatedByEmail; }
    public Instant getCreatedAt()           { return createdAt; }
}
