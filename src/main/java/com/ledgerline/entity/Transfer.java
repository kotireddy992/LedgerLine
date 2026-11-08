package com.ledgerline.entity;

import com.ledgerline.enums.TransferStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "transfers", indexes = {
        @Index(name = "idx_transfers_source", columnList = "source_account_id"),
        @Index(name = "idx_transfers_dest", columnList = "destination_account_id"),
        @Index(name = "idx_transfers_idempotency", columnList = "idempotency_key"),
        @Index(name = "idx_transfers_initiated_by", columnList = "initiated_by_id")
})
public class Transfer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_account_id", nullable = false)
    private Account sourceAccount;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "destination_account_id", nullable = false)
    private Account destinationAccount;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TransferStatus status;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 100)
    private String idempotencyKey;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "initiated_by_id", nullable = false)
    private User initiatedBy;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public Transfer() {}

    private Transfer(Builder b) {
        this.sourceAccount = b.sourceAccount;
        this.destinationAccount = b.destinationAccount;
        this.amount = b.amount;
        this.currency = b.currency;
        this.status = b.status;
        this.idempotencyKey = b.idempotencyKey;
        this.initiatedBy = b.initiatedBy;
        this.failureReason = b.failureReason;
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Account sourceAccount, destinationAccount;
        private BigDecimal amount;
        private String currency, idempotencyKey, failureReason;
        private TransferStatus status;
        private User initiatedBy;

        public Builder sourceAccount(Account a)        { this.sourceAccount = a; return this; }
        public Builder destinationAccount(Account a)   { this.destinationAccount = a; return this; }
        public Builder amount(BigDecimal amount)        { this.amount = amount; return this; }
        public Builder currency(String currency)        { this.currency = currency; return this; }
        public Builder status(TransferStatus status)    { this.status = status; return this; }
        public Builder idempotencyKey(String key)       { this.idempotencyKey = key; return this; }
        public Builder initiatedBy(User user)           { this.initiatedBy = user; return this; }
        public Builder failureReason(String reason)     { this.failureReason = reason; return this; }
        public Transfer build()                         { return new Transfer(this); }
    }

    public Long getId()                        { return id; }
    public Account getSourceAccount()          { return sourceAccount; }
    public Account getDestinationAccount()     { return destinationAccount; }
    public BigDecimal getAmount()              { return amount; }
    public String getCurrency()                { return currency; }
    public TransferStatus getStatus()          { return status; }
    public String getIdempotencyKey()          { return idempotencyKey; }
    public User getInitiatedBy()               { return initiatedBy; }
    public String getFailureReason()           { return failureReason; }
    public Instant getCreatedAt()              { return createdAt; }

    public void setStatus(TransferStatus status)       { this.status = status; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
}
