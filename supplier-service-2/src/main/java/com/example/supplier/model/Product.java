package com.example.supplier.model;

import jakarta.persistence.*;

@Entity
@Table(name = "product")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String description;
    private double price;
    private boolean available;
    @Column(nullable = false)
    private int quantity;

    // Constructors
    public Product() {}

    public Product(String name, String description, double price, boolean available, int quantity) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.available = available;
        this.quantity = quantity;
  ;  }

    // Getters and setters
    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }


    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public boolean isAvailable() {
        return quantity > 0;
    }


    public void setAvailable(boolean available) {
        this.available = available;
    }
}
