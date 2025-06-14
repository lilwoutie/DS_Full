package com.fooddelivery.brokerapplication.service;

import com.fooddelivery.brokerapplication.model.CartItem;
import com.fooddelivery.brokerapplication.model.Order;
import com.fooddelivery.brokerapplication.repository.CartItemRepository;
import com.fooddelivery.brokerapplication.repository.OrderRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartItemRepository cartItemRepository;


    // Constructor-based injection
    public OrderService(OrderRepository orderRepository, CartItemRepository cartItemRepository) {
        this.orderRepository = orderRepository;
        this.cartItemRepository = cartItemRepository;
    }

    public void saveOrder(Order order) {
        List<CartItem> persistedItems = new ArrayList<>();
        for (CartItem item : order.getMeals()) {
            CartItem saved = cartItemRepository.save(item);
            persistedItems.add(saved);
        }

        order.setMeals(persistedItems);
        orderRepository.save(order);
    }

    public List<Order> getAllOrders() {
        return orderRepository.findAll();  // Query from database
    }
}

