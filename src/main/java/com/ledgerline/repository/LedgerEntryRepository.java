package com.ledgerline.repository;

import com.ledgerline.entity.LedgerEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {

    Page<LedgerEntry> findByAccountIdOrderByCreatedAtDesc(Long accountId, Pageable pageable);

    /**
     * Verifies the double-entry invariant for a given transaction reference.
     * For a completed internal transfer: SUM(DEBIT amounts) - SUM(CREDIT amounts) = 0.
     * Returns the net sum; a balanced transaction returns 0.
     */
    @Query("""
            SELECT COALESCE(SUM(CASE WHEN e.entryType = 'DEBIT'  THEN e.amount ELSE 0 END), 0)
                 - COALESCE(SUM(CASE WHEN e.entryType = 'CREDIT' THEN e.amount ELSE 0 END), 0)
            FROM LedgerEntry e
            WHERE e.transactionReference = :ref
            """)
    BigDecimal calculateNetForReference(@Param("ref") String transactionReference);
}
