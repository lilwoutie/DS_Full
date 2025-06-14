package com.fooddelivery.brokerapplication.repository;

import com.fooddelivery.brokerapplication.model.TransactionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for TransactionLog entities.
 *
 * Provides CRUD operations for the global transaction log in the two-phase commit protocol.
 * The primary key is the transactionId (String), matching TransactionLog.id.
 *
 * Used by TwoPhaseCommitService to:
 *  - Persist a new TransactionLog when starting a transaction (status INIT).
 *  - Update status through PREPARED, COMMITTING, COMMITTED, or ABORTED.
 *  - On recovery, query by ID or scan for logs in intermediate states to resume or abort incomplete transactions.
 */
@Repository
public interface TransactionLogRepository extends JpaRepository<TransactionLog, String> {
    // No additional methods needed for basic 2PC, but custom query methods (e.g., findByStatus) can be added if desired.
}
