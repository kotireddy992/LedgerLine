package com.ledgerline.repository;

import com.ledgerline.entity.Account;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {

    List<Account> findByOwnerId(Long ownerId);

    boolean existsByAccountNumber(String accountNumber);

    /**
     * Acquires a pessimistic write lock on the account row.
     * Used before any balance-modifying operation to prevent lost updates
     * under concurrent requests. The database serializes access at the row level.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Account a WHERE a.id = :id")
    Optional<Account> findByIdWithLock(@Param("id") Long id);
}
