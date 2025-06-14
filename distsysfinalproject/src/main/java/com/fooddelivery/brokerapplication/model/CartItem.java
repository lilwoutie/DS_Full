package com.fooddelivery.brokerapplication.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Table;
import org.springframework.data.annotation.Id;

/**
 * A minimal representation of one item in the shopping cart.
 */
@Entity
@Table(name = "cart_item")
public class CartItem {
    @jakarta.persistence.Id
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long mealId;
    private int restaurantId;
    private String name;
    private double price;
    private int quantity = 1;

    public CartItem() { }

    public CartItem(Long mealId, int restaurantId, String name, double price, int quantity) {
        this.mealId = mealId;
        this.restaurantId = restaurantId;
        this.name = name;
        this.price = price;
        this.quantity = quantity;
    }

    // Getters and setters for all fields:

    public Long getMealId() { return mealId; }
    public void setMealId(Long mealId) { this.mealId = mealId; }

    public int getRestaurantId() { return restaurantId; }
    public void setRestaurantId(int restaurantId) { this.restaurantId = restaurantId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }
}

