package com.fooddelivery.brokerapplication.model;

import com.fooddelivery.brokerapplication.dto.PrepareItemDTO;

import java.util.List;

/**
 * DTO representing the prepare request sent from the broker (coordinator) to a restaurant (participant)
 * during the two-phase commit protocol.
 *
 * This class is placed in the model package so it can be reused both in the broker service and,
 * if desired, in restaurant services (with the same package structure) for JSON serialization/deserialization.
 *
 * Fields:
 *  - transactionId: the unique ID of the distributed transaction (UUID string).
 *  - items: the list of PrepareItemDTO objects for this restaurant in the current transaction.
 *
 * Usage in TwoPhaseCommitService:
 *   PrepareRequest req = new PrepareRequest(txId, itemsForThisRestaurant);
 *   restTemplate.postForEntity(restaurantBase + "/prepare", new HttpEntity<>(req, headers), Void.class);
 *
 * Note:
 *   - PrepareItemDTO must have a no-arg constructor and getters/setters so Jackson can serialize/deserialize.
 *   - If you wish to avoid sending internal fields (e.g., the database primary key `id` of PrepareItemDTO),
 *     consider annotating those getters with @JsonIgnore or creating a separate lightweight DTO.
 */
public class PrepareRequest {

    private String transactionId;
    private List<PrepareItemDTO> items;

    public PrepareRequest() {
        // Default constructor needed for JSON deserialization
    }

    public PrepareRequest(String transactionId, List<PrepareItemDTO> items) {
        this.transactionId = transactionId;
        this.items = items;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public List<PrepareItemDTO> getItems() {
        return items;
    }

    public void setItems(List<PrepareItemDTO> items) {
        this.items = items;
    }

    @Override
    public String toString() {
        return "PrepareRequest{" +
                "transactionId='" + transactionId + '\'' +
                ", items=" + items +
                '}';
    }
}
