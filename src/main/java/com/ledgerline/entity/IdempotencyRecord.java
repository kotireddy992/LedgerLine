package com.ledgerline.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "idempotency_records", indexes = {
        @Index(name = "idx_idempotency_key", columnList = "idempotency_key")
})
public class IdempotencyRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 100)
    private String idempotencyKey;

    @Column(name = "request_hash", nullable = false, length = 64)
    private String requestHash;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "response_body", columnDefinition = "TEXT")
    private String responseBody;

    @Column(name = "response_status")
    private Integer responseStatus;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public IdempotencyRecord() {}

    private IdempotencyRecord(Builder b) {
        this.idempotencyKey = b.idempotencyKey;
        this.requestHash = b.requestHash;
        this.status = b.status;
        this.responseBody = b.responseBody;
        this.responseStatus = b.responseStatus;
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String idempotencyKey, requestHash, status, responseBody;
        private Integer responseStatus;

        public Builder idempotencyKey(String key)      { this.idempotencyKey = key; return this; }
        public Builder requestHash(String hash)        { this.requestHash = hash; return this; }
        public Builder status(String status)           { this.status = status; return this; }
        public Builder responseBody(String body)       { this.responseBody = body; return this; }
        public Builder responseStatus(Integer status)  { this.responseStatus = status; return this; }
        public IdempotencyRecord build()               { return new IdempotencyRecord(this); }
    }

    public Long getId()              { return id; }
    public String getIdempotencyKey(){ return idempotencyKey; }
    public String getRequestHash()   { return requestHash; }
    public String getStatus()        { return status; }
    public String getResponseBody()  { return responseBody; }
    public Integer getResponseStatus(){ return responseStatus; }
    public Instant getCreatedAt()    { return createdAt; }

    public void setStatus(String status)           { this.status = status; }
    public void setResponseBody(String body)       { this.responseBody = body; }
    public void setResponseStatus(Integer status)  { this.responseStatus = status; }
}
