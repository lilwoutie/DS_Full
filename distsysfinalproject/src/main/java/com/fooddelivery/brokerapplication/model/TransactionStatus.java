package com.fooddelivery.brokerapplication.model;

/**
 * Enum representing the global transaction status in the two-phase commit protocol.
 *
 * This is persisted in TransactionLog to track the coordinator’s state durably.
 * Console logs will reference these statuses for clarity.
 */
public enum TransactionStatus {
    /**
     * Initial state: before sending any prepare requests.
     */
    INIT,

    /**
     * All participants have responded positively to prepare (voted “yes”).
     * The coordinator is now ready to send commit requests.
     */
    PREPARED,

    /**
     * Coordinator is in the process of sending commit requests to participants.
     */
    COMMITTING,

    /**
     * Coordinator has successfully sent commit to all participants (and they responded OK).
     * Final state indicating the distributed transaction committed.
     */
    COMMITTED,

    /**
     * A participant voted “no” during prepare, or an error occurred during any phase.
     * The transaction is aborted; coordinator will instruct all prepared participants to roll back.
     */
    ABORTED
}
