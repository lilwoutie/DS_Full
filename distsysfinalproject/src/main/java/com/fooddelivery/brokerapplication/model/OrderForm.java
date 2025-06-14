package com.fooddelivery.brokerapplication.model;

public class OrderForm {
    private String fullName;
    private String address;
    private String deliveryMethod; // "pickup" or "delivery"
    private String paymentMethod;  // e.g., "creditCard", "paypal"

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getDeliveryMethod() { return deliveryMethod; }
    public void setDeliveryMethod(String deliveryMethod) { this.deliveryMethod = deliveryMethod; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
}
