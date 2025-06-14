package com.example.supplier.dto;

import java.util.List;

public class TransactionPrepareRequest {
    private String transactionId;
    private List<OrderRequest> items; // assuming you already have this DTO

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public List<OrderRequest> getItems() {
        return items;
    }

    public void setItems(List<OrderRequest> items) {
        this.items = items;
    }
}
