package com.ledgerline.entity;

import com.ledgerline.enums.EntryType;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Append-only ledger record.
 * @Immutable prevents Hibernate from issuing UPDATE statements.
 * No setters are provided — only a builder for initial construction.
 */
@Entity
@Immutable
@Table(name = "ledger_entries", indexes = {
        @Index(name = "idx_ledger_account", columnList = "account_id"),
        @Index(name = "idx_ledger_tx_ref", columnList = "transaction_reference"),
        @Index(name = "idx_ledger_created_at", columnList = "created_at")
})
public class LedgerEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_reference", nullable = false, length = 100)
    private String transactionReference;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Enumerated(EnumType.STRING)
    @Column(name = "entry_type", nullable = false, length = 6)
    private EntryType entryType;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "resulting_balance", nullable = false, precision = 19, scale = 2)
    private BigDecimal resultingBalance;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "initiated_by_id", nullable = false)
    private User initiatedBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public LedgerEntry() {}

    private LedgerEntry(Builder b) {
        this.transactionReference = b.transactionReference;
        this.account = b.account;
        this.entryType = b.entryType;
        this.amount = b.amount;
        this.resultingBalance = b.resultingBalance;
        this.initiatedBy = b.initiatedBy;
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String transactionReference;
        private Account account;
        private EntryType entryType;
        private BigDecimal amount, resultingBalance;
        private User initiatedBy;

        public Builder transactionReference(String ref)    { this.transactionReference = ref; return this; }
        public Builder account(Account account)            { this.account = account; return this; }
        public Builder entryType(EntryType type)           { this.entryType = type; return this; }
        public Builder amount(BigDecimal amount)           { this.amount = amount; return this; }
        public Builder resultingBalance(BigDecimal bal)    { this.resultingBalance = bal; return this; }
        public Builder initiatedBy(User user)              { this.initiatedBy = user; return this; }
        public LedgerEntry build()                         { return new LedgerEntry(this); }
    }

    public Long getId()                      { return id; }
    public String getTransactionReference()  { return transactionReference; }
    public Account getAccount()              { return account; }
    public EntryType getEntryType()          { return entryType; }
    public BigDecimal getAmount()            { return amount; }
    public BigDecimal getResultingBalance()  { return resultingBalance; }
    public User getInitiatedBy()             { return initiatedBy; }
    public Instant getCreatedAt()            { return createdAt; }
}
