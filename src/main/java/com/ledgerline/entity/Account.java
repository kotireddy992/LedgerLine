package com.ledgerline.entity;

import com.ledgerline.enums.AccountStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "accounts", indexes = {
        @Index(name = "idx_accounts_owner", columnList = "owner_id"),
        @Index(name = "idx_accounts_number", columnList = "account_number")
})
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_number", nullable = false, unique = true, length = 20)
    private String accountNumber;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private AccountStatus status;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Version
    @Column(nullable = false)
    private Long version;

    public Account() {}

    private Account(Builder b) {
        this.id = b.id;
        this.accountNumber = b.accountNumber;
        this.balance = b.balance;
        this.currency = b.currency;
        this.status = b.status;
        this.owner = b.owner;
        this.version = b.version;
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long id;
        private String accountNumber, currency;
        private BigDecimal balance;
        private AccountStatus status;
        private User owner;
        private Long version;

        public Builder id(Long id)                         { this.id = id; return this; }
        public Builder accountNumber(String an)            { this.accountNumber = an; return this; }
        public Builder balance(BigDecimal balance)         { this.balance = balance; return this; }
        public Builder currency(String currency)           { this.currency = currency; return this; }
        public Builder status(AccountStatus status)        { this.status = status; return this; }
        public Builder owner(User owner)                   { this.owner = owner; return this; }
        public Builder version(Long version)               { this.version = version; return this; }
        public Account build()                             { return new Account(this); }
    }

    public Long getId()               { return id; }
    public String getAccountNumber()  { return accountNumber; }
    public BigDecimal getBalance()    { return balance; }
    public String getCurrency()       { return currency; }
    public AccountStatus getStatus()  { return status; }
    public User getOwner()            { return owner; }
    public Instant getCreatedAt()     { return createdAt; }
    public Long getVersion()          { return version; }

    public void setBalance(BigDecimal balance) { this.balance = balance; }
    public void setStatus(AccountStatus status){ this.status = status; }
}
