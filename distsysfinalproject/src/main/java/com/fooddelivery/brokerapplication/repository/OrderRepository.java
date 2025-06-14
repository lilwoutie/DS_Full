package com.fooddelivery.brokerapplication.repository;

import com.fooddelivery.brokerapplication.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {
}
