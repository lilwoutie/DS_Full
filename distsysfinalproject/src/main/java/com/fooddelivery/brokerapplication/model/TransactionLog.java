package com.fooddelivery.brokerapplication.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * JPA entity to persist the global transaction metadata for Two-Phase Commit.
 *
 * Each instance corresponds to one distributed transaction coordinated by the broker.
 *
 * Fields:
 *  - id: the unique transaction ID (UUID string) used across participants.
 *  - status: current global status in the 2PC protocol (INIT, PREPARED, COMMITTING, COMMITTED, ABORTED).
 *  - createdAt: timestamp when this log was first persisted.
 *  - updatedAt: timestamp of the last status update.
 *
 * Timestamps are managed via JPA lifecycle callbacks:
 *  - @PrePersist sets createdAt and updatedAt to now before initial insert.
 *  - @PreUpdate sets updatedAt to now before updates.
 */
@Entity
@Table(name = "transaction_log")
public class TransactionLog {

    /**
     * The unique identifier for the distributed transaction.
     * Generated externally (UUID string) by the broker.
     */
    @Id
    @Column(name = "transaction_id", nullable = false, updatable = false)
    private String id;

    /**
     * Current status of the global transaction in the 2PC protocol.
     * Stored as a string via EnumType.STRING for readability in the DB.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TransactionStatus status;

    /**
     * Timestamp when this TransactionLog was created.
     */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp when status was last updated.
     */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Default constructor for JPA.
     */
    protected TransactionLog() { }

    /**
     * Constructor to create a new TransactionLog record with initial status.
     * Timestamps are set via @PrePersist.
     *
     * @param id      Unique transaction ID (UUID string).
     * @param status  Initial status, e.g., TransactionStatus.INIT.
     */
    public TransactionLog(String id, TransactionStatus status) {
        this.id = id;
        this.status = status;
        //@PrePersist also handles createdAt, but this is to ensure no null values at all time
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    /**
     * Lifecycle callback before insert: set createdAt and updatedAt to current time.
     */
    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    /**
     * Lifecycle callback before update: set updatedAt to current time.
     */
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // ----- Getters and Setters -----

    public String getId() {
        return id;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    /**
     * Update the status.
     * Timestamp updated via @PreUpdate when the entity is saved.
     *
     * @param status new TransactionStatus
     */
    public void setStatus(TransactionStatus status) {
        this.status = status;
        // updatedAt will be set automatically in @PreUpdate
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime now) {
        this.updatedAt = now;
    }

    /**
     * toString for logging/debugging. Avoid printing large collections here.
     */
    @Override
    public String toString() {
        return "TransactionLog{" +
                "id='" + id + '\'' +
                ", status=" + status +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }

    /**
     * Equals and hashCode based on ID, since it's unique and immutable once set.
     * Important if you store entities in collections or use EntityManager methods.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TransactionLog)) return false;
        TransactionLog that = (TransactionLog) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

}
