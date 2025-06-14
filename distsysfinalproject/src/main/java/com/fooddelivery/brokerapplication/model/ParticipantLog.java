package com.fooddelivery.brokerapplication.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * JPA entity to persist the status of a single participant (restaurant)
 * in a distributed transaction coordinated via Two-Phase Commit.
 *
 * Each instance corresponds to one participant’s involvement in one global transaction.
 * Fields:
 *  - id: auto-generated primary key.
 *  - transactionId: the global transaction’s UUID string, matching TransactionLog.id.
 *  - participant: identifier for the restaurant service (e.g., its base URL or a logical name).
 *  - status: current status in the 2PC protocol (PARTICIPATING, PREPARED, COMMITTED, ABORTED).
 *  - createdAt: timestamp when this participant log was first created (i.e., when the broker began prepare for this participant).
 *  - updatedAt: timestamp of the last status update.
 *
 * By saving this entity at each participant state transition, the coordinator can recover
 * and resume or finalize the protocol after failures.
 */
@Entity
@Table(name = "participant_log")
public class ParticipantLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * References the global transaction ID stored in TransactionLog.id.
     */
    @Column(name = "transaction_id", nullable = false, updatable = false)
    private String transactionId;

    /**
     * Identifier for the participant service.
     * For example, the base URL like "http://localhost:8081/transaction" or a logical name.
     */
    @Column(name = "participant", nullable = false)
    private String participant;

    /**
     * The participant’s current status in the 2PC protocol.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ParticipantStatus status;

    /**
     * When this ParticipantLog was created.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * When this ParticipantLog was last updated (status change).
     */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Default constructor for JPA.
     */
    protected ParticipantLog() { }

    /**
     * Constructor to create a new ParticipantLog entry when starting prepare phase.
     * Initializes status to PARTICIPATING and sets timestamps.
     *
     * @param transactionId the global transaction ID
     * @param participant   identifier or URL of the participant service
     */
    public ParticipantLog(String transactionId, String participant) {
        this.transactionId = transactionId;
        this.participant = participant;
        this.status = ParticipantStatus.PARTICIPATING;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // ----- Getters and Setters -----

    public Long getId() {
        return id;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public String getParticipant() {
        return participant;
    }

    public ParticipantStatus getStatus() {
        return status;
    }

    /**
     * Update the participant status (e.g., to PREPARED, COMMITTED, or ABORTED).
     * Also updates updatedAt timestamp to now.
     *
     * @param status the new ParticipantStatus
     */
    public void setStatus(ParticipantStatus status) {
        this.status = status;
        this.updatedAt = LocalDateTime.now();
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * Optionally allow manually setting createdAt if needed (rarely used).
     */
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    /**
     * Optionally allow manually setting updatedAt (not typically needed).
     */
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
