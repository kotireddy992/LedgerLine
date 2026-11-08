package com.ledgerline.repository;

import com.ledgerline.entity.IdempotencyRecord;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface IdempotencyRecordRepository extends JpaRepository<IdempotencyRecord, Long> {

    Optional<IdempotencyRecord> findByIdempotencyKey(String idempotencyKey);

    /**
     * Locks the idempotency record row for update.
     * Prevents two concurrent requests from both seeing "not found" and
     * both proceeding to execute the financial operation.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM IdempotencyRecord r WHERE r.idempotencyKey = :key")
    Optional<IdempotencyRecord> findByIdempotencyKeyWithLock(@Param("key") String key);
}
