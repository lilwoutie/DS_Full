package com.fooddelivery.brokerapplication.repository;

import com.fooddelivery.brokerapplication.model.ParticipantLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for ParticipantLog entities.
 *
 * Provides CRUD operations and a method to find all ParticipantLog entries
 * by the global transaction ID. This is used by the TwoPhaseCommitService
 * to:
 *   - Retrieve all participants of a transaction during the commit/abort phase.
 *   - Inspect participant statuses during recovery if the broker restarts mid-transaction.
 */
@Repository
public interface ParticipantLogRepository extends JpaRepository<ParticipantLog, Long> {

    /**
     * Find all ParticipantLog entries for a given transactionId.
     *
     * @param transactionId the global transaction UUID string
     * @return list of ParticipantLog records associated with that transaction
     */
    List<ParticipantLog> findByTransactionId(String transactionId);
}
