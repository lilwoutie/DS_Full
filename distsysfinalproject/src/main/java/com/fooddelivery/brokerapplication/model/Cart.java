package com.fooddelivery.brokerapplication.model;

import java.util.ArrayList;
import java.util.List;

/**
 * A simple session-scoped cart that holds CartItem objects.
 * Because it’s tied to the user’s HTTP session, each user has their own Cart.
 */
public class Cart {
    private final List<CartItem> items = new ArrayList<>();

    public Cart() {
        // No-arg constructor
    }

    /** Return the list of items currently in the cart. */
    public List<CartItem> getItems() {
        return items;
    }

    /**
     * Add a new CartItem. In a more complete version, you’d
     * first check if an item with the same mealId already exists and increment its quantity.
     * For now, we just add.
     */
    public void addItem(CartItem item) {
        for (CartItem existing : items) {
            if (existing.getMealId().equals(item.getMealId())
                    && existing.getRestaurantId() == item.getRestaurantId()) {
                existing.setQuantity(existing.getQuantity() + item.getQuantity());
                return;
            }
        }
        items.add(item);
    }



    /** Remove every CartItem whose mealId matches the given ID. */
    public void removeItem(Long mealId) {
        items.removeIf(i -> i.getMealId().equals(mealId));
    }

    /** Return the total count of items (sum of each CartItem’s quantity). */
    public int getTotalItemCount() {
        return items.stream().mapToInt(CartItem::getQuantity).sum();
    }

    /** Clear all items (empty the cart). */
    public void clear() {
        items.clear();
    }

    /** Return the total monetary amount of items in the cart. */
    public double getTotalAmount() {
        return items.stream()
                .mapToDouble(i -> i.getPrice() * i.getQuantity())
                .sum();
    }
}

