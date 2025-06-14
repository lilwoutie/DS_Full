package com.fooddelivery.brokerapplication.model;

/**
 * Enum representing the status of a single participant (restaurant)
 * in the two-phase commit protocol.
 *
 * This is stored in ParticipantLog to track each participant’s vote and final outcome.
 * Console logs in TwoPhaseCommitService will reference these statuses for clarity.
 */
public enum ParticipantStatus {
    /**
     * Initial state for a participant when the coordinator has not yet sent a prepare request.
     * After creation of a ParticipantLog, status is set to PARTICIPATING.
     */
    PARTICIPATING,

    /**
     * Participant has responded positively to the prepare request (voted “yes”).
     * The coordinator will later send commit.
     */
    PREPARED,

    /**
     * Participant has successfully committed its local transaction in response to the commit request.
     */
    COMMITTED,

    /**
     * Participant either voted “no” during prepare or failed during commit,
     * and has rolled back or will roll back its local changes.
     */
    ABORTED
}
