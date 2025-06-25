package com.example.supplier.model;

import java.time.Instant;
import java.util.List;

public class TransactionLogEntry {
    private String transactionId;
    private String action; // "PREPARE", "COMMIT", "ROLLBACK"
    private List<Long> productIds;
    private Instant timestamp;

    public TransactionLogEntry() {} // For Jackson

    public TransactionLogEntry(String transactionId, String action, List<Long> productIds) {
        this.transactionId = transactionId;
        this.action = action;
        this.productIds = productIds;
        this.timestamp = Instant.now();
    }

    public String getTransactionId() { return transactionId; }
    public String getAction() { return action; }
    public List<Long> getProductIds() { return productIds; }
    public Instant getTimestamp() { return timestamp; }

    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
    public void setAction(String action) { this.action = action; }
    public void setProductIds(List<Long> productIds) { this.productIds = productIds; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
}
