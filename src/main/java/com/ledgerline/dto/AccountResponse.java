package com.ledgerline.dto;

import com.ledgerline.enums.AccountStatus;
import java.math.BigDecimal;
import java.time.Instant;

public class AccountResponse {
    private Long id;
    private String accountNumber;
    private BigDecimal balance;
    private String currency;
    private AccountStatus status;
    private Long ownerId;
    private String ownerName;
    private Instant createdAt;

    public AccountResponse() {}
    private AccountResponse(Builder b) {
        this.id = b.id; this.accountNumber = b.accountNumber; this.balance = b.balance;
        this.currency = b.currency; this.status = b.status; this.ownerId = b.ownerId;
        this.ownerName = b.ownerName; this.createdAt = b.createdAt;
    }

    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private Long id, ownerId;
        private String accountNumber, currency, ownerName;
        private BigDecimal balance;
        private AccountStatus status;
        private Instant createdAt;
        public Builder id(Long id)                     { this.id = id; return this; }
        public Builder accountNumber(String an)        { this.accountNumber = an; return this; }
        public Builder balance(BigDecimal bal)         { this.balance = bal; return this; }
        public Builder currency(String cur)            { this.currency = cur; return this; }
        public Builder status(AccountStatus s)         { this.status = s; return this; }
        public Builder ownerId(Long oid)               { this.ownerId = oid; return this; }
        public Builder ownerName(String name)          { this.ownerName = name; return this; }
        public Builder createdAt(Instant t)            { this.createdAt = t; return this; }
        public AccountResponse build()                 { return new AccountResponse(this); }
    }

    public Long getId()               { return id; }
    public String getAccountNumber()  { return accountNumber; }
    public BigDecimal getBalance()    { return balance; }
    public String getCurrency()       { return currency; }
    public AccountStatus getStatus()  { return status; }
    public Long getOwnerId()          { return ownerId; }
    public String getOwnerName()      { return ownerName; }
    public Instant getCreatedAt()     { return createdAt; }
}
