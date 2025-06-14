package com.fooddelivery.brokerapplication.dto;

public class PrepareItemDTO {
    private Long productId;
    private int quantity;

    public PrepareItemDTO() {
    }

    public PrepareItemDTO(Long productId, int quantity) {
        this.productId = productId;
        this.quantity = quantity;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
}
