package com.ledgerline.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ledgerline.entity.IdempotencyRecord;
import com.ledgerline.exception.IdempotencyConflictException;
import com.ledgerline.repository.IdempotencyRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Optional;

/**
 * DB-backed idempotency service.
 *
 * Protocol:
 *  1. Before executing a financial operation, call checkOrCreate(key, requestHash).
 *     - If no record exists: INSERT a PROCESSING record and return empty.
 *     - If a COMPLETED record exists with the same hash: return the stored response.
 *     - If a COMPLETED record exists with a different hash: throw 409 Conflict.
 *     - If a PROCESSING record exists: throw 409 (request still in flight).
 *  2. After the operation succeeds, call markCompleted(key, responseBody, status).
 *  3. After the operation fails, call markFailed(key).
 *
 * Concurrency:
 *  The UNIQUE constraint on idempotency_key means only one INSERT wins.
 *  The loser gets DataIntegrityViolationException → translated to 409.
 *  The findByIdempotencyKeyWithLock uses SELECT FOR UPDATE to prevent
 *  two threads from both reading "not found" before either inserts.
 *
 * Limitation:
 *  If the application crashes between INSERT(PROCESSING) and markCompleted,
 *  the key is stuck in PROCESSING. A background job should expire stale
 *  PROCESSING records after a timeout (not implemented here — noted as
 *  a known limitation in THREATS.md).
 */
@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private final IdempotencyRecordRepository repository;
    private final ObjectMapper objectMapper;

    /**
     * Returns the stored response if this is a replay, or empty if this is a new request.
     * Throws IdempotencyConflictException if the key is reused with a different payload
     * or if the original request is still processing.
     *
     * Uses REQUIRES_NEW so the idempotency INSERT commits independently of the
     * outer financial transaction. This ensures the key is visible to concurrent
     * requests even if the financial transaction is still open.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<StoredResponse> checkOrCreate(String idempotencyKey, String requestBodyJson) {
        String requestHash = sha256(requestBodyJson);

        // Lock the row if it exists to prevent concurrent reads both seeing "not found".
        Optional<IdempotencyRecord> existing =
                repository.findByIdempotencyKeyWithLock(idempotencyKey);

        if (existing.isPresent()) {
            IdempotencyRecord record = existing.get();

            if ("PROCESSING".equals(record.getStatus())) {
                throw new IdempotencyConflictException(
                        "Request with idempotency key '" + idempotencyKey + "' is still processing");
            }

            if (!record.getRequestHash().equals(requestHash)) {
                throw new IdempotencyConflictException(
                        "Idempotency key '" + idempotencyKey + "' was used with a different request payload");
            }

            // Same key, same payload, already completed — return cached result.
            return Optional.of(new StoredResponse(record.getResponseBody(), record.getResponseStatus()));
        }

        // New request — insert PROCESSING record.
        try {
            repository.saveAndFlush(IdempotencyRecord.builder()
                    .idempotencyKey(idempotencyKey)
                    .requestHash(requestHash)
                    .status("PROCESSING")
                    .build());
        } catch (DataIntegrityViolationException e) {
            // Another concurrent request inserted first — treat as conflict.
            throw new IdempotencyConflictException(
                    "Concurrent request with idempotency key '" + idempotencyKey + "' is already processing");
        }

        return Optional.empty();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markCompleted(String idempotencyKey, Object responseBody, int httpStatus) {
        repository.findByIdempotencyKey(idempotencyKey).ifPresent(record -> {
            record.setStatus("COMPLETED");
            record.setResponseBody(toJson(responseBody));
            record.setResponseStatus(httpStatus);
            repository.save(record);
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(String idempotencyKey) {
        repository.findByIdempotencyKey(idempotencyKey).ifPresent(record -> {
            record.setStatus("FAILED");
            repository.save(record);
        });
    }

    @SneakyThrows
    private String sha256(String input) {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(hash);
    }

    @SneakyThrows
    private String toJson(Object obj) {
        return objectMapper.writeValueAsString(obj);
    }

    public record StoredResponse(String body, int status) {}
}
